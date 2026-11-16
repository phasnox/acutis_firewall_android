package com.acutis.firewall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.acutis.firewall.ui.navigation.NavGraph
import com.acutis.firewall.ui.theme.AcutisFirewallTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Set when the VPN notification's "Disable Firewall" action was tapped while
         * PIN protection is on. The app opens and routes through the normal PIN
         * prompt instead of the notification stopping the firewall directly.
         */
        const val EXTRA_REQUEST_DISABLE = "com.acutis.firewall.EXTRA_REQUEST_DISABLE"
    }

    /**
     * Without POST_NOTIFICATIONS the foreground service still runs but Android hides
     * its ongoing notification, which is the only place the app shows that filtering
     * is on and the only route to the "Disable Firewall" action. Declaring the
     * permission in the manifest is not enough on Android 13+: it has to be asked for.
     */
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        val requestDisable = intent?.getBooleanExtra(EXTRA_REQUEST_DISABLE, false) == true
        setContent {
            AcutisFirewallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(requestDisableFirewall = requestDisable)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
