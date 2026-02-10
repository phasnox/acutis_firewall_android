package com.acutis.firewall.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_rules")
data class TimeRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String?,
    val category: BlockCategory?,
    val customListId: Long? = null,  // Reference to a custom blocklist
    val action: TimeRuleAction,
    val dailyLimitMinutes: Int?,
    val startHour: Int?,
    val startMinute: Int?,
    val endHour: Int?,
    val endMinute: Int?,
    val daysOfWeek: String = "1,2,3,4,5,6,7",
    val isEnabled: Boolean = true,
    val usedMinutesToday: Int = 0,
    val lastResetDate: String? = null,
    val lastQueryTimestamp: Long? = null
)

enum class TimeRuleAction {
    ALLOW,
    BLOCK
}
