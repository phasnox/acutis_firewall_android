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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
            val settingsDataStore = entryPoint.settingsDataStore()

            CoroutineScope(Dispatchers.IO).launch {
                val autoStart = settingsDataStore.autoStartEnabled.first()
                val wasEnabled = settingsDataStore.firewallEnabled.first()

                if (autoStart && wasEnabled) {
                    val vpnIntent = Intent(context, FirewallVpnService::class.java).apply {
                        action = FirewallVpnService.ACTION_START
                    }
                    context.startForegroundService(vpnIntent)
                }
            }
        }
    }
}
