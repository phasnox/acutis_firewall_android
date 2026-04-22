package com.acutis.firewall.ui.screens.home

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acutis.firewall.blocklist.BlocklistDownloader
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.TimeRuleAction
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.BlocklistRepository
import com.acutis.firewall.data.repository.TimeRuleRepository
import com.acutis.firewall.service.FirewallVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isFirewallEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val blockedSitesCount: Int = 0,
    val showPinDialog: Boolean = false,
    val pinError: Boolean = false,
    val pendingAction: PendingAction? = null,
    val isUpdatingBlocklists: Boolean = false,
    val updateStatus: String = "",
    val showUpdateResult: Boolean = false,
    val updateResultTitle: String = "",
    val updateResultMessage: String = "",
    val updateResultIsError: Boolean = false,
    val showVpnConflictAlert: Boolean = false,
    val showLockdownWarning: Boolean = false,
    val isTogglingFirewall: Boolean = false
)

enum class PendingAction {
    TOGGLE_FIREWALL
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val blocklistRepository: BlocklistRepository,
    private val blocklistDownloader: BlocklistDownloader,
    private val timeRuleRepository: TimeRuleRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.firewallEnabled,
                settingsDataStore.pinEnabled,
                blocklistRepository.getEnabledCount(),
                settingsDataStore.lockdownModeDetected
            ) { firewallEnabled, pinEnabled, blockedCount, lockdownDetected ->
                // Clear toggling state when firewall state changes
                _uiState.value.copy(
                    isFirewallEnabled = firewallEnabled,
                    isPinEnabled = pinEnabled,
                    blockedSitesCount = blockedCount,
                    showLockdownWarning = lockdownDetected,
                    isTogglingFirewall = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Check if blocklists need to be downloaded on first launch
        viewModelScope.launch {
            val count = blocklistRepository.getEnabledCount().first()
            if (count == 0) {
                // First launch - download blocklists
                downloadBlocklistsInternal(isInitialDownload = true)
            }
        }

        // Create default time rules on first launch
        viewModelScope.launch {
            if (!settingsDataStore.areDefaultTimeRulesCreated()) {
                createDefaultTimeRules()
                settingsDataStore.setDefaultTimeRulesCreated(true)
            }
        }
    }

    private suspend fun createDefaultTimeRules() {
        // Create a default rule: Allow social media for 30 minutes per day (disabled by default)
        val socialMediaRule = timeRuleRepository.createDailyLimitRule(
            domain = null,
            category = BlockCategory.SOCIAL_MEDIA,
            customListId = null,
            action = TimeRuleAction.ALLOW,
            limitMinutes = 30,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7) // All days
        ).copy(isEnabled = false)
        timeRuleRepository.addRule(socialMediaRule)
    }

    fun onToggleFirewall() {
        // Only require PIN when DISABLING the firewall (to prevent children from turning it off)
        val isDisabling = _uiState.value.isFirewallEnabled
        if (isDisabling && _uiState.value.isPinEnabled && settingsDataStore.hasPin()) {
            _uiState.value = _uiState.value.copy(
                showPinDialog = true,
                pendingAction = PendingAction.TOGGLE_FIREWALL
            )
        } else {
            toggleFirewallInternal()
        }
    }

    fun onPinEntered(pin: String) {
        if (settingsDataStore.verifyPin(pin)) {
            _uiState.value = _uiState.value.copy(
                showPinDialog = false,
                pinError = false
            )
            when (_uiState.value.pendingAction) {
                PendingAction.TOGGLE_FIREWALL -> toggleFirewallInternal()
                null -> {}
            }
            _uiState.value = _uiState.value.copy(pendingAction = null)
        } else {
            _uiState.value = _uiState.value.copy(pinError = true)
        }
    }

    fun onPinDialogDismiss() {
        _uiState.value = _uiState.value.copy(
            showPinDialog = false,
            pinError = false,
            pendingAction = null
        )
    }

    private fun toggleFirewallInternal() {
        _uiState.value = _uiState.value.copy(isTogglingFirewall = true)

        val context = application.applicationContext
        val intent = Intent(context, FirewallVpnService::class.java)

        if (_uiState.value.isFirewallEnabled) {
            intent.action = FirewallVpnService.ACTION_STOP
            context.startService(intent)
        } else {
            intent.action = FirewallVpnService.ACTION_START
            context.startForegroundService(intent)
        }
    }

    fun checkVpnPermission(): Intent? {
        return VpnService.prepare(application.applicationContext)
    }

    fun isOtherVpnActive(): Boolean {
        val connectivityManager = application.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    fun showVpnConflictAlert() {
        _uiState.value = _uiState.value.copy(showVpnConflictAlert = true)
    }

    fun dismissVpnConflictAlert() {
        _uiState.value = _uiState.value.copy(showVpnConflictAlert = false)
    }

    fun dismissLockdownWarning() {
        viewModelScope.launch {
            settingsDataStore.setLockdownModeDetected(false)
        }
    }

    fun updateBlocklists() {
        if (_uiState.value.isUpdatingBlocklists) return
        viewModelScope.launch {
            downloadBlocklistsInternal(isInitialDownload = false)
        }
    }

    private suspend fun downloadBlocklistsInternal(isInitialDownload: Boolean) {
        _uiState.value = _uiState.value.copy(
            isUpdatingBlocklists = true,
            updateStatus = if (isInitialDownload) "Setting up blocklists..." else "Updating blocklists..."
        )

        var totalDomains = 0
        val successLines = mutableListOf<String>()
        val failureLines = mutableListOf<String>()

        fun record(label: String, result: BlocklistDownloader.DownloadResult) {
            if (result.success) {
                totalDomains += result.domainsAdded
                successLines.add("$label: ${result.domainsAdded}")
            } else {
                failureLines.add("$label: ${result.error ?: "download failed"}")
            }
        }

        try {
            _uiState.value = _uiState.value.copy(updateStatus = "Downloading filter for adult content...")
            record("Adult", blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT))

            _uiState.value = _uiState.value.copy(updateStatus = "Downloading filter for malware...")
            record("Malware", blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE))

            _uiState.value = _uiState.value.copy(updateStatus = "Downloading filter for gambling...")
            record("Gambling", blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING))

            _uiState.value = _uiState.value.copy(updateStatus = "Downloading filter for social media...")
            record("Social Media", blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.SOCIAL_MEDIA))

            val anyFailed = failureLines.isNotEmpty()
            val anySucceeded = successLines.isNotEmpty()
            val title = when {
                !anySucceeded -> "Update Failed"
                anyFailed -> "Update Incomplete"
                else -> "Update Complete"
            }
            val message = buildString {
                if (anySucceeded) {
                    append("Downloaded $totalDomains domains\n")
                    append(successLines.joinToString("\n"))
                }
                if (anyFailed) {
                    if (isNotEmpty()) append("\n\n")
                    append("Failed:\n")
                    append(failureLines.joinToString("\n"))
                }
                if (!anySucceeded && !anyFailed) {
                    append("No blocklists were updated.")
                }
            }

            _uiState.value = _uiState.value.copy(
                isUpdatingBlocklists = false,
                updateStatus = "",
                showUpdateResult = !isInitialDownload || anyFailed,
                updateResultTitle = title,
                updateResultMessage = message,
                updateResultIsError = !anySucceeded || anyFailed
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isUpdatingBlocklists = false,
                updateStatus = "",
                showUpdateResult = true,
                updateResultTitle = "Update Failed",
                updateResultMessage = "Error downloading blocklists: ${e.message}",
                updateResultIsError = true
            )
        }
    }

    fun dismissUpdateResult() {
        _uiState.value = _uiState.value.copy(showUpdateResult = false)
    }
}
