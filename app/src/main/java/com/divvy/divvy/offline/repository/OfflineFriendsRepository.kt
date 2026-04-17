package com.divvy.divvy.offline.repository

import com.divvy.divvy.backend.FriendsRepository
import com.divvy.divvy.backend.FriendWithGroups
import com.divvy.divvy.backend.SupabaseFriendsRepository
import com.divvy.divvy.models.FriendBalance
import com.divvy.divvy.models.GroupBalance
import com.divvy.divvy.models.ProfileRow
import com.divvy.divvy.offline.NetworkMonitor
import com.divvy.divvy.offline.db.dao.CachedFriendBalanceDao
import com.divvy.divvy.offline.db.entity.CachedFriendBalanceEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFriendsRepository @Inject constructor(
    private val remote: SupabaseFriendsRepository,
    private val friendBalanceDao: CachedFriendBalanceDao,
    private val networkMonitor: NetworkMonitor
) : FriendsRepository {

    override suspend fun getFriendsBalances(): List<FriendBalance> {
        if (!networkMonitor.isOnline.value) {
            return getCachedBalances()
        }
        return try {
            val fresh = remote.getFriendsBalances()
            updateCache(fresh)
            fresh
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch friends balances, falling back to cache")
            getCachedBalances()
        }
    }

    override suspend fun getFriendsWithGroups(): List<FriendWithGroups> =
        remote.getFriendsWithGroups()

    override suspend fun getProfileByPhone(phone: String): ProfileRow? =
        remote.getProfileByPhone(phone)

    override suspend fun getProfileByEmail(email: String): ProfileRow? =
        remote.getProfileByEmail(email)

    override suspend fun getProfilesByPhones(phones: List<String>): List<ProfileRow> =
        remote.getProfilesByPhones(phones)

    override suspend fun getProfilesByEmails(emails: List<String>): List<ProfileRow> =
        remote.getProfilesByEmails(emails)

    private suspend fun getCachedBalances(): List<FriendBalance> {
        val entities = friendBalanceDao.getAll()
        if (entities.isEmpty()) return emptyList()
        return entities.groupBy { it.userId }.map { (userId, rows) ->
            val first = rows.first()
            FriendBalance(
                userId = userId,
                firstName = first.firstName,
                lastName = first.lastName,
                email = first.email,
                phone = first.phone,
                groupBalances = rows.map { row ->
                    GroupBalance(
                        groupId = row.groupId,
                        groupName = row.groupName,
                        groupIcon = row.groupIcon,
                        balanceCents = row.balanceCents,
                        currency = row.currency
                    )
                }
            )
        }
    }

    private suspend fun updateCache(balances: List<FriendBalance>) {
        val entities = balances.flatMap { fb ->
            fb.groupBalances.map { gb ->
                CachedFriendBalanceEntity(
                    userId = fb.userId,
                    firstName = fb.firstName,
                    lastName = fb.lastName,
                    email = fb.email,
                    phone = fb.phone,
                    groupId = gb.groupId,
                    groupName = gb.groupName,
                    groupIcon = gb.groupIcon,
                    currency = gb.currency,
                    balanceCents = gb.balanceCents
                )
            }
        }
        friendBalanceDao.deleteAll()
        friendBalanceDao.insertAll(entities)
    }
}
