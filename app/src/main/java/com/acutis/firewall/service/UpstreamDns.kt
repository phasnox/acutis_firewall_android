package com.acutis.firewall.service

import java.net.Inet4Address
import java.net.InetAddress

/**
 * Chooses the upstream DNS servers that allowed queries are forwarded to.
 *
 * The device's DNS list is not ordered by reachability. A dual-stack network
 * commonly advertises an IPv6 server (site-local `fec0::/10`, link-local
 * `fe80::/10`) that the device cannot route, and older Android builds put it
 * first. Taking the first entry — which this service used to do — forwarded every
 * query to an address that never answered, so nothing resolved while the UI still
 * said "Firewall is enabled".
 *
 * The list is therefore treated as ordered candidates: IPv4 first because it works
 * on nearly every network, IPv6 kept behind it for IPv6-only networks, and
 * [promote] moves whichever address actually answered to the front.
 */
internal object UpstreamDns {

    /** Addresses the tunnel intercepts; forwarding to one would send the query back into us. */
    internal val INTERCEPTED_HOSTS = setOf("8.8.8.8", "8.8.4.4")

    /**
     * @param systemServers the active network's resolvers, captured before the VPN
     *   is established so the list cannot become the tunnel's own address.
     * @param override the user's servers setting; when present it is the only
     *   candidate, because the user asked for that resolver specifically.
     */
    fun candidates(
        systemServers: List<InetAddress>,
        override: InetAddress?,
        fallback: InetAddress,
    ): List<InetAddress> {
        if (override != null) return listOf(override)

        val usable = systemServers.filter { candidate ->
            !candidate.isAnyLocalAddress &&
                !candidate.isLoopbackAddress &&
                candidate.hostAddress !in INTERCEPTED_HOSTS
        }.distinct()

        // IPv4 before IPv6, each group keeping the network's own order.
        val ordered = usable.filterIsInstance<Inet4Address>() + usable.filterNot { it is Inet4Address }
        return ordered.ifEmpty { listOf(fallback) }
    }

    /**
     * Combines a fresh resolver list with the one already in use. Network callbacks
     * can fire while the platform momentarily reports no resolvers (boot, network
     * switch); replacing a working list with the public fallback then strands DNS
     * on a resolver the network may not route. New system servers go first, the
     * previous ones stay behind them as a safety net, and the fallback is dropped
     * as soon as any real server is known. An explicit user override always wins.
     */
    fun merge(
        fresh: List<InetAddress>,
        current: List<InetAddress>,
        override: InetAddress?,
        fallback: InetAddress,
    ): List<InetAddress> {
        if (override != null) return listOf(override)
        val real = fresh.filter { it != fallback }
        if (real.isEmpty()) return current.ifEmpty { listOf(fallback) }
        return (real + current.filter { it != fallback && it !in real }).distinct()
    }

    /** Moves [winner] to the front so later queries start with the address that works. */
    fun promote(order: List<InetAddress>, winner: InetAddress): List<InetAddress> =
        buildList(order.size.coerceAtLeast(1)) {
            add(winner)
            order.filterTo(this) { it != winner }
        }
}
