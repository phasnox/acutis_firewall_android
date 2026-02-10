package com.acutis.firewall.data.db

import androidx.room.*
import com.acutis.firewall.data.db.entities.CustomBlocklist
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBlocklistDao {
    @Query("SELECT * FROM custom_blocklists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<CustomBlocklist>>

    @Query("SELECT * FROM custom_blocklists WHERE isEnabled = 1")
    fun getEnabledLists(): Flow<List<CustomBlocklist>>

    @Query("SELECT * FROM custom_blocklists WHERE id = :id")
    suspend fun getListById(id: Long): CustomBlocklist?

    @Query("SELECT * FROM custom_blocklists WHERE id = :id")
    fun getListByIdFlow(id: Long): Flow<CustomBlocklist?>

    @Query("SELECT COUNT(*) FROM custom_blocklists")
    fun getListCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: CustomBlocklist): Long

    @Update
    suspend fun update(list: CustomBlocklist)

    @Delete
    suspend fun delete(list: CustomBlocklist)

    @Query("DELETE FROM custom_blocklists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE custom_blocklists SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
