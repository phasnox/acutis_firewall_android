package com.acutis.firewall.data.db

import androidx.room.*
import com.acutis.firewall.data.db.entities.TimeRule
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeRuleDao {
    @Query("SELECT * FROM time_rules ORDER BY id ASC")
    fun getAllRules(): Flow<List<TimeRule>>

    @Query("SELECT * FROM time_rules WHERE isEnabled = 1")
    fun getEnabledRules(): Flow<List<TimeRule>>

    @Query("SELECT * FROM time_rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesList(): List<TimeRule>

    @Query("SELECT * FROM time_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): TimeRule?

    @Query("SELECT * FROM time_rules WHERE domain = :domain AND isEnabled = 1")
    suspend fun getRulesForDomain(domain: String): List<TimeRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: TimeRule): Long

    @Update
    suspend fun update(rule: TimeRule)

    @Delete
    suspend fun delete(rule: TimeRule)

    @Query("DELETE FROM time_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE time_rules SET usedMinutesToday = :minutes WHERE id = :id")
    suspend fun updateUsedMinutes(id: Long, minutes: Int)

    @Query("UPDATE time_rules SET usedMinutesToday = 0, lastResetDate = :date, lastQueryTimestamp = NULL")
    suspend fun resetDailyUsage(date: String)

    @Query("UPDATE time_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE time_rules SET lastQueryTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastQueryTimestamp(id: Long, timestamp: Long)

    @Query("UPDATE time_rules SET usedMinutesToday = :minutes, lastQueryTimestamp = :timestamp WHERE id = :id")
    suspend fun updateUsageAndTimestamp(id: Long, minutes: Int, timestamp: Long)
}
