package com.divvy.divvy.offline.repository

import com.divvy.divvy.backend.BalanceRepository
import com.divvy.divvy.backend.SupabaseBalanceRepository
import com.divvy.divvy.models.MemberBalance
import com.divvy.divvy.offline.NetworkMonitor
import com.divvy.divvy.offline.db.dao.CachedBalanceDao
import com.divvy.divvy.offline.db.entity.CachedBalanceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineBalanceRepository @Inject constructor(
    private val remote: SupabaseBalanceRepository,
    private val balanceDao: CachedBalanceDao,
    private val networkMonitor: NetworkMonitor
) : BalanceRepository {

    override fun observeBalances(groupId: String): Flow<List<MemberBalance>> =
        balanceDao.getByGroupId(groupId).map { entities ->
            entities.map { it.toMemberBalance() }
        }

    override suspend fun refreshBalances(groupId: String) {
        if (!networkMonitor.isOnline.value) return
        try {
            remote.refreshBalances(groupId)
            val balances = remote.observeBalances(groupId).first()
            balanceDao.deleteByGroupId(groupId)
            balanceDao.insertAll(balances.map { it.toEntity(groupId) })
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh balances for group $groupId")
        }
    }

    override fun clearCache(groupId: String) {
        remote.clearCache(groupId)
    }

    private fun CachedBalanceEntity.toMemberBalance(): MemberBalance = MemberBalance(
        userId = userId,
        name = name,
        balanceCents = balanceCents,
        currency = currency
    )

    private fun MemberBalance.toEntity(groupId: String): CachedBalanceEntity = CachedBalanceEntity(
        groupId = groupId,
        userId = userId,
        name = name,
        balanceCents = balanceCents,
        currency = currency
    )
}
