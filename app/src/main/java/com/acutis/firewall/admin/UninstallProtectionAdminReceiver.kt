package com.acutis.firewall.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.acutis.firewall.R
import com.acutis.firewall.data.preferences.SettingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Being an active admin is what stops the uninstall; this receiver just keeps our
 * own state in step with the platform's and raises the alarm when protection goes
 * away without the PIN.
 *
 * Uses the @EntryPoint accessor pattern rather than @AndroidEntryPoint, matching
 * BootReceiver. @AndroidEntryPoint would insert a generated superclass between this
 * and DeviceAdminReceiver that overrides onReceive - which is exactly where the
 * callback dispatch lives - and there is no reason to take that risk.
 */
class UninstallProtectionAdminReceiver : DeviceAdminReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AdminEntryPoint {
        fun settingsDataStore(): SettingsDataStore
    }

    private fun dataStore(context: Context): SettingsDataStore =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AdminEntryPoint::class.java
        ).settingsDataStore()

    override fun onEnabled(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = dataStore(appContext)
                store.setUninstallProtectionEnabled(true)
                store.setUninstallProtectionRemoved(false)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Fired when someone taps Deactivate in Settings. This CANNOT veto the removal -
     * the returned text is only a warning shown on the system confirmation dialog.
     *
     * Settings applies a 2000 ms timeout to this call (DeviceAdminAdd posts
     * continueRemoveAction(null) after 2s), so it must return essentially
     * immediately. In particular do not touch SettingsDataStore here: constructing it
     * builds a Keystore MasterKey and opens EncryptedSharedPreferences, which is slow
     * on a cold process.
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        PinGateLauncher.request(context)
        return context.getString(R.string.uninstall_protection_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = dataStore(appContext)
                store.setUninstallProtectionEnabled(false)
                if (store.isUninstallProtectionSelfDisable()) {
                    // The parent turned it off in-app after entering the PIN.
                    store.setUninstallProtectionSelfDisable(false)
                } else {
                    Log.w(TAG, "Device admin removed outside the app - flagging tamper")
                    store.setUninstallProtectionRemoved(true)
                    UninstallProtectionNotifier.showTamperAlert(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "UninstallProtection"
    }
}
