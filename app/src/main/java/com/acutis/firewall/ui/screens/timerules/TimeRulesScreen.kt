package com.acutis.firewall.ui.screens.timerules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acutis.firewall.R
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.db.entities.TimeRule
import com.acutis.firewall.data.db.entities.TimeRuleAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimeRulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val newRuleData by viewModel.newRuleData.collectAsState()

    if (uiState.showAddDialog) {
        AddEditRuleDialog(
            data = newRuleData,
            customLists = uiState.customLists,
            isEditing = uiState.editingRule != null,
            onDataChange = viewModel::updateNewRuleData,
            onSave = viewModel::saveRule,
            onDismiss = viewModel::hideAddDialog
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.time_rules)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        }
    ) { padding ->
        if (uiState.rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No time rules set.\nTap + to create a rule.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.rules, key = { it.id }) { rule ->
                    TimeRuleItem(
                        rule = rule,
                        customLists = uiState.customLists,
                        onToggle = { viewModel.toggleRule(rule) },
                        onEdit = { viewModel.showEditDialog(rule) },
                        onDelete = { viewModel.deleteRule(rule) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRuleItem(
    rule: TimeRule,
    customLists: List<CustomBlocklist>,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val targetName = when {
        rule.domain != null -> rule.domain
        rule.category != null -> "Category: ${rule.category.name}"
        rule.customListId != null -> {
            val listName = customLists.find { it.id == rule.customListId }?.name ?: "Unknown List"
            "List: $listName"
        }
        else -> "All sites"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = targetName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildRuleDescription(rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDaysOfWeek(rule.daysOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

private fun buildRuleDescription(rule: TimeRule): String {
    val action = if (rule.action == TimeRuleAction.BLOCK) "Block" else "Allow"

    return if (rule.dailyLimitMinutes != null) {
        "$action for ${rule.dailyLimitMinutes} min/day"
    } else if (rule.startHour != null && rule.endHour != null) {
        val startTime = String.format("%02d:%02d", rule.startHour, rule.startMinute ?: 0)
        val endTime = String.format("%02d:%02d", rule.endHour, rule.endMinute ?: 0)
        "$action from $startTime to $endTime"
    } else {
        action
    }
}

private fun formatDaysOfWeek(daysStr: String): String {
    val days = daysStr.split(",").mapNotNull { it.toIntOrNull() }
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    return if (days.size == 7) {
        "Every day"
    } else if (days == listOf(1, 2, 3, 4, 5)) {
        "Weekdays"
    } else if (days == listOf(6, 7)) {
        "Weekends"
    } else {
        days.map { dayNames.getOrNull(it - 1) ?: "" }.joinToString(", ")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditRuleDialog(
    data: NewRuleData,
    customLists: List<CustomBlocklist>,
    isEditing: Boolean,
    onDataChange: (NewRuleData) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var customListExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Rule" else "Add Rule")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Target type selection
                Text("Apply to", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TargetType.entries.forEach { targetType ->
                        Row(
                            modifier = Modifier
                                .selectable(
                                    selected = data.targetType == targetType,
                                    onClick = { onDataChange(data.copy(targetType = targetType)) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = data.targetType == targetType,
                                onClick = null
                            )
                            Text(
                                text = when (targetType) {
                                    TargetType.DOMAIN -> "Domain"
                                    TargetType.PRESET_CATEGORY -> "Category"
                                    TargetType.CUSTOM_LIST -> "Custom List"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target input based on type
                when (data.targetType) {
                    TargetType.DOMAIN -> {
                        OutlinedTextField(
                            value = data.domain,
                            onValueChange = { onDataChange(data.copy(domain = it)) },
                            label = { Text("Domain") },
                            placeholder = { Text("youtube.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TargetType.PRESET_CATEGORY -> {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = data.category?.name ?: "Select category",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                BlockCategory.entries.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            onDataChange(data.copy(category = category))
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    TargetType.CUSTOM_LIST -> {
                        if (customLists.isEmpty()) {
                            Text(
                                text = "No custom lists available. Create a custom list first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = customListExpanded,
                                onExpandedChange = { customListExpanded = it }
                            ) {
                                val selectedListName = customLists.find { it.id == data.customListId }?.name
                                    ?: "Select list"
                                OutlinedTextField(
                                    value = selectedListName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Custom List") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customListExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = customListExpanded,
                                    onDismissRequest = { customListExpanded = false }
                                ) {
                                    customLists.forEach { list ->
                                        DropdownMenuItem(
                                            text = { Text(list.name) },
                                            onClick = {
                                                onDataChange(data.copy(customListId = list.id))
                                                customListExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Action", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeRuleAction.entries.forEach { action ->
                        Row(
                            modifier = Modifier
                                .selectable(
                                    selected = data.action == action,
                                    onClick = { onDataChange(data.copy(action = action)) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = data.action == action,
                                onClick = null
                            )
                            Text(action.name, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Rule Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RuleType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .selectable(
                                    selected = data.ruleType == type,
                                    onClick = { onDataChange(data.copy(ruleType = type)) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = data.ruleType == type,
                                onClick = null
                            )
                            Text(
                                text = if (type == RuleType.SCHEDULE) "Schedule" else "Daily Limit",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (data.ruleType) {
                    RuleType.SCHEDULE -> {
                        // Convert hour:minute to slot index (0-47 for 30-min intervals)
                        val startSlot = data.startHour * 2 + (data.startMinute / 30)
                        val endSlot = data.endHour * 2 + (data.endMinute / 30)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Start", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${String.format("%02d", data.startHour)}:${String.format("%02d", data.startMinute)}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Column {
                                Text("End", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${String.format("%02d", data.endHour)}:${String.format("%02d", data.endMinute)}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Text("Start time", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = startSlot.toFloat(),
                            onValueChange = { slot ->
                                val slotInt = slot.toInt()
                                val hour = slotInt / 2
                                val minute = (slotInt % 2) * 30
                                onDataChange(data.copy(startHour = hour, startMinute = minute))
                            },
                            valueRange = 0f..47f,
                            steps = 46
                        )
                        Text("End time", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = endSlot.toFloat(),
                            onValueChange = { slot ->
                                val slotInt = slot.toInt()
                                val hour = slotInt / 2
                                val minute = (slotInt % 2) * 30
                                onDataChange(data.copy(endHour = hour, endMinute = minute))
                            },
                            valueRange = 0f..47f,
                            steps = 46
                        )
                    }
                    RuleType.DAILY_LIMIT -> {
                        Text("Daily limit: ${data.dailyLimitMinutes} minutes")
                        Slider(
                            value = data.dailyLimitMinutes.toFloat(),
                            onValueChange = { onDataChange(data.copy(dailyLimitMinutes = it.toInt())) },
                            valueRange = 15f..480f,
                            steps = 30
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Days", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                        val dayNum = index + 1
                        val isSelected = data.selectedDays.contains(dayNum)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newDays = if (isSelected) {
                                    data.selectedDays - dayNum
                                } else {
                                    data.selectedDays + dayNum
                                }
                                onDataChange(data.copy(selectedDays = newDays.sorted()))
                            },
                            label = { Text(day) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isValid = when (data.targetType) {
                TargetType.DOMAIN -> data.domain.isNotBlank()
                TargetType.PRESET_CATEGORY -> data.category != null
                TargetType.CUSTOM_LIST -> data.customListId != null
            }
            TextButton(
                onClick = onSave,
                enabled = isValid
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
