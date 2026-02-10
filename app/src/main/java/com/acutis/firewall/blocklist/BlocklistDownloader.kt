package com.acutis.firewall.blocklist

import android.content.Context
import android.content.Intent
import android.util.Log
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.service.FirewallVpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedSiteDao: BlockedSiteDao
) {
    companion object {
        private const val TAG = "BlocklistDownloader"

        // Adult content blocklists
        val ADULT_LISTS = listOf(
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn-only/hosts",
            "https://raw.githubusercontent.com/4skinSkywalker/Anti-Porn-HOSTS-File/master/HOSTS.txt"
        )

        // Malware/phishing blocklists
        val MALWARE_LISTS = listOf(
            "https://urlhaus.abuse.ch/downloads/hostfile/",
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/data/StevenBlack/hosts"
        )

        // Gambling blocklists
        val GAMBLING_LISTS = listOf(
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/gambling-only/hosts"
        )

        // Social media blocklists
        val SOCIAL_MEDIA_LISTS = listOf(
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/social-only/hosts"
        )
    }

    data class DownloadResult(
        val success: Boolean,
        val domainsAdded: Int,
        val error: String? = null
    )

    suspend fun downloadAndSaveBlocklist(
        category: BlockCategory,
        urls: List<String> = getUrlsForCategory(category)
    ): DownloadResult = withContext(Dispatchers.IO) {
        var totalAdded = 0

        try {
            // Clear existing non-custom entries for this category
            blockedSiteDao.deleteByCategory(category)

            val allDomains = mutableSetOf<String>()

            for (url in urls) {
                try {
                    Log.d(TAG, "Downloading from: $url")
                    val domains = downloadHostsFile(url)
                    Log.d(TAG, "Got ${domains.size} domains from $url")
                    allDomains.addAll(domains)
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading $url", e)
                }
            }

            // Convert to BlockedSite entities and save in batches
            val sites = allDomains.map { domain ->
                BlockedSite(
                    domain = domain,
                    category = category,
                    isEnabled = true,
                    isCustom = false
                )
            }

            // Save in batches of 1000 to avoid memory issues
            sites.chunked(1000).forEach { batch ->
                blockedSiteDao.insertAll(batch)
                totalAdded += batch.size
            }

            Log.d(TAG, "Saved $totalAdded domains for category $category")
            notifyBlocklistChanged()
            DownloadResult(success = true, domainsAdded = totalAdded)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving blocklist", e)
            DownloadResult(success = false, domainsAdded = totalAdded, error = e.message)
        }
    }

    private fun downloadHostsFile(urlString: String): Set<String> {
        val domains = mutableSetOf<String>()
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        parseDomainFromLine(line)?.let { domain ->
                            domains.add(domain)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        return domains
    }

    private fun parseDomainFromLine(line: String): String? {
        val trimmed = line.trim()

        // Skip comments and empty lines
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return null
        }

        // Handle hosts file format: "0.0.0.0 domain.com" or "127.0.0.1 domain.com"
        if (trimmed.startsWith("0.0.0.0") || trimmed.startsWith("127.0.0.1")) {
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size >= 2) {
                val domain = parts[1].lowercase().trim()
                if (isValidDomain(domain)) {
                    return domain
                }
            }
            return null
        }

        // Handle plain domain format
        val domain = trimmed.split(Regex("\\s+")).firstOrNull()?.lowercase()?.trim()
        if (domain != null && isValidDomain(domain)) {
            return domain
        }

        return null
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.isEmpty()) return false
        if (domain == "localhost") return false
        if (domain == "local") return false
        if (domain.startsWith("0.0.0.0")) return false
        if (domain.startsWith("127.")) return false
        if (domain.startsWith("#")) return false
        if (!domain.contains(".")) return false
        // Basic domain validation
        return domain.matches(Regex("^[a-z0-9][a-z0-9.-]*[a-z0-9]$"))
    }

    private fun getUrlsForCategory(category: BlockCategory): List<String> {
        return when (category) {
            BlockCategory.ADULT -> ADULT_LISTS
            BlockCategory.MALWARE -> MALWARE_LISTS
            BlockCategory.GAMBLING -> GAMBLING_LISTS
            BlockCategory.SOCIAL_MEDIA -> SOCIAL_MEDIA_LISTS
            BlockCategory.CUSTOM -> emptyList()
        }
    }

    suspend fun downloadAllCategories(
        includeAdult: Boolean = true,
        includeMalware: Boolean = true,
        includeGambling: Boolean = false,
        includeSocialMedia: Boolean = false
    ): Map<BlockCategory, DownloadResult> {
        val results = mutableMapOf<BlockCategory, DownloadResult>()

        if (includeAdult) {
            results[BlockCategory.ADULT] = downloadAndSaveBlocklist(BlockCategory.ADULT)
        }
        if (includeMalware) {
            results[BlockCategory.MALWARE] = downloadAndSaveBlocklist(BlockCategory.MALWARE)
        }
        if (includeGambling) {
            results[BlockCategory.GAMBLING] = downloadAndSaveBlocklist(BlockCategory.GAMBLING)
        }
        if (includeSocialMedia) {
            results[BlockCategory.SOCIAL_MEDIA] = downloadAndSaveBlocklist(BlockCategory.SOCIAL_MEDIA)
        }

        notifyBlocklistChanged()
        return results
    }

    private fun notifyBlocklistChanged() {
        val intent = Intent(context, FirewallVpnService::class.java).apply {
            action = FirewallVpnService.ACTION_REFRESH_BLOCKLIST
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // Service might not be running, that's OK
        }
    }
}
