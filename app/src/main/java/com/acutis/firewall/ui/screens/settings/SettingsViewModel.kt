package com.acutis.firewall.ui.screens.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acutis.firewall.admin.UninstallProtectionManager
import com.acutis.firewall.blocklist.BlocklistDownloader
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.preferences.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isPinEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val autoStartEnabled: Boolean = true,
    val showPinSetupDialog: Boolean = false,
    val showPinChangeDialog: Boolean = false,
    val showPinVerifyDialog: Boolean = false,
    val pinError: Boolean = false,
    val pendingAction: SettingsPendingAction? = null,
    val isUninstallProtectionActive: Boolean = false,
    val showUninstallProtectionNeedsPinDialog: Boolean = false,
    val uninstallProtectionError: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: String = "",
    val lastDownloadResult: String? = null
)

enum class SettingsPendingAction {
    DISABLE_PIN,
    CHANGE_PIN,
    DISABLE_UNINSTALL_PROTECTION
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val blocklistDownloader: BlocklistDownloader,
    private val uninstallProtection: UninstallProtectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.pinEnabled,
                settingsDataStore.autoStartEnabled
            ) { pinEnabled, autoStart ->
                _uiState.value.copy(
                    isPinEnabled = pinEnabled,
                    hasPin = settingsDataStore.hasPin(),
                    autoStartEnabled = autoStart
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
        refreshUninstallProtectionState()
    }

    /**
     * The platform, not our DataStore, is the source of truth: the admin can be
     * revoked from system Settings without the app running. Called from the screen
     * on every resume.
     */
    fun refreshUninstallProtectionState() {
        val active = uninstallProtection.isProtectionActive()
        if (active) {
            // Idempotent, and a no-op unless the device was provisioned as device
            // owner via the documented adb setup.
            uninstallProtection.applyDeviceOwnerHardening()
        }
        _uiState.value = _uiState.value.copy(isUninstallProtectionActive = active)
    }

    fun buildAddAdminIntent(): Intent = uninstallProtection.buildAddAdminIntent()

    fun onUninstallProtectionToggle(enabled: Boolean) {
        if (enabled) {
            // Enabling is never PIN-gated, matching onPinToggle and onToggleFirewall.
            // The screen launches the system activation screen; we only block the
            // case where there is no PIN to unlock it with later.
            if (!settingsDataStore.hasPin()) {
                _uiState.value = _uiState.value.copy(showUninstallProtectionNeedsPinDialog = true)
            }
            return
        }

        // Recovery path: clearing app data wipes the PIN but the admin registration
        // survives in system state. Without this the parent is left with an active
        // admin, no PIN, and no way to remove the app.
        if (!settingsDataStore.hasPin()) {
            disableUninstallProtectionInternal()
            return
        }

        _uiState.value = _uiState.value.copy(
            showPinVerifyDialog = true,
            pendingAction = SettingsPendingAction.DISABLE_UNINSTALL_PROTECTION
        )
    }

    private fun disableUninstallProtectionInternal() {
        viewModelScope.launch {
            // Awaited before removeActiveAdmin so onDisabled cannot fire first and
            // report the parent's own removal as tampering.
            settingsDataStore.setUninstallProtectionSelfDisable(true)

            val removed = uninstallProtection.disableProtection()
            settingsDataStore.setUninstallProtectionEnabled(false)

            if (!removed) {
                settingsDataStore.setUninstallProtectionSelfDisable(false)
                _uiState.value = _uiState.value.copy(uninstallProtectionError = true)
            }
            refreshUninstallProtectionState()
        }
    }

    fun dismissUninstallProtectionNeedsPinDialog() {
        _uiState.value = _uiState.value.copy(showUninstallProtectionNeedsPinDialog = false)
    }

    fun clearUninstallProtectionError() {
        _uiState.value = _uiState.value.copy(uninstallProtectionError = false)
    }

    fun onPinToggle(enabled: Boolean) {
        if (enabled) {
            if (!_uiState.value.hasPin) {
                _uiState.value = _uiState.value.copy(showPinSetupDialog = true)
            } else {
                viewModelScope.launch {
                    settingsDataStore.setPinEnabled(true)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                showPinVerifyDialog = true,
                pendingAction = SettingsPendingAction.DISABLE_PIN
            )
        }
    }

    fun onChangePin() {
        _uiState.value = _uiState.value.copy(
            showPinVerifyDialog = true,
            pendingAction = SettingsPendingAction.CHANGE_PIN
        )
    }

    fun onPinSetup(pin: String) {
        settingsDataStore.setPin(pin)
        viewModelScope.launch {
            settingsDataStore.setPinEnabled(true)
        }
        _uiState.value = _uiState.value.copy(
            showPinSetupDialog = false,
            hasPin = true
        )
    }

    fun onPinVerified(pin: String) {
        if (settingsDataStore.verifyPin(pin)) {
            _uiState.value = _uiState.value.copy(
                showPinVerifyDialog = false,
                pinError = false
            )
            when (_uiState.value.pendingAction) {
                SettingsPendingAction.DISABLE_PIN -> {
                    viewModelScope.launch {
                        settingsDataStore.setPinEnabled(false)
                    }
                }
                SettingsPendingAction.CHANGE_PIN -> {
                    _uiState.value = _uiState.value.copy(showPinSetupDialog = true)
                }
                SettingsPendingAction.DISABLE_UNINSTALL_PROTECTION -> {
                    disableUninstallProtectionInternal()
                }
                null -> {}
            }
            _uiState.value = _uiState.value.copy(pendingAction = null)
        } else {
            _uiState.value = _uiState.value.copy(pinError = true)
        }
    }

    fun dismissPinSetupDialog() {
        _uiState.value = _uiState.value.copy(showPinSetupDialog = false)
    }

    fun dismissPinVerifyDialog() {
        _uiState.value = _uiState.value.copy(
            showPinVerifyDialog = false,
            pinError = false,
            pendingAction = null
        )
    }

    fun onAutoStartToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoStartEnabled(enabled)
        }
    }

    fun downloadBlocklists() {
        if (_uiState.value.isDownloading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = "Downloading adult content list...",
                lastDownloadResult = null
            )

            var totalDomains = 0
            val results = mutableListOf<String>()

            // Download adult content list
            val adultResult = blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT)
            if (adultResult.success) {
                totalDomains += adultResult.domainsAdded
                results.add("Adult: ${adultResult.domainsAdded}")
            }

            _uiState.value = _uiState.value.copy(downloadProgress = "Downloading malware list...")

            // Download malware list
            val malwareResult = blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE)
            if (malwareResult.success) {
                totalDomains += malwareResult.domainsAdded
                results.add("Malware: ${malwareResult.domainsAdded}")
            }

            _uiState.value = _uiState.value.copy(downloadProgress = "Downloading gambling list...")

            // Download gambling list
            val gamblingResult = blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING)
            if (gamblingResult.success) {
                totalDomains += gamblingResult.domainsAdded
                results.add("Gambling: ${gamblingResult.domainsAdded}")
            }

            _uiState.value = _uiState.value.copy(downloadProgress = "Downloading social media list...")

            // Download social media list
            val socialMediaResult = blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.SOCIAL_MEDIA)
            if (socialMediaResult.success) {
                totalDomains += socialMediaResult.domainsAdded
                results.add("Social Media: ${socialMediaResult.domainsAdded}")
            }

            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                downloadProgress = "",
                lastDownloadResult = "Downloaded $totalDomains domains\n${results.joinToString(", ")}"
            )
        }
    }

    fun clearDownloadResult() {
        _uiState.value = _uiState.value.copy(lastDownloadResult = null)
    }
}
