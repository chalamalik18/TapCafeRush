package xyz.tapcaferush.jglsrc.entry

import xyz.tapcaferush.jglsrc.AppUrls
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

class LaunchService(private val context: Context) {

    internal val requestDeadlines = listOf(15L, 15L, 30L)

    private fun buildClient(timeoutSeconds: Long): OkHttpClient =
        OkHttpClient.Builder()
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .cache(null)
            .build()

    internal suspend fun execute(
        address: String,
        deadlines: List<Long> = requestDeadlines
    ): String? = withContext(Dispatchers.IO) {
        for (timeout in deadlines) {
            val response = runCatching {
                val client = buildClient(timeout)
                val request = Request.Builder()
                    .url(address)
                    .cacheControl(CacheControl.Builder().noCache().noStore().build())
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .build()
                client.newCall(request).execute().use { reply ->
                    if (reply.isSuccessful) reply.body?.string() else null
                }
            }.onFailure { error ->
                if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    Log.w("CafeEntry", "Connection attempt failed: ${error.javaClass.simpleName}; cause=${error.cause?.javaClass?.simpleName}")
                }
            }.getOrNull()
            if (!response.isNullOrBlank()) return@withContext response
        }
        null
    }

    suspend fun fetchEntryPayload(): String? = execute(buildEntryAddress())

    internal fun buildEntryAddress(): String {
        val profile = DeviceProfile(context)
        val parameters = StringBuilder()
            .append("&p=HFdDFHGdGHdsG")
            .append("&os=").append(profile.osLabel())
            .append("&lng=").append(profile.language())
            .append("&loc=").append(profile.region())
            .append("&devicemodel=").append(profile.deviceModel())
            .append("&bs=").append(profile.batteryStatus())
            .append("&bl=").append(profile.batteryLevel())
            .append("&nc=").append(profile.getNetworkCountry())
            .append("&sm=").append(profile.getSimState())
            .append("&dv=").append(if (profile.isDeveloperOptionsEnabled()) "1" else "0")
            .toString()
        val token = Base64.encodeToString(parameters.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val address = AppUrls.LAUNCH_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("token", token)
            .build().toString()
        return address
    }

}
