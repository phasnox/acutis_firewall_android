package com.acutis.firewall.ui.screens.home

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acutis.firewall.R
import com.acutis.firewall.ui.components.PinDialog
import com.acutis.firewall.ui.theme.FirewallColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBlocklist: () -> Unit,
    onNavigateToTimeRules: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onToggleFirewall()
        }
    }

    if (uiState.showPinDialog) {
        PinDialog(
            onDismiss = viewModel::onPinDialogDismiss,
            onPinEntered = viewModel::onPinEntered,
            isError = uiState.pinError
        )
    }

    if (uiState.showUpdateResult) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateResult,
            title = { Text("Update Complete") },
            text = { Text(uiState.updateResultMessage) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissUpdateResult) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.showVpnConflictAlert) {
        AlertDialog(
            onDismissRequest = viewModel::dismissVpnConflictAlert,
            title = { Text("VPN Conflict") },
            text = { Text("Another VPN is currently active. Please disable the other VPN before enabling Acutis Firewall.") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissVpnConflictAlert) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.showLockdownWarning) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = viewModel::dismissLockdownWarning,
            title = { Text("VPN Lockdown Mode Detected") },
            text = {
                Text(
                    "\"Block connections without VPN\" is enabled in your device settings. " +
                    "Acutis Firewall only filters DNS traffic, so other connections may be blocked.\n\n" +
                    "Disable \"Block connections without VPN\" in VPN settings to fix this."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                        viewModel.dismissLockdownWarning()
                    }
                ) {
                    Text("Open VPN Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLockdownWarning) {
                    Text("Dismiss")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Update status banner
            if (uiState.isUpdatingBlocklists) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.updateStatus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main shield toggle
            ShieldToggle(
                isEnabled = uiState.isFirewallEnabled,
                isToggling = uiState.isTogglingFirewall,
                onClick = {
                    // Don't allow clicks while toggling
                    if (uiState.isTogglingFirewall) return@ShieldToggle

                    // Only check for VPN conflict when enabling (not disabling)
                    if (!uiState.isFirewallEnabled && viewModel.isOtherVpnActive()) {
                        viewModel.showVpnConflictAlert()
                        return@ShieldToggle
                    }

                    val intent = viewModel.checkVpnPermission()
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    } else {
                        viewModel.onToggleFirewall()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status text with loading indicator
            if (uiState.isTogglingFirewall) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isFirewallEnabled) "Deactivating..." else "Activating...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = if (uiState.isFirewallEnabled) {
                        stringResource(R.string.firewall_enabled)
                    } else {
                        stringResource(R.string.firewall_disabled)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isFirewallEnabled) FirewallColors.enabled else FirewallColors.disabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sites_blocked_count, uiState.blockedSitesCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Update blocklists button
            OutlinedButton(
                onClick = viewModel::updateBlocklists,
                enabled = !uiState.isUpdatingBlocklists,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Blocklists")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionCard(
                    icon = Icons.Default.Block,
                    title = stringResource(R.string.blocked_sites),
                    onClick = onNavigateToBlocklist,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                QuickActionCard(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.time_rules),
                    onClick = onNavigateToTimeRules,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer info
            if (!uiState.isTogglingFirewall) {
                Text(
                    text = "Tap the shield to ${if (uiState.isFirewallEnabled) "disable" else "enable"} protection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ShieldToggle(
    isEnabled: Boolean,
    isToggling: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isEnabled) FirewallColors.enabled else FirewallColors.disabled,
        label = "background"
    )
    val scale by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.95f,
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            isToggling -> 0.3f
            isEnabled -> 1f
            else -> 0.5f
        },
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
            .background(backgroundColor.copy(alpha = 0.1f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isToggling,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.acutis_logo),
            contentDescription = if (isEnabled) "Protection enabled" else "Protection disabled",
            modifier = Modifier
                .size(160.dp)
                .scale(alpha),
            alpha = alpha
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
