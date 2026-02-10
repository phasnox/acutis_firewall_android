package com.acutis.firewall.ui.screens.blocklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acutis.firewall.R
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.ui.components.AddDomainDialog
import com.acutis.firewall.ui.components.BlockedSiteItem
import com.acutis.firewall.ui.components.CategoryToggleCard
import com.acutis.firewall.ui.components.PinDialog
import com.acutis.firewall.ui.theme.FirewallColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToListDetail: (Long) -> Unit,
    viewModel: BlocklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showAddDialog) {
        AddDomainDialog(
            onDismiss = viewModel::hideAddDialog,
            onAdd = viewModel::addCustomSite
        )
    }

    if (uiState.showAddListDialog) {
        AddListDialog(
            onDismiss = viewModel::hideAddListDialog,
            onAdd = viewModel::createList
        )
    }

    if (uiState.showPinDialog) {
        PinDialog(
            onDismiss = viewModel::dismissPinDialog,
            onPinEntered = viewModel::verifyPin,
            isError = uiState.pinError
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blocked_sites)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == 1) {
                FloatingActionButton(
                    onClick = viewModel::showAddListDialog
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add list")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = uiState.selectedTab
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text(stringResource(R.string.default_blocklists)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text(stringResource(R.string.custom_blocklist)) }
                )
            }

            when (uiState.selectedTab) {
                0 -> DefaultBlocklistsTab(uiState, viewModel)
                1 -> CustomBlocklistTab(uiState, viewModel, onNavigateToListDetail)
            }
        }
    }
}

@Composable
private fun DefaultBlocklistsTab(
    uiState: BlocklistUiState,
    viewModel: BlocklistViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            CategoryToggleCard(
                title = stringResource(R.string.adult_content),
                description = "Block adult and pornographic websites",
                count = uiState.adultCount,
                isEnabled = uiState.adultEnabled,
                color = FirewallColors.adult,
                onToggle = viewModel::toggleAdultBlock
            )
        }
        item {
            CategoryToggleCard(
                title = stringResource(R.string.malware),
                description = "Block known malware and phishing sites",
                count = uiState.malwareCount,
                isEnabled = uiState.malwareEnabled,
                color = FirewallColors.malware,
                onToggle = viewModel::toggleMalwareBlock
            )
        }
        item {
            CategoryToggleCard(
                title = stringResource(R.string.gambling),
                description = "Block gambling and betting websites",
                count = uiState.gamblingCount,
                isEnabled = uiState.gamblingEnabled,
                color = FirewallColors.gambling,
                onToggle = viewModel::toggleGamblingBlock
            )
        }
        item {
            CategoryToggleCard(
                title = stringResource(R.string.social_media),
                description = "Block social media platforms",
                count = uiState.socialMediaCount,
                isEnabled = uiState.socialMediaEnabled,
                color = FirewallColors.socialMedia,
                onToggle = viewModel::toggleSocialMediaBlock
            )
        }
    }
}

@Composable
private fun CustomBlocklistTab(
    uiState: BlocklistUiState,
    viewModel: BlocklistViewModel,
    onNavigateToListDetail: (Long) -> Unit
) {
    if (uiState.customLists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No custom lists created.\nTap + to create a list.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.customLists, key = { it.id }) { list ->
                CustomListItem(
                    list = list,
                    onClick = { onNavigateToListDetail(list.id) },
                    onToggle = { viewModel.toggleListEnabled(list) },
                    onDelete = { viewModel.deleteList(list) }
                )
            }
        }
    }
}

@Composable
private fun CustomListItem(
    list: CustomBlocklist,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (list.isEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (list.description.isNotEmpty()) {
                    Text(
                        text = list.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = list.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun AddListDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create List") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name") },
                    placeholder = { Text("e.g., Work Distractions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("e.g., Sites to block during work") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
