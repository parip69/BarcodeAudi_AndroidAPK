package de.parip69.barcodeaudiscanner

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.parip69.barcodeaudiscanner.databinding.ActivityMainBinding
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {
    private fun resolveMimeTypeForFileName(fileName: String, fallbackMimeType: String = "text/plain"): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return fallbackMimeType
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: fallbackMimeType
    }

    private fun saveBytesToDownloads(fileName: String, bytes: ByteArray, mimeType: String): Boolean {
        return try {
            val context = this@MainActivity
            val resolver = context.contentResolver
            val fileDisplayName = fileName.trim().ifEmpty { "export.txt" }
            val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            val uri = if (isQ) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileDisplayName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: throw IllegalStateException("Datei konnte im Download-Ordner nicht angelegt werden.")
                resolver.openOutputStream(itemUri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("Ausgabestream fuer den Download konnte nicht geoeffnet werden.")
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                itemUri
            } else {
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                val file = java.io.File(downloads, fileDisplayName)
                file.writeBytes(bytes)
                android.net.Uri.fromFile(file)
            }
            runOnUiThread {
                android.widget.Toast.makeText(
                    context,
                    "Datei gespeichert: ${uri?.path ?: "unbekannt"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            true
        } catch (e: Exception) {
            runOnUiThread {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "Fehler beim Speichern: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            false
        }
    }

    private fun resolveAppDisplayName(): String {
        return applicationInfo.loadLabel(packageManager).toString()
    }

    private fun resolveAppVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName?.takeIf { it.isNotBlank() } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    // Native Schnittstelle fuer Download/Senden/Export.
    inner class AndroidInterface {
        @android.webkit.JavascriptInterface
        fun setBarcodeFullscreenRotationEnabled(enabled: Boolean) {
            runOnUiThread {
                this@MainActivity.setBarcodeFullscreenRotationEnabled(enabled)
            }
        }

        @android.webkit.JavascriptInterface
        fun saveTextFile(fileName: String, content: String): Boolean {
            return saveBytesToDownloads(
                fileName,
                content.toByteArray(Charsets.UTF_8),
                resolveMimeTypeForFileName(fileName)
            )
        }

        @android.webkit.JavascriptInterface
        fun exportBundledIndexHtml(fileName: String): Boolean {
            return try {
                val htmlBytes = assets.open("index.html").use { it.readBytes() }
                saveBytesToDownloads(fileName, htmlBytes, "text/html")
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Fehler beim HTML-Export: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                false
            }
        }

        @android.webkit.JavascriptInterface
        fun getBundledIndexHtml(): String {
            return try {
                assets.open("index.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Exception) {
                ""
            }
        }

        @android.webkit.JavascriptInterface
        fun getAppDisplayName(): String {
            return resolveAppDisplayName()
        }

        @android.webkit.JavascriptInterface
        fun getAppVersionName(): String {
            return resolveAppVersionName()
        }

        @android.webkit.JavascriptInterface
        fun shareTextFile(fileName: String, content: String) {
            try {
                val downloads = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                val file = java.io.File(downloads, fileName)
                file.writeText(content, Charsets.UTF_8)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.provider",
                    file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                intent.type = resolveMimeTypeForFileName(fileName)
                intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(android.content.Intent.createChooser(intent, "Teilen/Senden als Datei"))
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Fehler beim Teilen: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private lateinit var binding: ActivityMainBinding
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private val immersiveModeRunnable = Runnable { applyImmersiveFullscreen() }
    private val initialOrientationReleaseRunnable = Runnable { releaseInitialPortraitLockIfNeeded() }
    private var hasReleasedInitialPortraitLock = false

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        fileUploadCallback?.onReceiveValue(
            if (uri != null) arrayOf(uri) else emptyArray()
        )
        fileUploadCallback = null
    }

    private fun configureImmersiveWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    private fun applyImmersiveFullscreen() {
        val controller = WindowCompat.getInsetsController(window, window.decorView) ?: return
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.isAppearanceLightStatusBars = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = false
        }
    }

    private fun scheduleImmersiveFullscreen() {
        if (!::binding.isInitialized) return
        binding.root.removeCallbacks(immersiveModeRunnable)
        binding.root.post(immersiveModeRunnable)
        binding.root.postDelayed(immersiveModeRunnable, 120)
        binding.root.postDelayed(immersiveModeRunnable, 300)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        configureImmersiveWindow()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding.root.postDelayed(initialOrientationReleaseRunnable, 400)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, _ ->
            scheduleImmersiveFullscreen()
            WindowInsetsCompat.CONSUMED
        }
        binding.webView.setBackgroundColor(Color.TRANSPARENT)
        scheduleImmersiveFullscreen()

        configureWebView(binding.webView)
        binding.webView.loadUrl("file:///android_asset/index.html")

        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setBarcodeFullscreenRotationEnabled(enabled: Boolean) {
        releaseInitialPortraitLockIfNeeded()
    }

    private fun releaseInitialPortraitLockIfNeeded() {
        if (hasReleasedInitialPortraitLock) return
        hasReleasedInitialPortraitLock = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadsImagesAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
        }

        // Binde die AndroidInterface fuer Download/Senden ein.
        webView.addJavascriptInterface(AndroidInterface(), "AndroidInterface")

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(emptyArray())
                fileUploadCallback = filePathCallback
                val acceptTypes = fileChooserParams?.acceptTypes ?: emptyArray()
                val mimeTypes = resolveMimeTypes(acceptTypes)
                try {
                    fileChooserLauncher.launch(mimeTypes)
                } catch (e: Exception) {
                    fileUploadCallback?.onReceiveValue(emptyArray())
                    fileUploadCallback = null
                    return false
                }
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.swipeRefresh.isRefreshing = false
                releaseInitialPortraitLockIfNeeded()
                scheduleImmersiveFullscreen()
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                return when {
                    url.endsWith("manifest.webmanifest") -> assetResponse("manifest.webmanifest", "application/manifest+json")
                    url.endsWith("sw.js") -> assetResponse("sw.js", "application/javascript")
                    url.contains("/icons/") -> {
                        val name = url.substringAfterLast('/')
                        assetResponse("icons/$name", "image/png")
                    }
                    else -> super.shouldInterceptRequest(view, request)
                }
            }
        }
    }

    private fun assetResponse(assetPath: String, mimeType: String): WebResourceResponse? {
        return try {
            val bytes = assets.open(assetPath).readBytes()
            WebResourceResponse(mimeType, "utf-8", ByteArrayInputStream(bytes))
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveMimeTypes(acceptTypes: Array<String>): Array<String> {
        val mimeTypes = mutableSetOf<String>()
        for (type in acceptTypes) {
            val trimmed = type.trim().lowercase()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("/")) {
                mimeTypes.add(trimmed)
            } else {
                val ext = trimmed.removePrefix(".")
                val resolved = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                if (resolved != null) mimeTypes.add(resolved)
                if (ext == "json") mimeTypes.add("text/plain")
            }
        }
        return if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes.toTypedArray()
    }

    override fun onResume() {
        super.onResume()
        scheduleImmersiveFullscreen()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        scheduleImmersiveFullscreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            scheduleImmersiveFullscreen()
        }
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.root.removeCallbacks(immersiveModeRunnable)
            binding.root.removeCallbacks(initialOrientationReleaseRunnable)
        }
        binding.webView.destroy()
        super.onDestroy()
    }
}
