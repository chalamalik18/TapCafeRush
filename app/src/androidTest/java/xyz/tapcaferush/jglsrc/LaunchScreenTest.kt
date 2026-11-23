package xyz.tapcaferush.jglsrc

import android.util.Base64
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import xyz.tapcaferush.jglsrc.entry.LaunchPreferences
import xyz.tapcaferush.jglsrc.entry.LaunchService
import xyz.tapcaferush.jglsrc.ui.theme.TapCafeRushTheme
import xyz.tapcaferush.jglsrc.entry.LaunchScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class LaunchScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = LaunchPreferences(context)

    @Before fun reset() = runBlocking {
        prefs.save("", "")
    }

    @Test fun gatewayPreservesEncodedDeviceFields() {
        val address = LaunchService(context).buildEntryAddress().toHttpUrl()
        assertEquals("gtappinfo.top", address.host)
        assertEquals("/a-vdm-30/otp.php", address.encodedPath)
        assertNull(address.queryParameter("p"))
        val parameters = String(Base64.decode(address.queryParameter("token"), Base64.NO_WRAP), Charsets.UTF_8)
        assertTrue(parameters.startsWith("&p=FDgAq3FDsah5gJk&os=Android "))
        for (field in listOf("lng", "loc", "devicemodel", "bs", "bl", "nc", "sm", "dv")) {
            assertTrue("Missing field $field", parameters.contains("&$field="))
        }
    }

    @Test fun serverResponseOpensPortalAndPersistsIt() {
        val opened = AtomicReference<String?>()
        compose.setContent {
            TapCafeRushTheme(darkTheme = true) {
                LaunchScreen(prefs, { opened.set(it) }, { fail("Unexpected native route") },
                    entryLoader = { "sample-token#https://example.test/start#fragment" })
            }
        }
        compose.waitUntil(5000) { opened.get() != null }
        assertEquals("https://example.test/start#fragment", opened.get())
        runBlocking {
            assertEquals("sample-token", prefs.readAccessToken())
            assertEquals(opened.get(), prefs.readResourceAddress())
        }
    }

    @Test fun redirectAfterOtpOpensPortalAndIsSaved() {
        val response = """{"status":"otp"}{"rdstr":null}{"status":"rd","rdstr":"https\://convertio.co/jpg-png/"}"""
        val address = AtomicReference<String?>()
        compose.setContent {
            LaunchScreen(prefs, address::set, { fail("Expected browser") }, entryLoader = { response })
        }
        compose.waitUntil(5000) { address.get() != null }
        assertEquals("https://convertio.co/jpg-png/", address.get())
        assertEquals(address.get(), runBlocking { prefs.savedDestination() })
    }

    @Test fun jsonWithoutRedirectOpensGameEvenWithHashInMessage() {
        val opened = AtomicInteger()
        compose.setContent {
            LaunchScreen(prefs, { fail("Unexpected browser") }, { opened.incrementAndGet() },
                entryLoader = { """{"status":"otp","message":"#otp"}{"rdstr":""}""" })
        }
        compose.waitUntil(5000) { opened.get() == 1 }
    }

    @Test fun cachedAddressSkipsNetworkLookup() {
        runBlocking {
            prefs.save("cached", "https://example.test/saved")
        }
        val opened = AtomicReference<String?>()
        val calls = AtomicInteger()
        compose.setContent {
            TapCafeRushTheme(darkTheme = true) {
                LaunchScreen(prefs, { opened.set(it) }, { fail("Unexpected native route") },
                    entryLoader = { calls.incrementAndGet(); "native" })
            }
        }
        compose.waitUntil(5000) { opened.get() != null }
        assertEquals("https://example.test/saved", opened.get())
        assertEquals(0, calls.get())
    }

    @Test fun partialPairTriggersLookup() {
        runBlocking { prefs.save("cached", "") }
        val opened = AtomicInteger()
        compose.setContent {
            LaunchScreen(prefs, { fail("Unexpected browser") }, { opened.incrementAndGet() }, entryLoader = { "native" })
        }
        compose.waitUntil(5000) { opened.get() == 1 }
    }

    @Test fun plainResponseOpensNativeSections() {
        val opened = AtomicInteger()
        compose.setContent {
            TapCafeRushTheme(darkTheme = true) { LaunchScreen(prefs, { fail("Unexpected portal") }, { opened.incrementAndGet() }, entryLoader = { "native" }) }
        }
        compose.waitUntil(5000) { opened.get() == 1 }
    }

    @Test fun missingResponseDisplaysConnectionMessage() {
        compose.setContent {
            TapCafeRushTheme(darkTheme = true) { LaunchScreen(prefs, { fail("Unexpected portal") }, { fail("Unexpected native route") }, entryLoader = { null }) }
        }
        compose.onNodeWithText("Unable to reach the service").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("Unable to reach the service").assertIsDisplayed()
    }

    @Test fun pendingResponseDoesNotShowPrematureConnectionError() {
        val response = CompletableDeferred<String?>()
        val started = AtomicInteger()
        val opened = AtomicInteger()
        compose.setContent {
            TapCafeRushTheme(darkTheme = true) {
                LaunchScreen(prefs, { fail("Unexpected portal") }, { opened.incrementAndGet() },
                    entryLoader = { started.incrementAndGet(); response.await() })
            }
        }
        compose.waitUntil(5000) { started.get() == 1 }
        // This exceeds the old eight-second timer while the request is still active.
        Thread.sleep(8500)
        compose.onNodeWithText("Unable to reach the service").assertDoesNotExist()
        compose.onNodeWithText("Try again").assertDoesNotExist()
        response.complete("native")
        compose.waitUntil(5000) { opened.get() == 1 }
    }

    @Test fun retryRepeatsLookupAndOpensReturnedRoute() {
        val calls = AtomicInteger()
        val opened = AtomicInteger()
        compose.setContent {
            TapCafeRushTheme(darkTheme = true) {
                LaunchScreen(prefs, { fail("Unexpected portal") }, { opened.incrementAndGet() },
                    entryLoader = { if (calls.incrementAndGet() == 1) null else "native" })
            }
        }
        compose.onNodeWithText("Unable to reach the service").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        compose.waitUntil(5000) { opened.get() == 1 }
        assertEquals(2, calls.get())
    }

}
