package com.acutis.firewall.data.repository

import android.content.Context
import android.content.Intent
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.CustomBlocklistDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.service.FirewallVpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomBlocklistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customBlocklistDao: CustomBlocklistDao,
    private val blockedSiteDao: BlockedSiteDao
) {
    fun getAllLists(): Flow<List<CustomBlocklist>> = customBlocklistDao.getAllLists()

    fun getEnabledLists(): Flow<List<CustomBlocklist>> = customBlocklistDao.getEnabledLists()

    fun getListById(id: Long): Flow<CustomBlocklist?> = customBlocklistDao.getListByIdFlow(id)

    fun getListCount(): Flow<Int> = customBlocklistDao.getListCount()

    fun getSitesInList(listId: Long): Flow<List<BlockedSite>> = blockedSiteDao.getSitesByListId(listId)

    fun getSiteCountInList(listId: Long): Flow<Int> = blockedSiteDao.getCountByListId(listId)

    suspend fun createList(name: String, description: String = ""): Long {
        val list = CustomBlocklist(
            name = name,
            description = description,
            isEnabled = true
        )
        return customBlocklistDao.insert(list)
    }

    suspend fun updateList(list: CustomBlocklist) {
        customBlocklistDao.update(list)
    }

    suspend fun deleteList(listId: Long) {
        // Delete all sites in the list first
        blockedSiteDao.deleteByListId(listId)
        // Then delete the list
        customBlocklistDao.deleteById(listId)
        notifyBlocklistChanged()
    }

    suspend fun toggleListEnabled(listId: Long, enabled: Boolean) {
        customBlocklistDao.setEnabled(listId, enabled)
        // Also toggle all sites in the list
        blockedSiteDao.setListSitesEnabled(listId, enabled)
        notifyBlocklistChanged()
    }

    suspend fun addDomainToList(listId: Long, domain: String): Boolean {
        val normalizedDomain = normalizeDomain(domain)
        android.util.Log.d("CustomBlocklistRepo", "Adding domain: $normalizedDomain to list $listId")

        // Check if domain already exists in this list
        val existing = blockedSiteDao.getSiteByDomain(normalizedDomain)
        if (existing != null && existing.customListId == listId) {
            android.util.Log.d("CustomBlocklistRepo", "Domain already exists in this list")
            return false
        }

        val site = BlockedSite(
            domain = normalizedDomain,
            category = BlockCategory.CUSTOM,
            isEnabled = true,
            isCustom = true,
            customListId = listId
        )
        val id = blockedSiteDao.insert(site)
        android.util.Log.d("CustomBlocklistRepo", "Inserted site with id=$id, domain=$normalizedDomain")
        notifyBlocklistChanged()
        return true
    }

    suspend fun removeDomainFromList(site: BlockedSite) {
        blockedSiteDao.delete(site)
        notifyBlocklistChanged()
    }

    suspend fun toggleDomainEnabled(site: BlockedSite) {
        blockedSiteDao.update(site.copy(isEnabled = !site.isEnabled))
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
            android.util.Log.d("CustomBlocklistRepo", "Sent blocklist refresh intent")
        } catch (e: Exception) {
            android.util.Log.w("CustomBlocklistRepo", "Failed to send refresh intent", e)
        }
    }
}
