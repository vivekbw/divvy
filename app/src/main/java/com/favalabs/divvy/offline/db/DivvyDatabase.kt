package com.favalabs.divvy.offline.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.favalabs.divvy.offline.db.dao.CachedActivityDao
import com.favalabs.divvy.offline.db.dao.CachedBalanceDao
import com.favalabs.divvy.offline.db.dao.CachedExpenseDao
import com.favalabs.divvy.offline.db.dao.CachedFriendBalanceDao
import com.favalabs.divvy.offline.db.dao.CachedExpenseSplitDao
import com.favalabs.divvy.offline.db.dao.CachedGroupDao
import com.favalabs.divvy.offline.db.dao.CachedMemberDao
import com.favalabs.divvy.offline.db.dao.PendingOperationDao
import com.favalabs.divvy.offline.db.entity.CachedActivityEntity
import com.favalabs.divvy.offline.db.entity.CachedBalanceEntity
import com.favalabs.divvy.offline.db.entity.CachedExpenseEntity
import com.favalabs.divvy.offline.db.entity.CachedFriendBalanceEntity
import com.favalabs.divvy.offline.db.entity.CachedExpenseSplitEntity
import com.favalabs.divvy.offline.db.entity.CachedGroupEntity
import com.favalabs.divvy.offline.db.entity.CachedMemberEntity
import com.favalabs.divvy.offline.db.entity.PendingOperationEntity

@Database(
    entities = [
        CachedGroupEntity::class,
        CachedExpenseEntity::class,
        CachedExpenseSplitEntity::class,
        CachedMemberEntity::class,
        CachedBalanceEntity::class,
        CachedActivityEntity::class,
        CachedFriendBalanceEntity::class,
        PendingOperationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DivvyDatabase : RoomDatabase() {
    abstract fun cachedGroupDao(): CachedGroupDao
    abstract fun cachedExpenseDao(): CachedExpenseDao
    abstract fun cachedExpenseSplitDao(): CachedExpenseSplitDao
    abstract fun cachedMemberDao(): CachedMemberDao
    abstract fun cachedBalanceDao(): CachedBalanceDao
    abstract fun cachedActivityDao(): CachedActivityDao
    abstract fun cachedFriendBalanceDao(): CachedFriendBalanceDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
