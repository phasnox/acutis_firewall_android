package com.acutis.firewall.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.InetAddress

class UpstreamDnsTest {

    private val fallback = InetAddress.getByName("1.1.1.1")

    @Test
    fun `the user override is the only candidate`() {
        val override = InetAddress.getByName("9.9.9.9")

        val candidates = UpstreamDns.candidates(
            systemServers = listOf(InetAddress.getByName("10.0.2.3")),
            override = override,
            fallback = fallback,
        )

        assertThat(candidates).containsExactly(override)
    }

    /**
     * The bug this guards: an emulator (and many dual-stack networks) advertises an
     * unroutable site-local IPv6 resolver first. Taking it verbatim meant every query
     * timed out and nothing resolved.
     */
    @Test
    fun `a routable IPv4 resolver is preferred over an IPv6 one listed first`() {
        val ipv6 = InetAddress.getByName("fec0::3")
        val ipv4 = InetAddress.getByName("10.0.2.3")

        val candidates = UpstreamDns.candidates(
            systemServers = listOf(ipv6, ipv4),
            override = null,
            fallback = fallback,
        )

        assertThat(candidates).containsExactly(ipv4, ipv6).inOrder()
    }

    @Test
    fun `intercepted and loopback addresses are never upstreams`() {
        val candidates = UpstreamDns.candidates(
            systemServers = listOf(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4"),
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("192.168.1.1"),
            ),
            override = null,
            fallback = fallback,
        )

        assertThat(candidates).containsExactly(InetAddress.getByName("192.168.1.1"))
    }

    @Test
    fun `no usable system server falls back to the public resolver`() {
        val candidates = UpstreamDns.candidates(
            systemServers = listOf(InetAddress.getByName("8.8.8.8")),
            override = null,
            fallback = fallback,
        )

        assertThat(candidates).containsExactly(fallback)
    }

    @Test
    fun `promotion moves the answering server to the front and keeps the rest`() {
        val dead = InetAddress.getByName("fec0::3")
        val working = InetAddress.getByName("10.0.2.3")

        val promoted = UpstreamDns.promote(listOf(dead, working), working)

        assertThat(promoted).containsExactly(working, dead).inOrder()
    }
}
