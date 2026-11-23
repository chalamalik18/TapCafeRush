package xyz.tapcaferush.jglsrc.entry

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import xyz.tapcaferush.jglsrc.R

class BrowserActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PORTAL_LINK = "extra_portal_link"
        private const val FILE_GATEWAY_CODE = 4829
        private const val DOWNLOAD_PERMISSION_CODE = 4830
    }

    private lateinit var browserView: WebView
    private lateinit var loadingIndicator: ProgressBar
    private var fileSelection: ValueCallback<Array<Uri>>? = null
    private var loadingFirstPage = true
    private data class PendingDownload(val href: String, val agent: String, val disposition: String, val mime: String)
    private var pendingDownload: PendingDownload? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        val startAddress = intent.getStringExtra(EXTRA_PORTAL_LINK) ?: run {
            finish()
            return
        }

        assembleHostSurface()
        wireWebEngine()
        beginNavigation(startAddress)
        bindBackStack()
    }

    private fun assembleHostSurface() {
        val activityBg = ContextCompat.getColor(this, R.color.portal_activity_background)
        val surfaceBg = ContextCompat.getColor(this, R.color.portal_surface_bg)
        val loaderColor = ContextCompat.getColor(this, R.color.portal_loader_color)

        val container = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(activityBg)
        }

        browserView = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            id = View.generateViewId()
            setBackgroundColor(surfaceBg)
        }

        loadingIndicator = ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            indeterminateTintList = ColorStateList.valueOf(loaderColor)
            visibility = View.VISIBLE
        }

        container.addView(browserView)
        container.addView(loadingIndicator)
        setContentView(container)

        applyImmersiveChrome()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun wireWebEngine() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(browserView, true)
        }

        browserView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }

        browserView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = false
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            blockNetworkImage = false
            setSupportMultipleWindows(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
            userAgentString = userAgentString.replace("; wv", "").replace("Version/4.0", "")
        }

        browserView.setDownloadListener { href, agent, disposition, mime, _ ->
            requestDownload(href, agent, disposition, mime)
        }

        browserView.webViewClient = buildClientBridge()
        browserView.webChromeClient = buildChromeFacade()
    }

    private fun buildClientBridge(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (loadingFirstPage) {
                    loadingIndicator.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (loadingFirstPage) {
                    loadingFirstPage = false
                    loadingIndicator.visibility = View.GONE
                }
                CookieManager.getInstance().flush()
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                CookieManager.getInstance().flush()
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme ?: return false

                if (scheme in listOf("http", "https")) {
                    return false
                }

                return try {
                    val externalIntent = if (scheme == "intent") {
                        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }

                    openExternalTarget(view?.context ?: return true, externalIntent)
                    true
                } catch (_: Exception) {
                    true
                }
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (!isFinishing && !isDestroyed) {
                    recreate()
                }
                return true
            }
        }
    }

    private fun buildChromeFacade(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileSelection?.onReceiveValue(null)
                fileSelection = callback

                val acceptTypes = params?.acceptTypes ?: arrayOf("*/*")
                val mimeType = acceptTypes.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "*/*"

                val picker = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mimeType
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                return try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(picker, FILE_GATEWAY_CODE)
                    true
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this@BrowserActivity, "File manager not found", Toast.LENGTH_SHORT).show()
                    callback?.onReceiveValue(null)
                    fileSelection = null
                    false
                }
            }
        }
    }

    private fun beginNavigation(href: String) {
        browserView.loadUrl(href)
    }

    private fun bindBackStack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (browserView.canGoBack()) {
                    browserView.goBack()
                }
            }
        })
    }

    private fun requestDownload(href: String, agent: String, disposition: String, mime: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = PendingDownload(href, agent, disposition, mime)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), DOWNLOAD_PERMISSION_CODE)
            return
        }
        enqueueDownload(href, agent, disposition, mime)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == DOWNLOAD_PERMISSION_CODE) {
            val pending = pendingDownload
            pendingDownload = null
            if (pending != null && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                enqueueDownload(pending.href, pending.agent, pending.disposition, pending.mime)
            } else {
                Toast.makeText(this, "Storage permission is needed to save this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enqueueDownload(
        downloadUrl: String,
        agent: String,
        disposition: String,
        mimeType: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
            request.setMimeType(mimeType)

            val cookies = CookieManager.getInstance().getCookie(downloadUrl)
            if (!cookies.isNullOrEmpty()) {
                request.addRequestHeader("Cookie", cookies)
            }
            request.addRequestHeader("User-Agent", agent)

            val fileName = URLUtil.guessFileName(downloadUrl, disposition, mimeType)
            request.setTitle(fileName)
            request.setDescription("Downloading file...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Download started: $fileName", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                android.util.Log.e("CafeBrowser", "Download request failed", error)
            }
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternalTarget(context: Context, intent: Intent): Boolean {
        return try {
            if (context !is ComponentActivity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveChrome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_GATEWAY_CODE) {
            absorbPickerResult(resultCode, data)
        }
    }

    private fun absorbPickerResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val uris = mutableListOf<Uri>()

            data?.data?.let { uri -> uris.add(uri) }

            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri -> uris.add(uri) }
                }
            }

            fileSelection?.onReceiveValue(uris.toTypedArray())
        } else {
            fileSelection?.onReceiveValue(null)
        }

        fileSelection = null
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
