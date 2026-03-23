package de.parip69.barcodeaudiscanner

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.MimeTypeMap
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.parip69.barcodeaudiscanner.databinding.ActivityMainBinding
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {
    // Native Schnittstelle für Download/Senden
    inner class AndroidInterface {
        @android.webkit.JavascriptInterface
        fun saveTextFile(fileName: String, content: String) {
            try {
                val context = this@MainActivity
                val resolver = context.contentResolver
                val mimeType = "text/plain"
                val fileDisplayName = fileName
                val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                val uri = if (isQ) {
                    // Android 10+ (Q): MediaStore nutzen
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileDisplayName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val collection = android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val itemUri = resolver.insert(collection, values)
                    if (itemUri != null) {
                        resolver.openOutputStream(itemUri)?.use { it.write(content.toByteArray()) }
                        values.clear()
                        values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(itemUri, values, null, null)
                    }
                    itemUri
                } else {
                    // Vor Android 10: Direkt in den öffentlichen Download-Ordner schreiben
                    val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloads, fileDisplayName)
                    file.writeText(content)
                    android.net.Uri.fromFile(file)
                }
                runOnUiThread {
                    android.widget.Toast.makeText(context, "Datei gespeichert: ${uri?.path ?: "unbekannt"}", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(this@MainActivity, "Fehler beim Speichern: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun shareTextFile(fileName: String, content: String) {
            try {
                val downloads = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloads, fileName)
                file.writeText(content)
                val uri = androidx.core.content.FileProvider.getUriForFile(this@MainActivity, "${packageName}.provider", file)
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(android.content.Intent.createChooser(intent, "Teilen/Senden als Datei"))
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(this@MainActivity, "Fehler beim Teilen: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private lateinit var binding: ActivityMainBinding
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        fileUploadCallback?.onReceiveValue(
            if (uri != null) arrayOf(uri) else emptyArray()
        )
        fileUploadCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

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

        // Binde die AndroidInterface für Download/Senden ein
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

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
