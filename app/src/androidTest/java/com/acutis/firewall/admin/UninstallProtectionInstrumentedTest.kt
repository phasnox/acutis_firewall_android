package com.acutis.firewall.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.acutis.firewall.MainActivity
import com.acutis.firewall.R
import com.acutis.firewall.data.preferences.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Drives the real system Settings screens that uninstall protection depends on: the
 * App info buttons it disables, and the device admin deactivate flow.
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
    private val tamperRecord =
        ComponentName("com.acutis.firewall", "com.acutis.firewall.admin.TamperRecord")

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
            settings.setInitialDownloadPromptShown(true)
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
        // Otherwise the next setUp can re-activate an admin that is still mid-removal.
        awaitAdminRemoved()
        runBlocking { settings.setPinEnabled(false) }
        settings.clearPin()
    }

    /** Sanity: the platform really does consider us an active admin. */
    @Test
    fun deviceAdminActivates() {
        assertTrue("platform does not report us as an active admin", isAdminActive())
    }

    /**
     * With protection on, Settings greys out Force stop: AppButtonsPreferenceController
     * disables it for any package that has an active device admin. The second half is
     * the control - the same button enables once protection is off - so the disabled
     * state is down to the admin, not to the page refreshing late.
     */
    @Test
    fun forceStopIsDisabledWhileProtected() {
        openAppInfo()
        assertStaysDisabled(::forceStopButton, "Force stop")

        turnProtectionOff()
        openAppInfo()
        assertBecomesEnabled(::forceStopButton, "Force stop")
    }

    /** Same for Clear storage, which AppStorageSettings disables on the same check. */
    @Test
    fun clearStorageIsDisabledWhileProtected() {
        openAppStorage()
        assertStaysDisabled(::clearStorageButton, "Clear storage")

        turnProtectionOff()
        openAppStorage()
        assertBecomesEnabled(::clearStorageButton, "Clear storage")
    }

    /**
     * Removing the admin outside the app - what a child does in Settings - has to be
     * recorded beyond app data too, or deactivating and then clearing storage erases the
     * alert. This runs the real onDisabled path: no self-disable flag is set.
     */
    @Test
    fun removalOutsideTheAppIsRecordedBeyondAppData() {
        context.getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(admin)
        assertTrue("device admin still active after removeActiveAdmin", awaitAdminRemoved())

        assertTrue("tamper flag not raised", runBlocking { settings.uninstallProtectionRemoved.first() })
        assertEquals(
            "tamper record not set outside app data",
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            context.packageManager.getComponentEnabledSetting(tamperRecord)
        )
    }

    /**
     * What Clear storage leaves behind: the record without the stored flag. Opening the
     * app must still show the parent the alert.
     */
    @Test
    fun tamperRecordAloneShowsTheAlert() {
        // setUp cleared both copies; restore only the one Clear storage cannot reach.
        context.packageManager.setComponentEnabledSetting(
            tamperRecord,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            val title = context.getString(R.string.uninstall_protection_removed_title)
            val shown = device.wait(Until.hasObject(By.text(title)), UI_TIMEOUT)
            if (!shown) dumpVisibleText()
            assertTrue("'$title' not shown from the record alone", shown)
        }
    }

    /**
     * Proves onDisableRequested ran and returned our string inside the 2000 ms budget
     * Settings allows before it gives up and shows nothing.
     */
    /**
     * Parked after seven emulator rounds without ever reaching a verdict, because the
     * harness cannot reproduce the real conditions.
     *
     * DeviceAdminAdd refuses FLAG_ACTIVITY_NEW_TASK, so it cannot be launched from
     * shell; launching it from our own Activity works, but the device log shows why
     * that is worthless as evidence:
     *
     *     START ... DeviceAdminAdd ... from uid 10210 (BAL_ALLOW_VISIBLE_WINDOW)
     *
     * The start was permitted only because our app had a visible window. In the real
     * flow - a child opening Settings from the launcher - our app has no visible window,
     * so the same start is refused. Any PASS obtained this way would be an artefact of
     * the test, and the screen also disappears before assertions run.
     *
     * The source is unambiguous about intent. DeviceAdminAdd holds an
     * OP_SYSTEM_ALERT_WINDOW user restriction for as long as it is resumed ("don't let
     * anyone overlay stuff on top of the screen") and calls stopAppSwitches() before
     * getRemoveWarning() ("Don't allow the admin to put a dialog up in front of us
     * while we interact with the user"). Both defeat the PIN gate by design.
     *
     * Treat the gate as best-effort. Settling this properly needs a real device, which
     * would also cover the OEM Settings variants an emulator cannot.
     */
    @Ignore("Cannot be reproduced on an emulator - see comment")
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
    /**
     * Parked after seven emulator rounds without ever reaching a verdict, because the
     * harness cannot reproduce the real conditions.
     *
     * DeviceAdminAdd refuses FLAG_ACTIVITY_NEW_TASK, so it cannot be launched from
     * shell; launching it from our own Activity works, but the device log shows why
     * that is worthless as evidence:
     *
     *     START ... DeviceAdminAdd ... from uid 10210 (BAL_ALLOW_VISIBLE_WINDOW)
     *
     * The start was permitted only because our app had a visible window. In the real
     * flow - a child opening Settings from the launcher - our app has no visible window,
     * so the same start is refused. Any PASS obtained this way would be an artefact of
     * the test, and the screen also disappears before assertions run.
     *
     * The source is unambiguous about intent. DeviceAdminAdd holds an
     * OP_SYSTEM_ALERT_WINDOW user restriction for as long as it is resumed ("don't let
     * anyone overlay stuff on top of the screen") and calls stopAppSwitches() before
     * getRemoveWarning() ("Don't allow the admin to put a dialog up in front of us
     * while we interact with the user"). Both defeat the PIN gate by design.
     *
     * Treat the gate as best-effort. Settling this properly needs a real device, which
     * would also cover the OEM Settings variants an emulator cannot.
     */
    @Ignore("Cannot be reproduced on an emulator - see comment")
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

    /**
     * Opens the deactivate screen from our own Activity, without FLAG_ACTIVITY_NEW_TASK.
     *
     * DeviceAdminAdd refuses to run as a new task ("Cannot start ADD_DEVICE_ADMIN as a
     * new task"), which rules out `am start` entirely, and shell-launching Settings'
     * internal list activity is version-fragile. Starting it from a real Activity keeps
     * it in the same task, which the screen accepts.
     *
     * The cost is that our app is briefly foreground, and BAL grants a recently
     * foregrounded app a grace period during which it CAN start activities. That would
     * let the PIN gate appear for a reason it never would in reality, so we wait the
     * grace period out before tapping Deactivate.
     */
    private fun openDeactivateScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.startActivity(
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                )
            }
            val opened = device.wait(Until.hasObject(By.textContains(DEACTIVATE_HINT)), UI_TIMEOUT)
            if (!opened) dumpVisibleText()
            assertTrue(
                "deactivate screen never opened (foreground=${device.currentPackageName})",
                opened
            )
        }

        // Let the BAL grace period lapse so the gate is judged under the same conditions
        // as a child who walked into Settings from the launcher.
        Thread.sleep(BAL_GRACE_WAIT_MS)
    }

    /** Cheap diagnostics so a failure names what was actually on screen. */
    private fun dumpVisibleText() {
        val texts = device.findObjects(By.clazz(Pattern.compile(".*")))
            .mapNotNull { runCatching { it.text }.getOrNull() }
            .filter { it.isNotBlank() }
            .distinct()
        Log.w(TAG, "visible text: $texts")
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

    // BySelector is mutable, so every query gets a fresh one.
    private fun forceStopButton(): BySelector = By.text(Pattern.compile("(?i)force stop"))

    private fun clearStorageButton(): BySelector = By.text(Pattern.compile("(?i)clear storage"))

    /** A fresh App info page every time, so it reads the current admin state. */
    private fun openAppInfo() {
        shell(
            "am start -W --activity-clear-task " +
                "-a android.settings.APPLICATION_DETAILS_SETTINGS -d package:${context.packageName}"
        )
        val opened = device.wait(Until.hasObject(forceStopButton()), UI_TIMEOUT)
        if (!opened) dumpVisibleText()
        assertTrue("App info never showed Force stop (foreground=${device.currentPackageName})", opened)
    }

    private fun openAppStorage() {
        openAppInfo()
        val entry = device.wait(Until.findObject(By.textStartsWith(STORAGE_ENTRY)), SHORT_TIMEOUT)
            ?: device.findObject(By.scrollable(true))?.let { list ->
                list.scroll(Direction.DOWN, 0.8f)
                device.wait(Until.findObject(By.textStartsWith(STORAGE_ENTRY)), SHORT_TIMEOUT)
            }
            ?: run {
                dumpVisibleText()
                error("App info has no '$STORAGE_ENTRY' entry. Foreground=${device.currentPackageName}")
            }
        entry.click()
        val opened = device.wait(Until.hasObject(clearStorageButton()), UI_TIMEOUT)
        if (!opened) dumpVisibleText()
        assertTrue(
            "Storage page never showed Clear storage (foreground=${device.currentPackageName})",
            opened
        )
    }

    /** Fails if the control reads as enabled at any point within [SETTLE_MS]. */
    private fun assertStaysDisabled(label: () -> BySelector, name: String) {
        val deadline = SystemClock.uptimeMillis() + SETTLE_MS
        var seen = false
        while (SystemClock.uptimeMillis() < deadline) {
            when (controlEnabled(label)) {
                true -> fail("$name is enabled while protection is on")
                false -> seen = true
                null -> Unit
            }
            Thread.sleep(POLL_MS)
        }
        assertTrue("$name disappeared from the page", seen)
    }

    private fun assertBecomesEnabled(label: () -> BySelector, name: String) {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (controlEnabled(label) == true) return
            Thread.sleep(POLL_MS)
        }
        dumpVisibleText()
        fail("$name never enabled with protection off, so its disabled state proves nothing")
    }

    /** isEnabled of the control carrying [label], or null if it is not on screen. */
    private fun controlEnabled(label: () -> BySelector): Boolean? = runCatching {
        device.findObjects(label()).firstNotNullOfOrNull { controlFor(it) }?.isEnabled
    }.getOrNull()

    /**
     * The clickable node a label belongs to. On View-based Settings the label is the
     * Button itself. Android 15's App info is Compose: the label is a separate Text node
     * inside the button, it always reports enabled, and only the button carries the state.
     */
    private fun controlFor(label: UiObject2): UiObject2? {
        var node: UiObject2? = label
        while (node != null && !node.isClickable) node = node.parent
        return node
    }

    /** What the in-app switch does once the PIN is accepted: flag it as ours, then remove. */
    private fun turnProtectionOff() {
        runBlocking { settings.setUninstallProtectionSelfDisable(true) }
        context.getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(admin)
        assertTrue("device admin still active after removeActiveAdmin", awaitAdminRemoved())
    }

    /**
     * DPMS drops the admin only after ACTION_DEVICE_ADMIN_DISABLED has been delivered,
     * an ordered broadcast that queues behind everything else on the background queue.
     * On a freshly booted API 33 emulator that took 13 s.
     */
    private fun awaitAdminRemoved(): Boolean {
        val deadline = SystemClock.uptimeMillis() + ADMIN_REMOVAL_TIMEOUT
        while (isAdminActive() && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(POLL_MS)
        }
        return !isAdminActive()
    }

    private companion object {
        const val TAG = "UninstallProtectionTest"
        const val SETTINGS_PKG = "com.android.settings"
        const val TEST_PIN = "1234"
        const val GATE_TITLE = "Ask a parent"
        const val DEACTIVATE_HINT = "eactivate"
        const val UI_TIMEOUT = 10_000L
        const val SHORT_TIMEOUT = 3_000L
        const val STORAGE_ENTRY = "Storage"
        // How long a disabled control must stay disabled. Settings settles it within about
        // 2 s; Android 15's Compose App info computes it off the main thread.
        const val SETTLE_MS = 5_000L
        const val POLL_MS = 100L
        // The background broadcast timeout: past this the queue gives up on the receiver.
        const val ADMIN_REMOVAL_TIMEOUT = 60_000L
        // BAL grace for a recently foregrounded app is ~10s; overshoot it.
        const val BAL_GRACE_WAIT_MS = 15_000L
    }
}
