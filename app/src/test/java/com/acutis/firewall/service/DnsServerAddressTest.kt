package com.acutis.firewall.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.Inet4Address
import java.net.Inet6Address

class DnsServerAddressTest {

    @Test
    fun `accepts an IPv4 literal`() {
        assertThat(parseDnsServerAddress("9.9.9.9")).isInstanceOf(Inet4Address::class.java)
    }

    @Test
    fun `accepts an IPv6 literal`() {
        assertThat(parseDnsServerAddress("2620:fe::fe")).isInstanceOf(Inet6Address::class.java)
    }

    @Test
    fun `accepts surrounding whitespace`() {
        val address = parseDnsServerAddress(" 9.9.9.9 ")

        assertThat(address).isInstanceOf(Inet4Address::class.java)
        assertThat(address?.hostAddress).isEqualTo("9.9.9.9")
    }

    @Test
    fun `rejects empty and blank input`() {
        assertThat(parseDnsServerAddress("")).isNull()
        assertThat(parseDnsServerAddress("   ")).isNull()
    }

    @Test
    fun `rejects an out of range IPv4 address`() {
        assertThat(parseDnsServerAddress("999.1.1.1")).isNull()
    }

    @Test
    fun `rejects an incomplete IPv4 address`() {
        assertThat(parseDnsServerAddress("9.9.9")).isNull()
    }

    @Test
    fun `rejects a malformed IPv6 address`() {
        assertThat(parseDnsServerAddress("::gg")).isNull()
    }

    @Test
    fun `rejects an address with a port`() {
        assertThat(parseDnsServerAddress("9.9.9.9:53")).isNull()
    }

    @Test
    fun `rejects a hostname`() {
        assertThat(parseDnsServerAddress("dns.example.com")).isNull()
    }
}
