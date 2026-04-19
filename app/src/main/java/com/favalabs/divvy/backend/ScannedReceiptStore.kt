package com.favalabs.divvy.backend

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.favalabs.divvy.models.ParsedReceipt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannedReceiptStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val receiptKey = "receipt_json"

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "scanned_receipt_encrypted",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private var _receipt: ParsedReceipt? = null

    fun store(receipt: ParsedReceipt) {
        _receipt = receipt
        persistToPrefs(receipt)
    }

    fun peek(): ParsedReceipt? {
        if (_receipt != null) return _receipt
        _receipt = readFromPrefs()
        return _receipt
    }

    fun consume(): ParsedReceipt? {
        val receipt = peek()
        _receipt = null
        clearPrefs()
        return receipt
    }

    fun clear() {
        _receipt = null
        clearPrefs()
    }

    private fun persistToPrefs(receipt: ParsedReceipt) {
        try {
            encryptedPrefs.edit()
                .putString(receiptKey, json.encodeToString(receipt))
                .apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to persist receipt")
        }
    }

    private fun readFromPrefs(): ParsedReceipt? {
        return try {
            encryptedPrefs.getString(receiptKey, null)?.let { jsonStr ->
                json.decodeFromString<ParsedReceipt>(jsonStr)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to read receipt")
            null
        }
    }

    private fun clearPrefs() {
        try {
            encryptedPrefs.edit()
                .remove(receiptKey)
                .apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to clear receipt")
        }
    }
}
