package com.acutis.firewall.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isDownloading: Boolean = false,
    val downloadProgress: String = "",
    val lastDownloadResult: String? = null
)

enum class SettingsPendingAction {
    DISABLE_PIN,
    CHANGE_PIN
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val blocklistDownloader: BlocklistDownloader
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
