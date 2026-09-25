package com.ripple.filemanager.data.webdav

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Persists saved WebDAV connections using Gson + SharedPreferences,
 * with passwords in EncryptedSharedPreferences — matching SmbStore's pattern exactly.
 */
class WebDavStore(context: Context) {

    private val prefs = context.getSharedPreferences("webdav_connections", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val encryptedPrefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "webdav_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            try {
                context.deleteSharedPreferences("webdav_secure_prefs")
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "webdav_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                context.getSharedPreferences("webdav_secure_prefs_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    fun getConnections(): PersistentList<WebDavConnection> {
        val json = prefs.getString("connections_list", null) ?: return persistentListOf()
        val type = object : TypeToken<List<WebDavConnection>>() {}.type
        val list: List<WebDavConnection> = gson.fromJson(json, type) ?: emptyList()
        return list.toPersistentList()
    }

    fun saveConnection(connection: WebDavConnection, password: String) {
        val current = getConnections().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == connection.id }
        if (existingIndex != -1) {
            current[existingIndex] = connection
        } else {
            current.add(connection)
        }

        prefs.edit().putString("connections_list", gson.toJson(current)).apply()
        encryptedPrefs.edit().putString("pwd_${connection.id}", password).apply()
    }

    fun deleteConnection(connectionId: String) {
        val current = getConnections().toMutableList()
        current.removeAll { it.id == connectionId }

        prefs.edit().putString("connections_list", gson.toJson(current)).apply()
        encryptedPrefs.edit().remove("pwd_$connectionId").apply()
    }

    fun getPassword(connectionId: String): String? {
        return encryptedPrefs.getString("pwd_$connectionId", null)
    }
}
