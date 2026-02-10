package com.acutis.firewall.ui.screens.blocklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.CustomBlocklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomListDetailUiState(
    val list: CustomBlocklist? = null,
    val sites: List<BlockedSite> = emptyList(),
    val showAddDomainDialog: Boolean = false,
    val isLoading: Boolean = true,
    val pinEnabled: Boolean = false,
    val showPinDialog: Boolean = false,
    val pinError: Boolean = false
)

sealed class DetailPendingAction {
    data class ToggleDomain(val site: BlockedSite) : DetailPendingAction()
    data class DeleteDomain(val site: BlockedSite) : DetailPendingAction()
}

@HiltViewModel
class CustomListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customBlocklistRepository: CustomBlocklistRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val listId: Long = savedStateHandle.get<Long>("listId") ?: 0L

    private val _uiState = MutableStateFlow(CustomListDetailUiState())
    val uiState: StateFlow<CustomListDetailUiState> = _uiState.asStateFlow()

    private var pendingAction: DetailPendingAction? = null

    init {
        viewModelScope.launch {
            combine(
                customBlocklistRepository.getListById(listId),
                customBlocklistRepository.getSitesInList(listId),
                settingsDataStore.pinEnabled
            ) { list, sites, pinEnabled ->
                CustomListDetailUiState(
                    list = list,
                    sites = sites,
                    showAddDomainDialog = _uiState.value.showAddDomainDialog,
                    isLoading = false,
                    pinEnabled = pinEnabled,
                    showPinDialog = _uiState.value.showPinDialog,
                    pinError = _uiState.value.pinError
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun requiresPinForAction(action: DetailPendingAction): Boolean {
        if (!_uiState.value.pinEnabled) return false
        return when (action) {
            is DetailPendingAction.ToggleDomain -> action.site.isEnabled // disabling requires PIN
            is DetailPendingAction.DeleteDomain -> true
        }
    }

    private fun executeAction(action: DetailPendingAction) {
        viewModelScope.launch {
            when (action) {
                is DetailPendingAction.ToggleDomain -> {
                    customBlocklistRepository.toggleDomainEnabled(action.site)
                }
                is DetailPendingAction.DeleteDomain -> {
                    customBlocklistRepository.removeDomainFromList(action.site)
                }
            }
        }
    }

    private fun requestAction(action: DetailPendingAction) {
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

    fun showAddDomainDialog() {
        _uiState.value = _uiState.value.copy(showAddDomainDialog = true)
    }

    fun hideAddDomainDialog() {
        _uiState.value = _uiState.value.copy(showAddDomainDialog = false)
    }

    fun addDomain(domain: String) {
        viewModelScope.launch {
            customBlocklistRepository.addDomainToList(listId, domain)
            hideAddDomainDialog()
        }
    }

    fun removeDomain(site: BlockedSite) {
        requestAction(DetailPendingAction.DeleteDomain(site))
    }

    fun toggleDomainEnabled(site: BlockedSite) {
        requestAction(DetailPendingAction.ToggleDomain(site))
    }

    fun updateListName(name: String) {
        val currentList = _uiState.value.list ?: return
        viewModelScope.launch {
            customBlocklistRepository.updateList(currentList.copy(name = name))
        }
    }

    fun updateListDescription(description: String) {
        val currentList = _uiState.value.list ?: return
        viewModelScope.launch {
            customBlocklistRepository.updateList(currentList.copy(description = description))
        }
    }
}
