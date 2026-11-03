package com.acutis.firewall.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BootReceiverTest {

    @Test
    fun `a package replacement restores the vpn that was running`() {
        // An update kills the process and with it the VPN, and START_STICKY cannot
        // bring it back. Users saw "Firewall is enabled" while nothing was filtered.
        assertThat(
            shouldRestoreVpn(
                afterPackageReplace = true,
                autoStartEnabled = false,
                firewallWasEnabled = true
            )
        ).isTrue()
    }

    @Test
    fun `a package replacement does not turn protection on by itself`() {
        assertThat(
            shouldRestoreVpn(
                afterPackageReplace = true,
                autoStartEnabled = true,
                firewallWasEnabled = false
            )
        ).isFalse()
    }

    @Test
    fun `boot starts the vpn only when auto-start is on`() {
        assertThat(
            shouldRestoreVpn(
                afterPackageReplace = false,
                autoStartEnabled = true,
                firewallWasEnabled = true
            )
        ).isTrue()
        assertThat(
            shouldRestoreVpn(
                afterPackageReplace = false,
                autoStartEnabled = false,
                firewallWasEnabled = true
            )
        ).isFalse()
    }

    @Test
    fun `boot never starts protection the user had turned off`() {
        assertThat(
            shouldRestoreVpn(
                afterPackageReplace = false,
                autoStartEnabled = true,
                firewallWasEnabled = false
            )
        ).isFalse()
    }
}
