package com.acutis.firewall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}
