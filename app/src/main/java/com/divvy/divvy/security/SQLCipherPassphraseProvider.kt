package com.divvy.divvy.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

object SQLCipherPassphraseProvider {

    private const val PREFS_NAME = "divvy_db_key"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_LENGTH = 32

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val prefs = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return existing.toByteArray(Charsets.ISO_8859_1)
        }

        val passphrase = ByteArray(PASSPHRASE_LENGTH).also {
            SecureRandom().nextBytes(it)
        }
        prefs.edit()
            .putString(KEY_PASSPHRASE, String(passphrase, Charsets.ISO_8859_1))
            .apply()

        return passphrase
    }
}
