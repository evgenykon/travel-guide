package com.evgenykon.travelguide.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    var yandexSaKeyJson: String?
        get() = prefs.getString(KEY_YANDEX_KEY, null)
        set(value) = prefs.edit().putString(KEY_YANDEX_KEY, value).apply()

    var iamToken: String?
        get() = prefs.getString(KEY_IAM_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_IAM_TOKEN, value).apply()

    var iamExpiresAt: Long
        get() = prefs.getLong(KEY_IAM_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_IAM_EXPIRES, value).apply()

    var openRouterKey: String?
        get() = prefs.getString(KEY_OPENROUTER, null)
        set(value) = prefs.edit().putString(KEY_OPENROUTER, value).apply()

    fun clearYandex() {
        prefs.edit()
            .remove(KEY_YANDEX_KEY)
            .remove(KEY_IAM_TOKEN)
            .remove(KEY_IAM_EXPIRES)
            .apply()
    }

    fun clearOpenRouter() {
        prefs.edit().remove(KEY_OPENROUTER).apply()
    }

    private companion object {
        const val FILE_NAME = "secure_store"
        const val KEY_YANDEX_KEY = "yandex_sa_key"
        const val KEY_IAM_TOKEN = "yandex_iam_token"
        const val KEY_IAM_EXPIRES = "yandex_iam_expires"
        const val KEY_OPENROUTER = "openrouter_key"

        fun createPrefs(context: Context): SharedPreferences = try {
            create(context)
        } catch (e: Exception) {
            context.deleteSharedPreferences(FILE_NAME)
            create(context)
        }

        fun create(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
