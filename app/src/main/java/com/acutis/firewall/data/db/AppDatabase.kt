package com.acutis.firewall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.db.entities.TimeRule

@Database(
    entities = [BlockedSite::class, TimeRule::class, CustomBlocklist::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedSiteDao(): BlockedSiteDao
    abstract fun timeRuleDao(): TimeRuleDao
    abstract fun customBlocklistDao(): CustomBlocklistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE time_rules ADD COLUMN lastQueryTimestamp INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create custom_blocklists table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_blocklists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Add customListId column to blocked_sites
                database.execSQL("ALTER TABLE blocked_sites ADD COLUMN customListId INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add customListId column to time_rules
                database.execSQL("ALTER TABLE time_rules ADD COLUMN customListId INTEGER DEFAULT NULL")
            }
        }
    }
}
