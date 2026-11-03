package com.acutis.firewall.service

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

private val IPV4_LITERAL = Regex("""^[0-9]{1,3}(\.[0-9]{1,3}){3}$""")

/**
 * Parses a user-supplied upstream DNS server. Only numeric IPv4 and IPv6 literals
 * are accepted: anything that could be a hostname (including `host:port`) is
 * rejected before resolution so a typo cannot turn into a network lookup.
 *
 * Shared by the settings screen and the VPN service so their validation cannot drift.
 */
internal fun parseDnsServerAddress(value: String): InetAddress? {
    val candidate = value.trim()
    if (candidate.isEmpty()) return null

    val isIpv4Candidate = IPV4_LITERAL.matches(candidate)
    val isIpv6Candidate = candidate.contains(':')
    if (!isIpv4Candidate && !isIpv6Candidate) return null

    val address = try {
        InetAddress.getByName(candidate)
    } catch (e: Exception) {
        return null
    }

    return when {
        isIpv4Candidate && address is Inet4Address -> address
        isIpv6Candidate && address is Inet6Address -> address
        else -> null
    }
}
