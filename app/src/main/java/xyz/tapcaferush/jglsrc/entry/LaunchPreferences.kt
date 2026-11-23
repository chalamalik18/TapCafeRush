package xyz.tapcaferush.jglsrc.entry

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.launchStore by preferencesDataStore(name = "cafe_launch")

class LaunchPreferences(private val context: Context) {
    private val sessionKey = stringPreferencesKey("access_token")
    private val addressKey = stringPreferencesKey("resource_address")

    private val redirectKey = booleanPreferencesKey("json_redirect")

    suspend fun savedDestination(): String? {
        val values = context.launchStore.data.first()
        val confirmed = values[redirectKey] == true || !values[sessionKey].isNullOrBlank()
        return values[addressKey]?.takeIf { confirmed && it.isNotBlank() }
    }

    suspend fun readAccessToken() = context.launchStore.data.first()[sessionKey]
    suspend fun readResourceAddress() = context.launchStore.data.first()[addressKey]

    suspend fun save(session: String?, address: String) {
        context.launchStore.edit { values ->
            values[redirectKey] = session == null
            if (session == null) values.remove(sessionKey) else values[sessionKey] = session
            values[addressKey] = address
        }
    }
}
