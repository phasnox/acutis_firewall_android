package com.acutis.firewall.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private val FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
        private val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        private val ADULT_BLOCK_ENABLED = booleanPreferencesKey("adult_block_enabled")
        private val MALWARE_BLOCK_ENABLED = booleanPreferencesKey("malware_block_enabled")
        private val GAMBLING_BLOCK_ENABLED = booleanPreferencesKey("gambling_block_enabled")
        private val SOCIAL_MEDIA_BLOCK_ENABLED = booleanPreferencesKey("social_media_block_enabled")
        private val AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        private const val PIN_HASH_KEY = "pin_hash"
    }

    val firewallEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[FIREWALL_ENABLED] ?: false
    }

    val pinEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PIN_ENABLED] ?: false
    }

    val adultBlockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ADULT_BLOCK_ENABLED] ?: true
    }

    val malwareBlockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[MALWARE_BLOCK_ENABLED] ?: true
    }

    val gamblingBlockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[GAMBLING_BLOCK_ENABLED] ?: false
    }

    val socialMediaBlockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SOCIAL_MEDIA_BLOCK_ENABLED] ?: false
    }

    val autoStartEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTO_START_ENABLED] ?: true
    }

    suspend fun isFirewallEnabled(): Boolean = firewallEnabled.first()

    suspend fun setFirewallEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[FIREWALL_ENABLED] = enabled
        }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PIN_ENABLED] = enabled
        }
    }

    suspend fun setAdultBlockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[ADULT_BLOCK_ENABLED] = enabled
        }
    }

    suspend fun setMalwareBlockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MALWARE_BLOCK_ENABLED] = enabled
        }
    }

    suspend fun setGamblingBlockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[GAMBLING_BLOCK_ENABLED] = enabled
        }
    }

    suspend fun setSocialMediaBlockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SOCIAL_MEDIA_BLOCK_ENABLED] = enabled
        }
    }

    suspend fun setAutoStartEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_START_ENABLED] = enabled
        }
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        encryptedPrefs.edit().putString(PIN_HASH_KEY, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString(PIN_HASH_KEY, null) ?: return false
        return hashPin(pin) == storedHash
    }

    fun hasPin(): Boolean {
        return encryptedPrefs.getString(PIN_HASH_KEY, null) != null
    }

    fun clearPin() {
        encryptedPrefs.edit().remove(PIN_HASH_KEY).apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
