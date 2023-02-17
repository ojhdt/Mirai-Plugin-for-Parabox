package com.ojhdtapp.miraipluginforparabox.core.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object DataStoreKeys{
    val AUTO_LOGIN = booleanPreferencesKey("auto_login")
    val FOREGROUND_SERVICE = booleanPreferencesKey("foreground_service")
    val CONTACT_CACHE = booleanPreferencesKey("contact_cache")
    val PROTOCOL = intPreferencesKey("protocol")
    val CANCEL_TIMEOUT = booleanPreferencesKey("cancel_timeout")

    val LAST_SUCCESSFUL_HANDLE_TIMESTAMP = longPreferencesKey("last_successful_handle_timestamp")
}