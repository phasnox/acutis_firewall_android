package com.acutis.firewall.admin

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.UserManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class UninstallProtectionManagerTest {

    private lateinit var context: Context
    private lateinit var dpm: DevicePolicyManager
    private lateinit var manager: UninstallProtectionManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.packageName } returns "com.acutis.firewall"
        dpm = mockk(relaxed = true)
        manager = UninstallProtectionManager(context, dpm)
    }

    @Test
    fun `isProtectionActive delegates to isAdminActive`() {
        every { dpm.isAdminActive(any()) } returns true

        assertThat(manager.isProtectionActive()).isTrue()
    }

    @Test
    fun `isProtectionActive returns false when the platform throws`() {
        // Some OEMs and managed-profile configurations throw here; protection must
        // report itself unavailable rather than taking the app down.
        every { dpm.isAdminActive(any()) } throws SecurityException("nope")

        assertThat(manager.isProtectionActive()).isFalse()
    }

    @Test
    fun `disableProtection removes the active admin`() {
        every { dpm.isDeviceOwnerApp(any()) } returns false

        assertThat(manager.disableProtection()).isTrue()

        verify { dpm.removeActiveAdmin(any()) }
    }

    @Test
    fun `disableProtection reports failure instead of throwing`() {
        every { dpm.isDeviceOwnerApp(any()) } returns false
        every { dpm.removeActiveAdmin(any()) } throws SecurityException("refused")

        // The caller shows "remove it from Settings" rather than leaving the parent
        // with an active admin and a crashed app.
        assertThat(manager.disableProtection()).isFalse()
    }

    @Test
    fun `disableProtection on a device owner clears ownership rather than the admin`() {
        every { dpm.isDeviceOwnerApp(any()) } returns true

        assertThat(manager.disableProtection()).isTrue()

        verify { dpm.setUninstallBlocked(any(), "com.acutis.firewall", false) }
        verify { dpm.clearDeviceOwnerApp("com.acutis.firewall") }
        // removeActiveAdmin always fails for a device owner.
        verify(exactly = 0) { dpm.removeActiveAdmin(any()) }
    }

    @Test
    fun `device owner hardening blocks uninstall and safe boot`() {
        every { dpm.isDeviceOwnerApp(any()) } returns true

        manager.applyDeviceOwnerHardening()

        verify { dpm.setUninstallBlocked(any(), "com.acutis.firewall", true) }
        verify { dpm.addUserRestriction(any(), UserManager.DISALLOW_SAFE_BOOT) }
        verify { dpm.addUserRestriction(any(), UserManager.DISALLOW_FACTORY_RESET) }
    }

    @Test
    fun `device owner hardening does nothing when not device owner`() {
        every { dpm.isDeviceOwnerApp(any()) } returns false

        manager.applyDeviceOwnerHardening()

        verify(exactly = 0) { dpm.setUninstallBlocked(any(), any(), any()) }
    }

    @Test
    fun `blocking all uninstalls is opt-in`() {
        every { dpm.isDeviceOwnerApp(any()) } returns true

        manager.applyDeviceOwnerHardening(blockAllUninstalls = false)

        // DISALLOW_UNINSTALL_APPS blocks uninstalling *every* app on the device, so
        // it must never be applied as a side effect of enabling protection.
        verify(exactly = 0) {
            dpm.addUserRestriction(any(), UserManager.DISALLOW_UNINSTALL_APPS)
        }
    }
}
