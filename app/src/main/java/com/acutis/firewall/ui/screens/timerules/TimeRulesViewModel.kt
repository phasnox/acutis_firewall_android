package com.acutis.firewall.ui.screens.timerules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.db.entities.TimeRule
import com.acutis.firewall.data.db.entities.TimeRuleAction
import com.acutis.firewall.data.repository.CustomBlocklistRepository
import com.acutis.firewall.data.repository.TimeRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeRulesUiState(
    val rules: List<TimeRule> = emptyList(),
    val customLists: List<CustomBlocklist> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingRule: TimeRule? = null
)

enum class TargetType {
    DOMAIN,
    PRESET_CATEGORY,
    CUSTOM_LIST
}

data class NewRuleData(
    val targetType: TargetType = TargetType.DOMAIN,
    val domain: String = "",
    val category: BlockCategory? = null,
    val customListId: Long? = null,
    val action: TimeRuleAction = TimeRuleAction.BLOCK,
    val ruleType: RuleType = RuleType.SCHEDULE,
    val dailyLimitMinutes: Int = 60,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 22,
    val endMinute: Int = 0,
    val selectedDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
)

enum class RuleType {
    SCHEDULE,
    DAILY_LIMIT
}

@HiltViewModel
class TimeRulesViewModel @Inject constructor(
    private val timeRuleRepository: TimeRuleRepository,
    private val customBlocklistRepository: CustomBlocklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeRulesUiState())
    val uiState: StateFlow<TimeRulesUiState> = _uiState.asStateFlow()

    private val _newRuleData = MutableStateFlow(NewRuleData())
    val newRuleData: StateFlow<NewRuleData> = _newRuleData.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                timeRuleRepository.getAllRules(),
                customBlocklistRepository.getAllLists()
            ) { rules, customLists ->
                _uiState.value.copy(
                    rules = rules,
                    customLists = customLists
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun showAddDialog() {
        _newRuleData.value = NewRuleData()
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingRule = null)
    }

    fun showEditDialog(rule: TimeRule) {
        val targetType = when {
            rule.customListId != null -> TargetType.CUSTOM_LIST
            rule.category != null -> TargetType.PRESET_CATEGORY
            else -> TargetType.DOMAIN
        }
        _newRuleData.value = NewRuleData(
            targetType = targetType,
            domain = rule.domain ?: "",
            category = rule.category,
            customListId = rule.customListId,
            action = rule.action,
            ruleType = if (rule.dailyLimitMinutes != null) RuleType.DAILY_LIMIT else RuleType.SCHEDULE,
            dailyLimitMinutes = rule.dailyLimitMinutes ?: 60,
            startHour = rule.startHour ?: 8,
            startMinute = rule.startMinute ?: 0,
            endHour = rule.endHour ?: 22,
            endMinute = rule.endMinute ?: 0,
            selectedDays = rule.daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
        )
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingRule = rule)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingRule = null)
    }

    fun updateNewRuleData(data: NewRuleData) {
        _newRuleData.value = data
    }

    fun saveRule() {
        val data = _newRuleData.value
        val editingRule = _uiState.value.editingRule

        // Determine which target to use based on target type
        val domain = when (data.targetType) {
            TargetType.DOMAIN -> data.domain.takeIf { it.isNotBlank() }
            else -> null
        }
        val category = when (data.targetType) {
            TargetType.PRESET_CATEGORY -> data.category
            else -> null
        }
        val customListId = when (data.targetType) {
            TargetType.CUSTOM_LIST -> data.customListId
            else -> null
        }

        viewModelScope.launch {
            val rule = when (data.ruleType) {
                RuleType.SCHEDULE -> timeRuleRepository.createScheduleRule(
                    domain = domain,
                    category = category,
                    customListId = customListId,
                    action = data.action,
                    startHour = data.startHour,
                    startMinute = data.startMinute,
                    endHour = data.endHour,
                    endMinute = data.endMinute,
                    daysOfWeek = data.selectedDays
                )
                RuleType.DAILY_LIMIT -> timeRuleRepository.createDailyLimitRule(
                    domain = domain,
                    category = category,
                    customListId = customListId,
                    action = data.action,
                    limitMinutes = data.dailyLimitMinutes,
                    daysOfWeek = data.selectedDays
                )
            }

            if (editingRule != null) {
                timeRuleRepository.updateRule(rule.copy(id = editingRule.id))
            } else {
                timeRuleRepository.addRule(rule)
            }

            hideAddDialog()
        }
    }

    fun deleteRule(rule: TimeRule) {
        viewModelScope.launch {
            timeRuleRepository.deleteRule(rule)
        }
    }

    fun toggleRule(rule: TimeRule) {
        viewModelScope.launch {
            timeRuleRepository.setRuleEnabled(rule.id, !rule.isEnabled)
        }
    }
}
