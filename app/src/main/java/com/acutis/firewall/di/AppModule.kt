package com.acutis.firewall.di

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.acutis.firewall.blocklist.DefaultHttpFetcher
import com.acutis.firewall.blocklist.HttpFetcher
import com.acutis.firewall.data.db.AppDatabase
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.CustomBlocklistDao
import com.acutis.firewall.data.db.TimeRuleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpFetcher(impl: DefaultHttpFetcher): HttpFetcher = impl

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "acutis_firewall_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
    }

    @Provides
    @Singleton
    fun provideBlockedSiteDao(database: AppDatabase): BlockedSiteDao {
        return database.blockedSiteDao()
    }

    @Provides
    @Singleton
    fun provideTimeRuleDao(database: AppDatabase): TimeRuleDao {
        return database.timeRuleDao()
    }

    @Provides
    @Singleton
    fun provideCustomBlocklistDao(database: AppDatabase): CustomBlocklistDao {
        return database.customBlocklistDao()
    }

    @Provides
    @Singleton
    fun provideDevicePolicyManager(
        @ApplicationContext context: Context
    ): DevicePolicyManager {
        return context.getSystemService(DevicePolicyManager::class.java)
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
