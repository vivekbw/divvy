package com.divvy.divvy.backend

import com.divvy.divvy.components.GroupIcon
import com.divvy.divvy.models.FriendBalance
import com.divvy.divvy.models.Group
import com.divvy.divvy.models.GroupBalance
import com.divvy.divvy.models.ProfileRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

data class FriendWithGroups(
    val profile: ProfileRow,
    val sharedGroups: List<Group>
)

interface FriendsRepository {
    suspend fun getFriendsWithGroups(): List<FriendWithGroups>
    suspend fun getFriendsBalances(): List<FriendBalance>
    suspend fun getProfileByPhone(phone: String): ProfileRow?
    suspend fun getProfileByEmail(email: String): ProfileRow?
    suspend fun getProfilesByPhones(phones: List<String>): List<ProfileRow>
    suspend fun getProfilesByEmails(emails: List<String>): List<ProfileRow>
}

@Serializable
private data class FriendBalanceRow(
    @SerialName("user_id") val userId: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("group_icon") val groupIcon: String? = null,
    @SerialName("currency") val currency: String = "USD",
    @SerialName("balance_cents") val balanceCents: Long = 0L
)

@Serializable
private data class FriendGroupRow(
    @SerialName("user_id") val userId: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("group_icon") val groupIcon: String? = null
)

@Singleton
class SupabaseFriendsRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FriendsRepository {

    override suspend fun getFriendsWithGroups(): List<FriendWithGroups> {
        val rows = supabaseClient.postgrest
            .rpc("get_friends_with_groups")
            .decodeList<FriendGroupRow>()

        return rows.groupBy { it.userId }.map { (userId, userRows) ->
            val first = userRows.first()
            FriendWithGroups(
                profile = ProfileRow(
                    id = userId,
                    firstName = first.firstName.orEmpty(),
                    lastName = first.lastName.orEmpty(),
                    email = first.email,
                    phone = first.phone
                ),
                sharedGroups = userRows.map { row ->
                    Group(
                        id = row.groupId,
                        name = row.groupName,
                        icon = row.groupIcon?.let { iconName ->
                            runCatching { GroupIcon.valueOf(iconName) }.getOrDefault(GroupIcon.Group)
                        } ?: GroupIcon.Group
                    )
                }
            )
        }
    }

    override suspend fun getFriendsBalances(): List<FriendBalance> {
        val rows = supabaseClient.postgrest
            .rpc("get_friends_balances")
            .decodeList<FriendBalanceRow>()

        return rows.groupBy { it.userId }.map { (userId, userRows) ->
            val first = userRows.first()
            FriendBalance(
                userId = userId,
                firstName = first.firstName,
                lastName = first.lastName,
                email = first.email,
                phone = first.phone,
                groupBalances = userRows.map { row ->
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

    override suspend fun getProfileByPhone(phone: String): ProfileRow? {
        return supabaseClient.from("profiles")
            .select {
                filter { eq("phone", phone) }
                limit(1)
            }
            .decodeSingleOrNull<ProfileRow>()
    }

    override suspend fun getProfileByEmail(email: String): ProfileRow? {
        return supabaseClient.from("profiles")
            .select {
                filter { eq("email", email) }
                limit(1)
            }
            .decodeSingleOrNull<ProfileRow>()
    }

    override suspend fun getProfilesByPhones(phones: List<String>): List<ProfileRow> {
        if (phones.isEmpty()) return emptyList()
        return supabaseClient.from("profiles")
            .select {
                filter { isIn("phone", phones) }
            }
            .decodeList<ProfileRow>()
    }

    override suspend fun getProfilesByEmails(emails: List<String>): List<ProfileRow> {
        if (emails.isEmpty()) return emptyList()
        return supabaseClient.from("profiles")
            .select {
                filter { isIn("email", emails) }
            }
            .decodeList<ProfileRow>()
    }
}
