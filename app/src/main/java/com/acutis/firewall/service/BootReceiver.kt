package com.acutis.firewall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acutis.firewall.data.preferences.SettingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun settingsDataStore(): SettingsDataStore
    }

    override fun onReceive(context: Context, intent: Intent) {
        val afterUpdate = intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && !afterUpdate) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        val settingsDataStore = entryPoint.settingsDataStore()

        CoroutineScope(Dispatchers.IO).launch {
            val wasEnabled = settingsDataStore.firewallEnabled.first()
            // An update replaces the package and kills the process, so the VPN dies
            // with it and START_STICKY does not bring it back: the app then claims
            // "Firewall is enabled" while nothing is filtered until the shield is
            // toggled twice. Restore what was on, regardless of autoStart - that
            // setting is about booting, and protection was already running.
            val autoStart = if (afterUpdate) false else settingsDataStore.autoStartEnabled.first()

            if (shouldRestoreVpn(afterUpdate, autoStart, wasEnabled)) {
                val vpnIntent = Intent(context, FirewallVpnService::class.java).apply {
                    action = FirewallVpnService.ACTION_START
                }
                context.startForegroundService(vpnIntent)
            }
        }
    }
}

/**
 * Whether an unsolicited event should bring the VPN back. Never starts protection the
 * user had turned off. A package replacement restores it because it was already
 * running; a boot also honours the auto-start setting.
 */
internal fun shouldRestoreVpn(
    afterPackageReplace: Boolean,
    autoStartEnabled: Boolean,
    firewallWasEnabled: Boolean
): Boolean = firewallWasEnabled && (afterPackageReplace || autoStartEnabled)
