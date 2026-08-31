package com.acutis.firewall.admin

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Fire-and-forget launch of the PIN interstitial from the admin receiver.
 *
 * Must stay cheap: it is called from onDisableRequested, which Settings gives 2
 * seconds before it gives up and proceeds without our warning text. All the actual
 * gating (is a PIN even set? is this the parent's own in-app removal?) happens
 * inside PinGateActivity, off that critical path.
 *
 * This deliberately uses an Activity rather than a SYSTEM_ALERT_WINDOW overlay.
 * Settings' DeviceAdminAdd screen sets the OP_SYSTEM_ALERT_WINDOW user restriction
 * for as long as it is resumed ("don't let anyone overlay stuff on top of the
 * screen"), which suppresses every overlay silently - addView succeeds and nothing
 * is ever drawn. An Activity is not subject to that restriction, and showing it
 * pauses DeviceAdminAdd, which lifts the restriction as a side effect.
 */
object PinGateLauncher {

    private const val TAG = "UninstallProtection"

    fun request(context: Context) {
        runCatching {
            val intent = Intent(context, PinGateActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }.onFailure {
            // Background activity launch may be refused depending on OS version and
            // OEM. Protection and the tamper alert still work; only the prompt is lost.
            Log.w(TAG, "Could not show PIN gate", it)
        }
    }
}
