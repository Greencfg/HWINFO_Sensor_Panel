package com.green.hwinfo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val IP_ADDRESS_KEY = stringPreferencesKey("ip_address")
    }

    val ipAddress: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[IP_ADDRESS_KEY]
        }

    suspend fun saveIpAddress(ip: String) {
        context.dataStore.edit { preferences ->
            preferences[IP_ADDRESS_KEY] = ip
        }
    }
}
