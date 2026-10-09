package de.parip69.barcodeaudiscanner

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
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
import androidx.core.content.ContextCompat
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

    private val apkUpdateRunning = java.util.concurrent.atomic.AtomicBoolean(false)
    private var pendingUpdateApk: java.io.File? = null
    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val apk = pendingUpdateApk
        if (apk != null && (Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls())) {
            launchApkInstaller(apk)
        } else {
            android.widget.Toast.makeText(this, "Zum Aktualisieren bitte die Installation für diese App erlauben und erneut auf Update drücken.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun launchApkInstaller(apk: java.io.File) {
        pendingUpdateApk = apk
        try {
            if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                installPermissionLauncher.launch(android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                ))
                return
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.provider", apk)
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Installation konnte nicht geöffnet werden: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Native Schnittstelle fuer Download/Senden/Export.
    inner class AndroidInterface {
        @android.webkit.JavascriptInterface
        fun installAppUpdate(version: String): Boolean {
            if (!version.matches(Regex("[1-9][0-9]{0,8}"))) return false
            if (!apkUpdateRunning.compareAndSet(false, true)) return false
            Thread {
                var downloaded: java.io.File? = null
                try {
                    val url = java.net.URL("https://raw.githubusercontent.com/parip69/BarcodeAudi_AndroidAPK/main/Privat/BarcodeAudiScanner_ver${version}.apk")
                    val connection = url.openConnection() as javax.net.ssl.HttpsURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    try {
                        check(connection.responseCode == 200) { "Download: HTTP ${connection.responseCode}" }
                        val apk = java.io.File.createTempFile("barcode-update-", ".apk", cacheDir)
                        downloaded = apk
                        connection.inputStream.use { input ->
                            apk.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0L
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    check(total <= 100L * 1024 * 1024) { "APK ist zu groß." }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        @Suppress("DEPRECATION")
                        val info = packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
                        check(info?.packageName == packageName && info.versionName == version) { "APK gehört nicht zu dieser App-Version." }
                        runOnUiThread { launchApkInstaller(apk) }
                    } finally {
                        connection.disconnect()
                    }
                } catch (e: Exception) {
                    downloaded?.delete()
                    runOnUiThread {
                        android.widget.Toast.makeText(this@MainActivity, "Update fehlgeschlagen: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                } finally {
                    apkUpdateRunning.set(false)
                }
            }.start()
            return true
        }

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
        fun getPackageName(): String {
            return this@MainActivity.packageName
        }

        @android.webkit.JavascriptInterface
        fun getAppId(): String {
            return this@MainActivity.packageName
        }

        @android.webkit.JavascriptInterface
        fun setSystemThemeMode(mode: String?) {
            runOnUiThread {
                applyAppChromeForTheme(mode)
            }
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

        @android.webkit.JavascriptInterface
        fun shareAppLink(text: String, url: String) {
            runOnUiThread {
                try {
                    val parsed = android.net.Uri.parse(url)
                    require(parsed.scheme == "https" && parsed.host == "parip69.github.io") {
                        "Ungültiger App-Link."
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Audi Barcode-Scanner")
                        putExtra(android.content.Intent.EXTRA_TEXT, "${url.trim()}\n${text.trim()}")
                    }
                    startActivity(android.content.Intent.createChooser(intent, "App-Link teilen"))
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Fehler beim Teilen des App-Links: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun shareAppCard(text: String, url: String) {
            try {
                val parsed = android.net.Uri.parse(url)
                require(parsed.scheme == "https" && parsed.host == "parip69.github.io") {
                    "Ungültiger App-Link."
                }
                val image = assets.open("icons/share-card.png").use { it.readBytes() }
                shareQrCode(
                    "Audi-Barcode-Scanner-Kaertchen.png",
                    "data:image/png;base64," + Base64.encodeToString(image, Base64.NO_WRAP),
                    text,
                    url
                )
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "App-Kärtchen konnte nicht geteilt werden: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun shareQrCode(fileName: String, dataUrl: String, text: String, url: String) {
            try {
                val encodedImage = dataUrl.substringAfter("base64,", "")
                require(encodedImage.isNotEmpty()) { "Ungültige Bilddaten." }
                val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                val shareDirectory = java.io.File(cacheDir, "shared_qr_codes").apply { mkdirs() }
                val safeFileName = fileName
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "Audi-Barcode-Scanner-QR.png" }
                val imageFile = java.io.File(shareDirectory, safeFileName)
                imageFile.writeBytes(imageBytes)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.provider",
                    imageFile
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Audi Barcode-Scanner")
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        listOf(text.trim(), url.trim()).filter { it.isNotEmpty() }.joinToString("\n")
                    )
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runOnUiThread {
                    try {
                        startActivity(android.content.Intent.createChooser(intent, "App-Kärtchen teilen"))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this@MainActivity, "Teilen nicht möglich: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Fehler beim Teilen des Bildes: ${e.message}",
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
    private var currentThemeMode = "light"

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
        val useDarkIcons = currentThemeMode == "light"
        controller.isAppearanceLightStatusBars = useDarkIcons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = useDarkIcons
        }
    }

    private fun normalizeThemeMode(mode: String?): String {
        return if (mode?.trim()?.equals("dark", ignoreCase = true) == true) "dark" else "light"
    }

    private fun resolveChromeBackgroundColor(mode: String): Int {
        return ContextCompat.getColor(
            this,
            if (mode == "dark") R.color.app_background_dark else R.color.app_background_light
        )
    }

    private fun applyAppChromeForTheme(mode: String?) {
        val effectiveMode = normalizeThemeMode(mode)
        currentThemeMode = effectiveMode
        val backgroundColor = resolveChromeBackgroundColor(effectiveMode)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.setBackgroundDrawable(ColorDrawable(backgroundColor))
        window.decorView.setBackgroundColor(backgroundColor)

        if (::binding.isInitialized) {
            binding.root.setBackgroundColor(backgroundColor)
            binding.swipeRefresh.setBackgroundColor(backgroundColor)
            binding.webView.setBackgroundColor(backgroundColor)
        }

        scheduleImmersiveFullscreen()
    }

    private fun syncThemeFromWebView() {
        if (!::binding.isInitialized) return
        binding.webView.evaluateJavascript(
            """
            (function() {
                try {
                    if (typeof getStoredThemeMode === "function") {
                        return getStoredThemeMode();
                    }
                    if (document.body && document.body.classList.contains("theme-dark")) {
                        return "dark";
                    }
                } catch (e) {}
                return "light";
            })();
            """.trimIndent()
        ) { value ->
            applyAppChromeForTheme(value?.trim('"'))
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
    private fun loadSharedAppIntent(incoming: android.content.Intent?) {
        val uri = incoming?.data
        val shared = if (uri?.scheme == "barcodeaudi" && uri.host == "import") uri.getQueryParameter("share") else null
        val suffix = if (!shared.isNullOrBlank()) "?share=${Uri.encode(shared)}" else ""
        binding.webView.loadUrl("file:///android_asset/index.html$suffix")
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadSharedAppIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        configureImmersiveWindow()
        applyAppChromeForTheme("light")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding.root.postDelayed(initialOrientationReleaseRunnable, 400)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, _ ->
            scheduleImmersiveFullscreen()
            WindowInsetsCompat.CONSUMED
        }
        scheduleImmersiveFullscreen()

        configureWebView(binding.webView)
        loadSharedAppIntent(intent)

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
        webView.overScrollMode = View.OVER_SCROLL_NEVER
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
                syncThemeFromWebView()
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
