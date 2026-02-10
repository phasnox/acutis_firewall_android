package com.acutis.firewall.data.db

import androidx.room.*
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedSiteDao {
    @Query("SELECT * FROM blocked_sites ORDER BY domain ASC")
    fun getAllSites(): Flow<List<BlockedSite>>

    @Query("SELECT * FROM blocked_sites WHERE isEnabled = 1")
    fun getEnabledSites(): Flow<List<BlockedSite>>

    @Query("SELECT * FROM blocked_sites WHERE isEnabled = 1")
    suspend fun getEnabledSitesList(): List<BlockedSite>

    @Query("SELECT * FROM blocked_sites WHERE category = :category")
    fun getSitesByCategory(category: BlockCategory): Flow<List<BlockedSite>>

    @Query("SELECT * FROM blocked_sites WHERE isCustom = 1")
    fun getCustomSites(): Flow<List<BlockedSite>>

    @Query("SELECT * FROM blocked_sites WHERE domain = :domain LIMIT 1")
    suspend fun getSiteByDomain(domain: String): BlockedSite?

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE isEnabled = 1")
    fun getEnabledCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE category = :category AND isEnabled = 1")
    fun getEnabledCountByCategory(category: BlockCategory): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: BlockedSite): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sites: List<BlockedSite>)

    @Update
    suspend fun update(site: BlockedSite)

    @Delete
    suspend fun delete(site: BlockedSite)

    @Query("DELETE FROM blocked_sites WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("DELETE FROM blocked_sites WHERE category = :category AND isCustom = 0")
    suspend fun deleteByCategory(category: BlockCategory)

    @Query("UPDATE blocked_sites SET isEnabled = :enabled WHERE category = :category")
    suspend fun setCategoryEnabled(category: BlockCategory, enabled: Boolean)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_sites WHERE domain = :domain AND isEnabled = 1 LIMIT 1)")
    suspend fun isDomainBlocked(domain: String): Boolean

    @Query("SELECT * FROM blocked_sites WHERE customListId = :listId ORDER BY domain ASC")
    fun getSitesByListId(listId: Long): Flow<List<BlockedSite>>

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE customListId = :listId")
    fun getCountByListId(listId: Long): Flow<Int>

    @Query("DELETE FROM blocked_sites WHERE customListId = :listId")
    suspend fun deleteByListId(listId: Long)

    @Query("UPDATE blocked_sites SET isEnabled = :enabled WHERE customListId = :listId")
    suspend fun setListSitesEnabled(listId: Long, enabled: Boolean)
}
