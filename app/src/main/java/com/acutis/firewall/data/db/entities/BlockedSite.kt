package com.acutis.firewall.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_sites")
data class BlockedSite(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val category: BlockCategory,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val customListId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BlockCategory {
    ADULT,
    MALWARE,
    GAMBLING,
    SOCIAL_MEDIA,
    CUSTOM
}
