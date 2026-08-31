package com.acutis.firewall.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.acutis.firewall.MainActivity
import com.acutis.firewall.R

/**
 * Tamper alert for when uninstall protection is removed without the PIN.
 *
 * This is only half the story: POST_NOTIFICATIONS is never requested at runtime,
 * so on Android 13+ this may be silently dropped. The in-app warning driven by
 * SettingsDataStore.uninstallProtectionRemoved is the reliable channel.
 */
object UninstallProtectionNotifier {

    private const val TAG = "UninstallProtection"
    private const val TAMPER_NOTIFICATION_ID = 3

    // Must match FirewallVpnService.WARNING_CHANNEL_ID. Channel creation is
    // idempotent, but the first creator wins on name and importance, so these
    // are kept identical on purpose.
    private const val WARNING_CHANNEL_ID = "firewall_warning_channel"

    fun showTamperAlert(context: Context) {
        runCatching {
            ensureChannel(context)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val message = context.getString(R.string.uninstall_protection_removed_message)
            val notification = NotificationCompat.Builder(context, WARNING_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.uninstall_protection_removed_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            context.getSystemService(NotificationManager::class.java)
                .notify(TAMPER_NOTIFICATION_ID, notification)
        }.onFailure { Log.e(TAG, "Failed to post tamper alert", it) }
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            WARNING_CHANNEL_ID,
            "Firewall Warnings",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important warnings about firewall configuration"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
