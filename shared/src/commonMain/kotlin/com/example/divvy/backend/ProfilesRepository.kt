package com.example.divvy.backend

import com.example.divvy.models.ProfileRow
import io.github.jan.supabase.postgrest.from

interface ProfilesRepository {
    suspend fun upsertProfile(profile: ProfileRow)
    suspend fun getProfile(userId: String): ProfileRow?
}

class SupabaseProfilesRepository : ProfilesRepository {
    private val client get() = SupabaseClientProvider.client

    override suspend fun upsertProfile(profile: ProfileRow) {
        client.from("profiles").upsert(value = profile, onConflict = "id")
    }

    override suspend fun getProfile(userId: String): ProfileRow? {
        return client.from("profiles")
            .select { filter { eq("id", userId) }; limit(1) }
            .decodeSingleOrNull()
    }
}
