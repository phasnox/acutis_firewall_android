package com.acutis.firewall.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.acutis.firewall.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place in the app that touches [DevicePolicyManager].
 *
 * While the receiver is an active device admin the platform refuses to uninstall
 * the package, which is what actually stops a child from removing the app. Every
 * call here is wrapped so that a device which does not support device admin, or an
 * OEM that throws from these APIs, degrades to "protection unavailable" rather than
 * crashing or - worse - leaving the parent unable to remove the app.
 */
@Singleton
class UninstallProtectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val devicePolicyManager: DevicePolicyManager
) {

    val componentName: ComponentName =
        ComponentName(context, UninstallProtectionAdminReceiver::class.java)

    fun isProtectionActive(): Boolean = runCatching {
        devicePolicyManager.isAdminActive(componentName)
    }.getOrElse {
        Log.w(TAG, "isAdminActive failed", it)
        false
    }

    fun isDeviceOwner(): Boolean = runCatching {
        devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }.getOrElse { false }

    /** Intent for the system "activate device admin" screen. */
    fun buildAddAdminIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.uninstall_protection_explanation)
            )
        }

    /**
     * Removes protection. Returns false if the platform refused, in which case the
     * caller must surface a message pointing the parent at system Settings - never
     * leave them stuck with an active admin and no way out.
     */
    @Suppress("DEPRECATION")
    fun disableProtection(): Boolean = runCatching {
        if (isDeviceOwner()) {
            clearDeviceOwnerHardening()
            // removeActiveAdmin always fails for a device owner; this is the only exit.
            devicePolicyManager.clearDeviceOwnerApp(context.packageName)
        } else {
            devicePolicyManager.removeActiveAdmin(componentName)
        }
        true
    }.getOrElse {
        Log.e(TAG, "disableProtection failed", it)
        false
    }

    /**
     * Device-owner-only hardening. Unlike a plain admin this genuinely cannot be
     * removed from Settings, so it is opt-in and documented separately.
     */
    fun applyDeviceOwnerHardening(blockAllUninstalls: Boolean = false) {
        if (!isDeviceOwner()) return
        runCatching {
            devicePolicyManager.setUninstallBlocked(componentName, context.packageName, true)
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_FACTORY_RESET)
            if (blockAllUninstalls) {
                devicePolicyManager.addUserRestriction(
                    componentName,
                    UserManager.DISALLOW_UNINSTALL_APPS
                )
            }
        }.onFailure { Log.e(TAG, "applyDeviceOwnerHardening failed", it) }
    }

    fun clearDeviceOwnerHardening() {
        if (!isDeviceOwner()) return
        runCatching {
            devicePolicyManager.setUninstallBlocked(componentName, context.packageName, false)
            devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.clearUserRestriction(
                componentName,
                UserManager.DISALLOW_UNINSTALL_APPS
            )
        }.onFailure { Log.e(TAG, "clearDeviceOwnerHardening failed", it) }
    }

    private companion object {
        const val TAG = "UninstallProtection"
    }
}
