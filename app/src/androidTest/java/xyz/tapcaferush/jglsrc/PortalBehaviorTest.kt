package xyz.tapcaferush.jglsrc

import android.content.Intent
import android.content.ClipData
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.app.DownloadManager
import android.content.Context
import android.webkit.ValueCallback
import android.net.Uri
import androidx.test.espresso.Espresso
import androidx.test.filters.SdkSuppress
import android.os.Build
import android.view.MotionEvent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import xyz.tapcaferush.jglsrc.entry.BrowserActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class PortalBehaviorTest {
    @Test fun browserOptionsScriptsHistoryCookiesAndReopen() {
        FixtureServer().use { server ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val intent = Intent(context, BrowserActivity::class.java)
                .putExtra(BrowserActivity.EXTRA_PORTAL_LINK, server.address + "/first")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<BrowserActivity>(intent).use { scenario ->
                await(scenario, "document.title", "First")
                scenario.onActivity { activity ->
                    val pane = findPane(activity.window.decorView)!!
                    assertTrue(pane.settings.javaScriptEnabled)
                    assertTrue(pane.settings.domStorageEnabled)
                    assertTrue(pane.settings.allowContentAccess)
                    assertTrue(pane.settings.allowFileAccess)
                    assertFalse(pane.settings.mediaPlaybackRequiresUserGesture)
                    assertEquals(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW, pane.settings.mixedContentMode)
                    assertEquals(WebSettings.LOAD_NO_CACHE, pane.settings.cacheMode)
                    if (Build.VERSION.SDK_INT >= 26) assertFalse(pane.settings.safeBrowsingEnabled)
                    assertFalse(pane.settings.userAgentString.contains("; wv"))
                    assertTrue(CookieManager.getInstance().acceptThirdPartyCookies(pane))
                }
                assertEquals("\"enabled\"", evaluate(scenario, "localStorage.getItem('probe')"))
                assertTrue(evaluate(scenario, "document.cookie").contains("cafe_session=ready"))
                scenario.onActivity { findPane(it.window.decorView)!!.loadUrl(server.address + "/second") }
                await(scenario, "document.title", "Second")
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                await(scenario, "document.title", "First")
                // At the first page Back must keep the activity on screen.
                scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                    assertFalse(activity.isFinishing)
                }
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                await(scenario, "document.title", "First")
                val rotateDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                var rotated = false
                while (!rotated && System.nanoTime() < rotateDeadline) {
                    scenario.onActivity { rotated = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
                    if (!rotated) Thread.sleep(100)
                }
                assertTrue("Portal did not rotate", rotated)
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                evaluate(scenario, "document.cookie='restart_marker=kept;max-age=3600;path=/'")
                scenario.recreate()
                await(scenario, "document.title", "First")
                assertTrue(evaluate(scenario, "document.cookie").contains("restart_marker=kept"))
            }
        }
    }

    @Test fun downloadableFileReachesSystemDownloadManager() {
        FixtureServer().use { server ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val intent = Intent(context, BrowserActivity::class.java)
                .putExtra(BrowserActivity.EXTRA_PORTAL_LINK, server.address + "/first")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<BrowserActivity>(intent).use { scenario ->
                await(scenario, "document.title", "First")
                val fileName = "cafe-fixture-${System.nanoTime()}.txt"
                evaluate(scenario, "document.body.innerHTML=" + org.json.JSONObject.quote("<a href='/download/$fileName' style='position:fixed;top:0;left:0;width:100%;height:100%;display:block'>Download fixture</a>") + ";")
                assertTrue(evaluate(scenario, "document.body.innerHTML").contains(fileName))
                val position = AtomicReference<Pair<Float, Float>>()
                scenario.onActivity {
                    val pane = findPane(it.window.decorView)!!
                    val point = IntArray(2)
                    pane.getLocationOnScreen(point)
                    position.set((point[0] + pane.width / 2f) to (point[1] + pane.height / 2f))
                }
                Thread.sleep(200)
                val (x, y) = position.get()
                val now = SystemClock.uptimeMillis()
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, x, y, 0).also {
                        instrumentation.sendPointerSync(it); it.recycle()
                    }
                }
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
                var completedId: Long? = null
                var observed = "no record"
                while (System.nanoTime() < deadline && completedId == null) {
                    manager.query(DownloadManager.Query()).use { cursor ->
                        while (cursor.moveToNext()) {
                            if (cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)) == server.address + "/download/" + fileName) {
                                observed = "status=${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))}, reason=${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}"
                            }
                            if (cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)) == server.address + "/download/" + fileName &&
                                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                                completedId = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                            }
                        }
                    }
                    if (completedId == null) Thread.sleep(100)
                }
                assertNotNull("File download did not finish: $observed; requests=${server.requests}; page=${evaluate(scenario, "location.href")}", completedId)
                android.os.ParcelFileDescriptor.AutoCloseInputStream(manager.openDownloadedFile(completedId!!)).bufferedReader().use {
                    assertEquals("Tap Cafe fixture", it.readText())
                }
                manager.remove(completedId!!)
            }
        }
    }

    @Test @SdkSuppress(minSdkVersion = 26)
    fun filePickerCanBeCancelledWithoutClosingPortal() {
        FixtureServer().use { server ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val intent = Intent(context, BrowserActivity::class.java)
                .putExtra(BrowserActivity.EXTRA_PORTAL_LINK, server.address + "/first")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<BrowserActivity>(intent).use { scenario ->
                await(scenario, "document.title", "First")
                val done = CountDownLatch(1)
                val received = AtomicReference<Array<Uri>?>()
                scenario.onActivity { activity ->
                    val pane = findPane(activity.window.decorView)!!
                    assertTrue(pane.webChromeClient!!.onShowFileChooser(pane,
                        ValueCallback { received.set(it); done.countDown() }, null))
                }
                Thread.sleep(500)
                pressSystemBack()
                assertTrue("Picker callback not delivered", done.await(5, TimeUnit.SECONDS))
                assertNull(received.get())
                scenario.onActivity { assertFalse(it.isFinishing) }
                await(scenario, "document.title", "First")
            }
        }
    }

    @Test @SdkSuppress(minSdkVersion = 26)
    fun multipleSelectedFilesAreForwardedToThePage() {
        FixtureServer().use { server ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val intent = Intent(context, BrowserActivity::class.java)
                .putExtra(BrowserActivity.EXTRA_PORTAL_LINK, server.address + "/first")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<BrowserActivity>(intent).use { scenario ->
                await(scenario, "document.title", "First")
                val received = AtomicReference<Array<Uri>?>()
                val done = CountDownLatch(1)
                val first = Uri.parse("content://cafe.fixture/first")
                val second = Uri.parse("content://cafe.fixture/second")
                scenario.onActivity { activity ->
                    val pane = findPane(activity.window.decorView)!!
                    assertTrue(pane.webChromeClient!!.onShowFileChooser(pane,
                        ValueCallback { received.set(it); done.countDown() }, null))
                    val clip = ClipData.newRawUri("Selected files", first)
                    clip.addItem(ClipData.Item(second))
                    val result = Intent().apply { clipData = clip }
                    BrowserActivity::class.java.getDeclaredMethod("onActivityResult",
                        Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java).apply {
                            isAccessible = true
                        }.invoke(activity, 4829, Activity.RESULT_OK, result)
                }
                assertTrue(done.await(5, TimeUnit.SECONDS))
                assertArrayEquals(arrayOf(first, second), received.get())
                Thread.sleep(300)
                pressSystemBack()
                scenario.onActivity { assertFalse(it.isFinishing) }
            }
        }
    }

    @Test @SdkSuppress(minSdkVersion = 26)
    fun specialAddressOpensExternalActivityAndBackReturns() {
        FixtureServer().use { server ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val intent = Intent(context, BrowserActivity::class.java)
                .putExtra(BrowserActivity.EXTRA_PORTAL_LINK, server.address + "/first")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<BrowserActivity>(intent).use { scenario ->
                await(scenario, "document.title", "First")
                val request = object : android.webkit.WebResourceRequest {
                    override fun getUrl() = Uri.parse("intent:#Intent;action=android.settings.SETTINGS;end")
                    override fun isForMainFrame() = true
                    override fun isRedirect() = false
                    override fun hasGesture() = true
                    override fun getMethod() = "GET"
                    override fun getRequestHeaders() = emptyMap<String, String>()
                }
                scenario.onActivity {
                    val pane = findPane(it.window.decorView)!!
                    assertTrue(pane.webViewClient!!.shouldOverrideUrlLoading(pane, request))
                }
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                var opened = false
                while (!opened && System.nanoTime() < deadline) {
                    opened = instrumentation.uiAutomation.rootInActiveWindow?.packageName == "com.android.settings"
                    if (!opened) Thread.sleep(100)
                }
                assertTrue("External activity did not open", opened)
                pressSystemBack()
                scenario.onActivity { assertFalse(it.isFinishing) }
                await(scenario, "document.title", "First")
            }
        }
    }

    private fun pressSystemBack() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val output = automation.executeShellCommand("input keyevent KEYCODE_BACK")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    private fun evaluate(scenario: ActivityScenario<BrowserActivity>, script: String): String {
        val result = AtomicReference<String>()
        val done = CountDownLatch(1)
        scenario.onActivity { activity ->
            findPane(activity.window.decorView)!!.evaluateJavascript(script) { value ->
                result.set(value); done.countDown()
            }
        }
        assertTrue("Script did not complete", done.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    private fun await(scenario: ActivityScenario<BrowserActivity>, script: String, expected: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < deadline) {
            if (evaluate(scenario, script) == "\"$expected\"") return
            Thread.sleep(100)
        }
        fail("Expected $expected")
    }

    private fun findPane(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            findPane(view.getChildAt(i))?.let { return it }
        }
        return null
    }
}

private class FixtureServer : AutoCloseable {
    private val socket = ServerSocket(0)
    val address = "http://127.0.0.1:${socket.localPort}"
    val requests = java.util.concurrent.CopyOnWriteArrayList<String>()
    @Volatile private var active = true
    private val worker = thread(isDaemon = true, name = "CafeFixture") {
        while (active) {
            try {
                socket.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader()
                    val path = reader.readLine()?.split(" ")?.getOrNull(1).orEmpty()
                    requests.add(path)
                    while (!reader.readLine().isNullOrEmpty()) { }
                    val title = if (path.startsWith("/second")) "Second" else "First"
                    val downloading = path.startsWith("/download/")
                    val body = if (downloading) "Tap Cafe fixture".toByteArray() else "<html><head><title>$title</title><meta name='viewport' content='width=device-width,initial-scale=1'></head><body><h1>$title</h1><script>localStorage.setItem('probe','enabled');document.cookie='cafe_session=ready;path=/';</script><a href='/second'>Next page</a></body></html>".toByteArray()
                    val mime = if (downloading) "application/octet-stream" else "text/html"
                    val attachment = if (downloading) "Content-Disposition: attachment; filename=${path.substringAfterLast('/')}\r\n" else ""
                    val headers = "HTTP/1.1 200 OK\r\nContent-Type: $mime\r\n$attachment" +
                        "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                    client.getOutputStream().write(headers.toByteArray() + body)
                }
            } catch (_: Exception) { }
        }
    }
    override fun close() { active = false; socket.close(); worker.join(1000) }
}
