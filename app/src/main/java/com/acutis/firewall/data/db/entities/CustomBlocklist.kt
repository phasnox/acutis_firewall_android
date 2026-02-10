package com.acutis.firewall.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_blocklists")
data class CustomBlocklist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
