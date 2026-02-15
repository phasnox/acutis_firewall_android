package com.acutis.firewall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.acutis.firewall.MainActivity
import com.acutis.firewall.R
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.TimeRuleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class FirewallVpnService : VpnService() {

    @Inject
    lateinit var blockedSiteDao: BlockedSiteDao

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var timeRuleRepository: TimeRuleRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var blockedDomains: Set<String> = emptySet()

    companion object {
        private const val TAG = "FirewallVPN"
        const val ACTION_START = "com.acutis.firewall.START_VPN"
        const val ACTION_STOP = "com.acutis.firewall.STOP_VPN"
        const val ACTION_REFRESH_BLOCKLIST = "com.acutis.firewall.REFRESH_BLOCKLIST"
        private const val NOTIFICATION_ID = 1
        private const val LOCKDOWN_WARNING_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "firewall_channel"
        private const val WARNING_CHANNEL_ID = "firewall_warning_channel"
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val MTU = 1500
        private const val DNS_PORT = 53

        // Use Google DNS as our "fake" DNS that we intercept
        private const val INTERCEPT_DNS = "8.8.8.8"
        // Use Cloudflare DNS as the actual upstream
        private val UPSTREAM_DNS = InetAddress.getByName("1.1.1.1")
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createWarningNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            ACTION_REFRESH_BLOCKLIST -> refreshBlocklist()
            null -> {
                // Service was restarted by the system, check if we should be running
                serviceScope.launch {
                    if (settingsDataStore.isFirewallEnabled() && !isRunning) {
                        Log.d(TAG, "Service restarted by system, restarting VPN")
                        startVpn()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        serviceScope.launch {
            try {
                blockedDomains = loadBlockedDomains()
                Log.d(TAG, "Loaded ${blockedDomains.size} blocked domains")
                // Log first 10 domains for debugging
                blockedDomains.take(10).forEach { Log.d(TAG, "  Blocked: $it") }

                val builder = Builder()
                    .setSession("Acutis Firewall")
                    .setMtu(MTU)
                    .addAddress(VPN_ADDRESS, 24)
                    // Set DNS server that apps will use - we'll intercept queries to this
                    .addDnsServer(INTERCEPT_DNS)
                    // Route only DNS server IPs through VPN
                    .addRoute(INTERCEPT_DNS, 32)
                    .addRoute("8.8.4.4", 32)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                vpnInterface = builder.establish()

                if (vpnInterface != null) {
                    isRunning = true
                    startForeground(NOTIFICATION_ID, createNotification())
                    settingsDataStore.setFirewallEnabled(true)
                    Log.d(TAG, "VPN established successfully")

                    // Check if lockdown mode is enabled and warn user
                    checkLockdownMode()

                    runVpnLoop()
                } else {
                    Log.e(TAG, "Failed to establish VPN")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting VPN", e)
                stopSelf()
            }
        }
    }

    private suspend fun loadBlockedDomains(): Set<String> {
        return blockedSiteDao.getEnabledSitesList()
            .map { it.domain.lowercase().trimEnd('.') }
            .toSet()
    }

    private fun runVpnLoop() {
        serviceScope.launch {
            val fd = vpnInterface?.fileDescriptor ?: return@launch
            val inputStream = FileInputStream(fd)
            val outputStream = FileOutputStream(fd)
            val buffer = ByteArray(MTU)

            Log.d(TAG, "Starting VPN packet loop")

            while (isRunning && vpnInterface != null) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        val packetData = buffer.copyOf(length)
                        handlePacket(packetData, outputStream)
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "Error in VPN loop", e)
                    }
                    break
                }
            }
            Log.d(TAG, "VPN loop ended")
        }
    }

    private fun handlePacket(packetData: ByteArray, outputStream: FileOutputStream) {
        // Need at least IP header
        if (packetData.size < 20) return

        val version = (packetData[0].toInt() and 0xF0) shr 4
        if (version != 4) return // IPv4 only

        val headerLength = (packetData[0].toInt() and 0x0F) * 4
        val protocol = packetData[9].toInt() and 0xFF

        // We only handle UDP (protocol 17)
        if (protocol != 17) return
        if (packetData.size < headerLength + 8) return

        val sourcePort = ((packetData[headerLength].toInt() and 0xFF) shl 8) or
                        (packetData[headerLength + 1].toInt() and 0xFF)
        val destPort = ((packetData[headerLength + 2].toInt() and 0xFF) shl 8) or
                      (packetData[headerLength + 3].toInt() and 0xFF)

        // Only handle DNS (port 53)
        if (destPort != DNS_PORT) return

        val udpLength = ((packetData[headerLength + 4].toInt() and 0xFF) shl 8) or
                       (packetData[headerLength + 5].toInt() and 0xFF)

        if (packetData.size < headerLength + udpLength) return

        val dnsPayload = packetData.copyOfRange(headerLength + 8, headerLength + udpLength)

        // Extract source and dest IPs for response
        val sourceIp = packetData.copyOfRange(12, 16)
        val destIp = packetData.copyOfRange(16, 20)

        // Handle DNS query async
        serviceScope.launch(Dispatchers.IO) {
            val dnsResponse = processDnsQuery(dnsPayload)
            if (dnsResponse != null) {
                val responsePacket = buildUdpResponsePacket(
                    sourceIp = destIp,
                    destIp = sourceIp,
                    sourcePort = DNS_PORT,
                    destPort = sourcePort,
                    payload = dnsResponse
                )
                writePacket(outputStream, responsePacket)
            }
        }
    }

    private suspend fun processDnsQuery(dnsPayload: ByteArray): ByteArray? {
        if (dnsPayload.size < 12) return null

        val domain = extractDomainName(dnsPayload)
        Log.d(TAG, "DNS query for: $domain")

        if (domain != null) {
            // First check static blocklist
            if (shouldBlockDomain(domain)) {
                Log.d(TAG, "BLOCKING (blocklist): $domain")
                return createBlockedResponse(dnsPayload)
            }

            // Then check time rules (this also updates usage tracking)
            if (timeRuleRepository.checkAndUpdateTimeRule(domain)) {
                Log.d(TAG, "BLOCKING (time rule): $domain")
                return createBlockedResponse(dnsPayload)
            }
        }

        // Forward to upstream DNS
        Log.d(TAG, "Forwarding: $domain")
        return forwardToUpstream(dnsPayload)
    }

    private fun extractDomainName(dnsPayload: ByteArray): String? {
        try {
            val parts = mutableListOf<String>()
            var pos = 12 // Skip DNS header

            while (pos < dnsPayload.size) {
                val len = dnsPayload[pos].toInt() and 0xFF
                if (len == 0) break

                if ((len and 0xC0) == 0xC0) {
                    // Compression pointer - follow it
                    if (pos + 1 >= dnsPayload.size) break
                    val offset = ((len and 0x3F) shl 8) or (dnsPayload[pos + 1].toInt() and 0xFF)
                    return extractDomainFromOffset(dnsPayload, offset, parts)
                }

                pos++
                if (pos + len > dnsPayload.size) break
                parts.add(String(dnsPayload, pos, len, Charsets.US_ASCII))
                pos += len
            }

            return if (parts.isNotEmpty()) parts.joinToString(".").lowercase() else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing domain", e)
            return null
        }
    }

    private fun extractDomainFromOffset(data: ByteArray, startOffset: Int, existingParts: MutableList<String>): String? {
        var pos = startOffset
        while (pos < data.size) {
            val len = data[pos].toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) break // Another pointer, stop

            pos++
            if (pos + len > data.size) break
            existingParts.add(String(data, pos, len, Charsets.US_ASCII))
            pos += len
        }
        return if (existingParts.isNotEmpty()) existingParts.joinToString(".").lowercase() else null
    }

    private fun shouldBlockDomain(domain: String): Boolean {
        val normalized = domain.lowercase().trimEnd('.')

        // Direct match
        if (blockedDomains.contains(normalized)) {
            return true
        }

        // Check parent domains and wildcards
        val parts = normalized.split(".")
        for (i in parts.indices) {
            val parent = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(parent)) {
                return true
            }
            if (blockedDomains.contains("*.$parent")) {
                return true
            }
        }

        return false
    }

    private fun createBlockedResponse(query: ByteArray): ByteArray {
        // Create NXDOMAIN response
        val response = query.copyOf()

        // Set QR=1 (response), RD=1 (recursion desired)
        response[2] = (0x81).toByte()
        // Set RA=1 (recursion available), RCODE=3 (NXDOMAIN)
        response[3] = (0x83).toByte()

        // ANCOUNT = 0
        response[6] = 0
        response[7] = 0

        // NSCOUNT = 0
        response[8] = 0
        response[9] = 0

        // ARCOUNT = 0
        response[10] = 0
        response[11] = 0

        return response
    }

    private fun forwardToUpstream(query: ByteArray): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            protect(socket) // CRITICAL: Prevent routing loop
            socket.soTimeout = 5000

            val packet = DatagramPacket(query, query.size, UPSTREAM_DNS, DNS_PORT)
            socket.send(packet)

            val responseBuffer = ByteArray(1024)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)

            responseBuffer.copyOf(responsePacket.length)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding DNS", e)
            null
        } finally {
            socket?.close()
        }
    }

    private fun buildUdpResponsePacket(
        sourceIp: ByteArray,
        destIp: ByteArray,
        sourcePort: Int,
        destPort: Int,
        payload: ByteArray
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + payload.size

        val packet = ByteBuffer.allocate(totalLen)

        // IP Header
        packet.put(0x45.toByte())           // Version 4, IHL 5
        packet.put(0x00.toByte())           // DSCP/ECN
        packet.putShort(totalLen.toShort()) // Total length
        packet.putShort(0x0000.toShort())   // ID
        packet.putShort(0x4000.toShort())   // Flags (DF)
        packet.put(64.toByte())             // TTL
        packet.put(17.toByte())             // Protocol (UDP)
        packet.putShort(0)                  // Checksum placeholder
        packet.put(sourceIp)                // Source IP
        packet.put(destIp)                  // Dest IP

        // UDP Header
        packet.putShort(sourcePort.toShort())
        packet.putShort(destPort.toShort())
        packet.putShort((udpHeaderLen + payload.size).toShort())
        packet.putShort(0) // UDP checksum optional

        // Payload
        packet.put(payload)

        val data = packet.array()

        // Calculate IP checksum
        var sum = 0
        for (i in 0 until ipHeaderLen step 2) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
        }
        while ((sum shr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        data[10] = (checksum shr 8).toByte()
        data[11] = (checksum and 0xFF).toByte()

        return data
    }

    private suspend fun writePacket(outputStream: FileOutputStream, packet: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                outputStream.write(packet)
                outputStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Error writing packet", e)
            }
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        isRunning = false
        // Use runBlocking to ensure DataStore writes complete before stopping service
        runBlocking {
            settingsDataStore.setFirewallEnabled(false)
            settingsDataStore.setLockdownModeDetected(false)
        }
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        vpnInterface?.close()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.vpn_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.vpn_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createWarningNotificationChannel() {
        Log.d(TAG, "Creating warning notification channel")
        val channel = NotificationChannel(
            WARNING_CHANNEL_ID,
            "Firewall Warnings",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important warnings about firewall configuration"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun checkLockdownMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val lockdownEnabled = isLockdownEnabled
            val alwaysOn = isAlwaysOn
            Log.d(TAG, "VPN status - isAlwaysOn: $alwaysOn, isLockdownEnabled: $lockdownEnabled")
            if (lockdownEnabled) {
                Log.w(TAG, "VPN lockdown mode is enabled - internet may not work properly")
                // Save to DataStore so UI can show a dialog
                serviceScope.launch {
                    settingsDataStore.setLockdownModeDetected(true)
                }
                showLockdownWarning()
            } else {
                // Clear the lockdown flag if not in lockdown mode
                serviceScope.launch {
                    settingsDataStore.setLockdownModeDetected(false)
                }
            }
        } else {
            Log.d(TAG, "Lockdown check skipped - requires API 29+, current: ${Build.VERSION.SDK_INT}")
        }
    }

    private fun showLockdownWarning() {
        Log.d(TAG, "Showing lockdown warning notification")
        try {
            val intent = Intent(android.provider.Settings.ACTION_VPN_SETTINGS)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
                .setContentTitle("VPN Lockdown Mode Detected")
                .setContentText("\"Block connections without VPN\" is enabled. This may cause internet issues.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("\"Block connections without VPN\" is enabled in your device settings. Acutis Firewall only filters DNS traffic, so other connections may be blocked. Tap to open VPN settings and disable this option."))
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            getSystemService(NotificationManager::class.java)
                .notify(LOCKDOWN_WARNING_NOTIFICATION_ID, notification)
            Log.d(TAG, "Lockdown warning notification sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show lockdown warning", e)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FirewallVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_shield, getString(R.string.disable_firewall), stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    fun refreshBlocklist() {
        Log.d(TAG, "Refresh blocklist requested, isRunning=$isRunning")
        if (!isRunning) {
            Log.d(TAG, "VPN not running, skipping refresh")
            return
        }
        serviceScope.launch {
            val previousSize = blockedDomains.size
            blockedDomains = loadBlockedDomains()
            Log.d(TAG, "Refreshed blocklist: $previousSize -> ${blockedDomains.size} domains")
            // Log some newly added domains for debugging
            if (blockedDomains.size > previousSize) {
                blockedDomains.take(5).forEach { Log.d(TAG, "  Sample domain: $it") }
            }
        }
    }
}
