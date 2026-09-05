package com.acutis.firewall.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.acutis.firewall.data.preferences.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Drives the real system Settings deactivate flow to answer the one question unit
 * tests cannot: when someone taps "Deactivate" on the device admin screen, does our
 * PIN gate actually reach the foreground?
 *
 * Deliberately does NOT use a Hilt test runner. These tests need the real
 * AcutisFirewallApp running with its real Hilt graph, because the thing under test is
 * the platform's interaction with our real manifest-registered receiver. Swapping in
 * HiltTestApplication would replace the application class and test something else.
 * SettingsDataStore is constructed directly - it takes only a Context, and the
 * `by preferencesDataStore` delegate hands back the same underlying DataStore.
 */
@RunWith(AndroidJUnit4::class)
class UninstallProtectionInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val admin = ComponentName(
        "com.acutis.firewall",
        "com.acutis.firewall.admin.UninstallProtectionAdminReceiver"
    )
    private val adminArg = admin.flattenToString()

    private lateinit var settings: SettingsDataStore

    private fun shell(cmd: String): String =
        device.executeShellCommand(cmd).also { Log.i(TAG, "\$ $cmd\n$it") }

    private fun isAdminActive(): Boolean =
        context.getSystemService(DevicePolicyManager::class.java).isAdminActive(admin)

    @Before
    fun setUp() {
        device.wakeUp()
        device.pressHome()

        settings = SettingsDataStore(context)
        settings.setPin(TEST_PIN)
        runBlocking {
            settings.setPinEnabled(true)
            settings.setUninstallProtectionSelfDisable(false)
            settings.setUninstallProtectionRemoved(false)
        }

        shell("dpm set-active-admin --user 0 $adminArg")
        // If the emulator image will not grant shell MANAGE_DEVICE_ADMINS there is
        // nothing to test; skip loudly rather than report a false failure.
        assumeTrue("Could not activate device admin on this image", isAdminActive())
    }

    @After
    fun tearDown() {
        repeat(3) { device.pressBack() }
        device.pressHome()
        // In-process self-removal, not `dpm remove-active-admin`: the dpm path calls
        // forceRemoveActiveAdmin, which refuses a non-testOnly admin. An admin removing
        // itself is always permitted, so cleanup works on any build.
        runCatching {
            context.getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(admin)
        }
        runBlocking { settings.setPinEnabled(false) }
        settings.clearPin()
    }

    /** Sanity: the platform really does consider us an active admin. */
    @Test
    fun deviceAdminActivates() {
        assertTrue("platform does not report us as an active admin", isAdminActive())
    }

    /**
     * Proves onDisableRequested ran and returned our string inside the 2000 ms budget
     * Settings allows before it gives up and shows nothing.
     */
    @Test
    fun deactivateShowsOurWarningText() {
        openDeactivateScreen()
        tapDeactivate()

        val shown = device.wait(
            Until.hasObject(By.textContains("a parent will be notified")),
            UI_TIMEOUT
        )
        assertTrue(
            "onDisableRequested warning never reached the confirmation dialog; " +
                "foreground=${device.currentPackageName}",
            shown
        )
    }

    /**
     * THE open question. Settings calls ActivityManager.stopAppSwitches() immediately
     * before getRemoveWarning() - "Don't allow the admin to put a dialog up in front of
     * us while we interact with the user" - and holds an OP_SYSTEM_ALERT_WINDOW user
     * restriction for as long as it is resumed. If this fails, the PIN gate does not
     * work on stock Android and the docs must stop claiming it does.
     */
    @Test
    fun pinGateReachesForegroundOnDeactivate() {
        openDeactivateScreen()
        tapDeactivate()

        val appeared = device.wait(
            Until.hasObject(By.text(GATE_TITLE)),
            UI_TIMEOUT
        )
        val foreground = device.currentPackageName
        Log.i(TAG, "foreground package after Deactivate = $foreground")
        assertTrue(
            "PIN gate did not reach the foreground after tapping Deactivate " +
                "(foreground=$foreground). Settings calls stopAppSwitches() before " +
                "getRemoveWarning(), which blocks background activity starts.",
            appeared
        )
    }

    private fun openDeactivateScreen() {
        // With the admin already active, DeviceAdminAdd renders its "Deactivate"
        // variant rather than the activation prompt.
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        assertTrue(
            "Settings device-admin screen never opened",
            device.wait(Until.hasObject(By.pkg(SETTINGS_PKG).depth(0)), UI_TIMEOUT)
        )
    }

    private fun tapDeactivate() {
        // Resource ids first, visible text last - the label differs across versions.
        val candidates = listOf<BySelector>(
            By.res(SETTINGS_PKG, "restricted_action"),
            By.res(SETTINGS_PKG, "action_button"),
            By.text(Pattern.compile("(?i).*deactivate.*"))
        )
        val target = candidates.firstNotNullOfOrNull { sel ->
            device.wait(Until.findObject(sel), SHORT_TIMEOUT)
        } ?: error("No deactivate control found. Foreground=${device.currentPackageName}")
        target.click()
    }

    private companion object {
        const val TAG = "UninstallProtectionTest"
        const val SETTINGS_PKG = "com.android.settings"
        const val TEST_PIN = "1234"
        const val GATE_TITLE = "Ask a parent"
        const val UI_TIMEOUT = 10_000L
        const val SHORT_TIMEOUT = 3_000L
    }
}
