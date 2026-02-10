package com.acutis.firewall.ui.screens.home

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acutis.firewall.blocklist.BlocklistDownloader
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.BlocklistRepository
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
    val updateResultMessage: String = ""
)

enum class PendingAction {
    TOGGLE_FIREWALL
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val blocklistRepository: BlocklistRepository,
    private val blocklistDownloader: BlocklistDownloader
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.firewallEnabled,
                settingsDataStore.pinEnabled,
                blocklistRepository.getEnabledCount()
            ) { firewallEnabled, pinEnabled, blockedCount ->
                _uiState.value.copy(
                    isFirewallEnabled = firewallEnabled,
                    isPinEnabled = pinEnabled,
                    blockedSitesCount = blockedCount
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
        val results = mutableListOf<String>()

        try {
            // Download adult content list
            _uiState.value = _uiState.value.copy(updateStatus = "Downloading adult content filter...")
            val adultResult = blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT)
            if (adultResult.success) {
                totalDomains += adultResult.domainsAdded
                results.add("Adult: ${adultResult.domainsAdded}")
            }

            // Download malware list
            _uiState.value = _uiState.value.copy(updateStatus = "Downloading malware filter...")
            val malwareResult = blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE)
            if (malwareResult.success) {
                totalDomains += malwareResult.domainsAdded
                results.add("Malware: ${malwareResult.domainsAdded}")
            }

            // Download gambling list
            _uiState.value = _uiState.value.copy(updateStatus = "Downloading gambling filter...")
            val gamblingResult = blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING)
            if (gamblingResult.success) {
                totalDomains += gamblingResult.domainsAdded
                results.add("Gambling: ${gamblingResult.domainsAdded}")
            }

            _uiState.value = _uiState.value.copy(
                isUpdatingBlocklists = false,
                updateStatus = "",
                showUpdateResult = !isInitialDownload,
                updateResultMessage = "Downloaded $totalDomains domains\n${results.joinToString("\n")}"
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isUpdatingBlocklists = false,
                updateStatus = "",
                showUpdateResult = !isInitialDownload,
                updateResultMessage = "Error downloading blocklists: ${e.message}"
            )
        }
    }

    fun dismissUpdateResult() {
        _uiState.value = _uiState.value.copy(showUpdateResult = false)
    }
}
