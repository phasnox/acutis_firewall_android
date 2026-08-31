package com.acutis.firewall.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.acutis.firewall.R
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.ui.components.PinGateContent
import com.acutis.firewall.ui.theme.AcutisFirewallTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PIN prompt shown when someone tries to deactivate the device admin.
 *
 * Important: this cannot actually stop the deactivation - the platform gives an
 * admin no veto over its own removal. It is a speed bump in front of a child plus
 * a trigger for the tamper alert. Only Device Owner mode is non-bypassable.
 */
@AndroidEntryPoint
class PinGateActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = bounceHome()
        })

        // These guards live here, not in the receiver, so none of Settings' 2 second
        // onDisableRequested budget is spent on them.
        lifecycleScope.launch {
            val selfDisable = settingsDataStore.isUninstallProtectionSelfDisable()
            if (!settingsDataStore.hasPin() || selfDisable) {
                finish()
                return@launch
            }
            showPrompt()
        }
    }

    private fun showPrompt() {
        setContent {
            AcutisFirewallTheme {
                var isError by remember { mutableStateOf(false) }
                var attemptsLeft by remember { mutableIntStateOf(MAX_ATTEMPTS) }

                PinGateContent(
                    title = getString(R.string.uninstall_protection_gate_title),
                    message = getString(R.string.uninstall_protection_gate_message),
                    isError = isError,
                    attemptsLeft = attemptsLeft,
                    onPinEntered = { pin ->
                        if (settingsDataStore.verifyPin(pin)) {
                            // Correct PIN: step aside and let Settings proceed.
                            finish()
                        } else {
                            isError = true
                            attemptsLeft -= 1
                            if (attemptsLeft <= 0) bounceHome()
                        }
                    },
                    onCancel = ::bounceHome
                )
            }
        }
    }

    private fun bounceHome() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
