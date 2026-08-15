package com.coffeelab.coffeenotes.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 云端备份凭据（EncryptedSharedPreferences 加密存储，绝不落明文）。
 * 密码在 SharedPreferences 内加密；配置是否完成以「地址+账号+密码 均非空」为准。
 */
object CloudBackupPrefs {

    private const val PREFS_NAME = "cloud_backup_prefs"
    private const val KEY_BASE_URL = "cloud_base_url"
    private const val KEY_USERNAME = "cloud_username"
    private const val KEY_PASSWORD = "cloud_password"

    data class CloudConfig(
        val baseUrl: String,
        val username: String,
        val password: String
    )

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveConfig(context: Context, config: CloudConfig) {
        prefs(context).edit()
            .putString(KEY_BASE_URL, config.baseUrl.trim().trimEnd('/'))
            .putString(KEY_USERNAME, config.username.trim())
            .putString(KEY_PASSWORD, config.password.trim())
            .apply()
    }

    fun getConfig(context: Context): CloudConfig? {
        val p = prefs(context)
        val url = p.getString(KEY_BASE_URL, "")?.trim().orEmpty()
        val user = p.getString(KEY_USERNAME, "")?.trim().orEmpty()
        val pass = p.getString(KEY_PASSWORD, "")?.trim().orEmpty()
        if (url.isBlank() || user.isBlank() || pass.isBlank()) return null
        return CloudConfig(url, user, pass)
    }

    fun isConfigured(context: Context): Boolean = getConfig(context) != null

    fun clearConfig(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
