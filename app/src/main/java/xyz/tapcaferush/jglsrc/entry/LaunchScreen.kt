package xyz.tapcaferush.jglsrc.entry

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LaunchScreen(
    prefs: LaunchPreferences,
    onOpenShell: (String) -> Unit,
    onOpenApp: () -> Unit,
    entryLoader: (suspend () -> String?)? = null
) {
    androidx.activity.compose.BackHandler {}
    val context = LocalContext.current
    val launchService = remember { LaunchService(context.applicationContext) }
    var serviceUnavailable by remember { mutableStateOf(false) }

    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        serviceUnavailable = false
        val savedAddress = prefs.savedDestination()
        if (savedAddress != null) {
            onOpenShell(savedAddress)
            return@LaunchedEffect
        }

        val replyText = if (entryLoader != null) entryLoader() else launchService.fetchEntryPayload()

        if (replyText.isNullOrBlank()) {
            serviceUnavailable = true
            return@LaunchedEffect
        }

        if (replyText.trimStart().startsWith("{")) {
            val address = findRedirectAddress(replyText)
            if (address != null) {
                prefs.save(null, address)
                onOpenShell(address)
                return@LaunchedEffect
            }
        } else if (replyText.contains("#")) {
            val segments = replyText.split("#", limit = 2)
            val token = segments[0]
            val link = segments.getOrNull(1).orEmpty()
            if (token.isNotBlank() && link.isNotBlank()) {
                prefs.save(token, link)
                onOpenShell(link)
                return@LaunchedEffect
            }
        }

        onOpenApp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (serviceUnavailable) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Unable to reach the service",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Check your connection and try again. The service may be temporarily unavailable.",
                    color = Color(0xFFB8B8B8),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { attempt += 1 }) {
                    Text("Try again")
                }
            }
        } else {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

private fun findRedirectAddress(body: String): String? {
    val stream = JSONTokener(body.trim())
    while (stream.more()) {
        val record = try {
            stream.nextValue()
        } catch (_: JSONException) {
            return null
        }
        val address = ((record as? JSONObject)?.opt("rdstr") as? String)?.trim() ?: continue
        if (address.toHttpUrlOrNull() != null) return address
    }
    return null
}
