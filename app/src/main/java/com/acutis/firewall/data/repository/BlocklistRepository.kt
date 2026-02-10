package com.acutis.firewall.data.repository

import android.content.Context
import android.content.Intent
import com.acutis.firewall.blocklist.DefaultBlocklists
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.service.FirewallVpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedSiteDao: BlockedSiteDao
) {
    fun getAllSites(): Flow<List<BlockedSite>> = blockedSiteDao.getAllSites()

    fun getEnabledSites(): Flow<List<BlockedSite>> = blockedSiteDao.getEnabledSites()

    fun getSitesByCategory(category: BlockCategory): Flow<List<BlockedSite>> =
        blockedSiteDao.getSitesByCategory(category)

    fun getCustomSites(): Flow<List<BlockedSite>> = blockedSiteDao.getCustomSites()

    fun getEnabledCount(): Flow<Int> = blockedSiteDao.getEnabledCount()

    fun getEnabledCountByCategory(category: BlockCategory): Flow<Int> =
        blockedSiteDao.getEnabledCountByCategory(category)

    suspend fun addCustomSite(domain: String): Boolean {
        val normalizedDomain = normalizeDomain(domain)
        val existing = blockedSiteDao.getSiteByDomain(normalizedDomain)
        if (existing != null) return false

        val site = BlockedSite(
            domain = normalizedDomain,
            category = BlockCategory.CUSTOM,
            isEnabled = true,
            isCustom = true
        )
        blockedSiteDao.insert(site)
        notifyBlocklistChanged()
        return true
    }

    suspend fun removeSite(site: BlockedSite) {
        blockedSiteDao.delete(site)
        notifyBlocklistChanged()
    }

    suspend fun removeSiteByDomain(domain: String) {
        blockedSiteDao.deleteByDomain(normalizeDomain(domain))
        notifyBlocklistChanged()
    }

    suspend fun toggleSite(site: BlockedSite) {
        blockedSiteDao.update(site.copy(isEnabled = !site.isEnabled))
        notifyBlocklistChanged()
    }

    suspend fun setCategoryEnabled(category: BlockCategory, enabled: Boolean) {
        blockedSiteDao.setCategoryEnabled(category, enabled)
        notifyBlocklistChanged()
    }

    suspend fun isDomainBlocked(domain: String): Boolean {
        return blockedSiteDao.isDomainBlocked(normalizeDomain(domain))
    }

    suspend fun initializeDefaultBlocklists() {
        val existingSites = blockedSiteDao.getEnabledSitesList()
        if (existingSites.isEmpty()) {
            val defaults = DefaultBlocklists.getAllDefaultDomains()
            blockedSiteDao.insertAll(defaults)
            notifyBlocklistChanged()
        }
    }

    suspend fun resetCategory(category: BlockCategory) {
        blockedSiteDao.deleteByCategory(category)
        val defaults = when (category) {
            BlockCategory.ADULT -> DefaultBlocklists.getAdultContentDomains()
            BlockCategory.MALWARE -> DefaultBlocklists.getMalwareDomains()
            BlockCategory.GAMBLING -> DefaultBlocklists.getGamblingDomains()
            BlockCategory.SOCIAL_MEDIA -> DefaultBlocklists.getSocialMediaDomains()
            BlockCategory.CUSTOM -> emptyList()
        }
        if (defaults.isNotEmpty()) {
            blockedSiteDao.insertAll(defaults)
        }
        notifyBlocklistChanged()
    }

    private fun normalizeDomain(domain: String): String {
        return domain.lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .trimEnd('/')
            .split('/').first()
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
