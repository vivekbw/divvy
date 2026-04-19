package com.favalabs.divvy.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.favalabs.divvy.offline.db.DivvyDatabase
import com.favalabs.divvy.offline.db.dao.CachedActivityDao
import com.favalabs.divvy.offline.db.dao.CachedBalanceDao
import com.favalabs.divvy.offline.db.dao.CachedExpenseDao
import com.favalabs.divvy.offline.db.dao.CachedFriendBalanceDao
import com.favalabs.divvy.offline.db.dao.CachedExpenseSplitDao
import com.favalabs.divvy.offline.db.dao.CachedGroupDao
import com.favalabs.divvy.offline.db.dao.CachedMemberDao
import com.favalabs.divvy.offline.db.dao.PendingOperationDao
import com.favalabs.divvy.security.SQLCipherPassphraseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfflineModule {

    private const val OLD_DB_NAME = "divvy_cache.db"
    private const val ENCRYPTED_DB_NAME = "divvy_cache_encrypted.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DivvyDatabase {
        System.loadLibrary("sqlcipher")

        // Remove the pre-encryption plaintext cache so no unencrypted data lingers on disk.
        // This is a no-op once the old file is gone.
        context.deleteDatabase(OLD_DB_NAME)

        val passphrase = SQLCipherPassphraseProvider.getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)
        @Suppress("DEPRECATION")
        return Room.databaseBuilder(
            context,
            DivvyDatabase::class.java,
            ENCRYPTED_DB_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideCachedGroupDao(db: DivvyDatabase): CachedGroupDao = db.cachedGroupDao()
    @Provides fun provideCachedExpenseDao(db: DivvyDatabase): CachedExpenseDao = db.cachedExpenseDao()
    @Provides fun provideCachedExpenseSplitDao(db: DivvyDatabase): CachedExpenseSplitDao = db.cachedExpenseSplitDao()
    @Provides fun provideCachedMemberDao(db: DivvyDatabase): CachedMemberDao = db.cachedMemberDao()
    @Provides fun provideCachedBalanceDao(db: DivvyDatabase): CachedBalanceDao = db.cachedBalanceDao()
    @Provides fun provideCachedActivityDao(db: DivvyDatabase): CachedActivityDao = db.cachedActivityDao()
    @Provides fun provideCachedFriendBalanceDao(db: DivvyDatabase): CachedFriendBalanceDao = db.cachedFriendBalanceDao()
    @Provides fun providePendingOperationDao(db: DivvyDatabase): PendingOperationDao = db.pendingOperationDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
