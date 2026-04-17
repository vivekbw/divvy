package com.divvy.divvy.backend

import com.divvy.divvy.models.ProfileRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

interface ProfilesRepository {
    suspend fun upsertProfile(profile: ProfileRow)
    suspend fun getProfile(userId: String): ProfileRow?
    suspend fun listAllProfiles(): List<ProfileRow>
}

class SupabaseProfilesRepository @Inject constructor(
    private val client: SupabaseClient
) : ProfilesRepository {

    override suspend fun upsertProfile(profile: ProfileRow) {
        client.from("profiles").upsert(
            value = profile,
            onConflict = "id"
        )
    }

    override suspend fun getProfile(userId: String): ProfileRow? {
        return client.from("profiles")
            .select {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingleOrNull()
    }

    override suspend fun listAllProfiles(): List<ProfileRow> {
        return client.from("profiles").select().decodeList()
    }
}
