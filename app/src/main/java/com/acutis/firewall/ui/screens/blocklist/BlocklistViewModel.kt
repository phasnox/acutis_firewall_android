package com.acutis.firewall.ui.screens.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.BlocklistRepository
import com.acutis.firewall.data.repository.CustomBlocklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlocklistUiState(
    val sites: List<BlockedSite> = emptyList(),
    val customSites: List<BlockedSite> = emptyList(),
    val customLists: List<CustomBlocklist> = emptyList(),
    val adultEnabled: Boolean = true,
    val malwareEnabled: Boolean = true,
    val gamblingEnabled: Boolean = false,
    val socialMediaEnabled: Boolean = false,
    val adultCount: Int = 0,
    val malwareCount: Int = 0,
    val gamblingCount: Int = 0,
    val socialMediaCount: Int = 0,
    val showAddDialog: Boolean = false,
    val showAddListDialog: Boolean = false,
    val selectedTab: Int = 0,
    val pinEnabled: Boolean = false,
    val showPinDialog: Boolean = false,
    val pinError: Boolean = false
)

sealed class PendingAction {
    data class ToggleAdult(val enabled: Boolean) : PendingAction()
    data class ToggleMalware(val enabled: Boolean) : PendingAction()
    data class ToggleGambling(val enabled: Boolean) : PendingAction()
    data class ToggleSocialMedia(val enabled: Boolean) : PendingAction()
    data class ToggleList(val list: CustomBlocklist) : PendingAction()
    data class DeleteList(val list: CustomBlocklist) : PendingAction()
    data class ToggleSite(val site: BlockedSite) : PendingAction()
    data class DeleteSite(val site: BlockedSite) : PendingAction()
}

@HiltViewModel
class BlocklistViewModel @Inject constructor(
    private val blocklistRepository: BlocklistRepository,
    private val customBlocklistRepository: CustomBlocklistRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlocklistUiState())
    val uiState: StateFlow<BlocklistUiState> = _uiState.asStateFlow()

    private var pendingAction: PendingAction? = null

    init {
        viewModelScope.launch {
            val sitesFlow = combine(
                blocklistRepository.getAllSites(),
                blocklistRepository.getCustomSites(),
                customBlocklistRepository.getAllLists()
            ) { sites, customSites, customLists ->
                Triple(sites, customSites, customLists)
            }

            val settingsFlow = combine(
                settingsDataStore.adultBlockEnabled,
                settingsDataStore.malwareBlockEnabled,
                settingsDataStore.gamblingBlockEnabled,
                settingsDataStore.socialMediaBlockEnabled,
                settingsDataStore.pinEnabled
            ) { adult, malware, gambling, social, pinEnabled ->
                listOf(adult, malware, gambling, social, pinEnabled)
            }

            combine(sitesFlow, settingsFlow) { (sites, customSites, customLists), settings ->
                BlocklistUiState(
                    sites = sites,
                    customSites = customSites.filter { it.customListId == null },
                    customLists = customLists,
                    adultEnabled = settings[0],
                    malwareEnabled = settings[1],
                    gamblingEnabled = settings[2],
                    socialMediaEnabled = settings[3],
                    adultCount = sites.count { it.category == BlockCategory.ADULT },
                    malwareCount = sites.count { it.category == BlockCategory.MALWARE },
                    gamblingCount = sites.count { it.category == BlockCategory.GAMBLING },
                    socialMediaCount = sites.count { it.category == BlockCategory.SOCIAL_MEDIA },
                    showAddDialog = _uiState.value.showAddDialog,
                    showAddListDialog = _uiState.value.showAddListDialog,
                    selectedTab = _uiState.value.selectedTab,
                    pinEnabled = settings[4],
                    showPinDialog = _uiState.value.showPinDialog,
                    pinError = _uiState.value.pinError
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun requiresPinForAction(action: PendingAction): Boolean {
        if (!_uiState.value.pinEnabled) return false
        // PIN is required for disabling/deleting actions
        return when (action) {
            is PendingAction.ToggleAdult -> !action.enabled
            is PendingAction.ToggleMalware -> !action.enabled
            is PendingAction.ToggleGambling -> !action.enabled
            is PendingAction.ToggleSocialMedia -> !action.enabled
            is PendingAction.ToggleList -> action.list.isEnabled // disabling requires PIN
            is PendingAction.DeleteList -> true
            is PendingAction.ToggleSite -> action.site.isEnabled // disabling requires PIN
            is PendingAction.DeleteSite -> true
        }
    }

    private fun executeAction(action: PendingAction) {
        viewModelScope.launch {
            when (action) {
                is PendingAction.ToggleAdult -> {
                    settingsDataStore.setAdultBlockEnabled(action.enabled)
                    blocklistRepository.setCategoryEnabled(BlockCategory.ADULT, action.enabled)
                }
                is PendingAction.ToggleMalware -> {
                    settingsDataStore.setMalwareBlockEnabled(action.enabled)
                    blocklistRepository.setCategoryEnabled(BlockCategory.MALWARE, action.enabled)
                }
                is PendingAction.ToggleGambling -> {
                    settingsDataStore.setGamblingBlockEnabled(action.enabled)
                    blocklistRepository.setCategoryEnabled(BlockCategory.GAMBLING, action.enabled)
                }
                is PendingAction.ToggleSocialMedia -> {
                    settingsDataStore.setSocialMediaBlockEnabled(action.enabled)
                    blocklistRepository.setCategoryEnabled(BlockCategory.SOCIAL_MEDIA, action.enabled)
                }
                is PendingAction.ToggleList -> {
                    customBlocklistRepository.toggleListEnabled(action.list.id, !action.list.isEnabled)
                }
                is PendingAction.DeleteList -> {
                    customBlocklistRepository.deleteList(action.list.id)
                }
                is PendingAction.ToggleSite -> {
                    blocklistRepository.toggleSite(action.site)
                }
                is PendingAction.DeleteSite -> {
                    blocklistRepository.removeSite(action.site)
                }
            }
        }
    }

    private fun requestAction(action: PendingAction) {
        if (requiresPinForAction(action)) {
            pendingAction = action
            _uiState.value = _uiState.value.copy(showPinDialog = true, pinError = false)
        } else {
            executeAction(action)
        }
    }

    fun verifyPin(pin: String) {
        if (settingsDataStore.verifyPin(pin)) {
            _uiState.value = _uiState.value.copy(showPinDialog = false, pinError = false)
            pendingAction?.let { executeAction(it) }
            pendingAction = null
        } else {
            _uiState.value = _uiState.value.copy(pinError = true)
        }
    }

    fun dismissPinDialog() {
        pendingAction = null
        _uiState.value = _uiState.value.copy(showPinDialog = false, pinError = false)
    }

    fun setSelectedTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun toggleAdultBlock(enabled: Boolean) {
        requestAction(PendingAction.ToggleAdult(enabled))
    }

    fun toggleMalwareBlock(enabled: Boolean) {
        requestAction(PendingAction.ToggleMalware(enabled))
    }

    fun toggleGamblingBlock(enabled: Boolean) {
        requestAction(PendingAction.ToggleGambling(enabled))
    }

    fun toggleSocialMediaBlock(enabled: Boolean) {
        requestAction(PendingAction.ToggleSocialMedia(enabled))
    }

    fun toggleSite(site: BlockedSite) {
        requestAction(PendingAction.ToggleSite(site))
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addCustomSite(domain: String) {
        viewModelScope.launch {
            blocklistRepository.addCustomSite(domain)
            hideAddDialog()
        }
    }

    fun deleteCustomSite(site: BlockedSite) {
        requestAction(PendingAction.DeleteSite(site))
    }

    // Custom list management
    fun showAddListDialog() {
        _uiState.value = _uiState.value.copy(showAddListDialog = true)
    }

    fun hideAddListDialog() {
        _uiState.value = _uiState.value.copy(showAddListDialog = false)
    }

    fun createList(name: String, description: String = "") {
        viewModelScope.launch {
            customBlocklistRepository.createList(name, description)
            hideAddListDialog()
        }
    }

    fun deleteList(list: CustomBlocklist) {
        requestAction(PendingAction.DeleteList(list))
    }

    fun toggleListEnabled(list: CustomBlocklist) {
        requestAction(PendingAction.ToggleList(list))
    }
}
