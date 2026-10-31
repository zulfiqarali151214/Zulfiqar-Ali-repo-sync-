package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
  private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
  private var activeWebView: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    filePickerLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
          val data = result.data!!
          val uris = ArrayList<Uri>()
          val clipData = data.clipData
          if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
              uris.add(clipData.getItemAt(i).uri)
            }
          } else if (data.data != null) {
            uris.add(data.data!!)
          }
          fileUploadCallback?.onReceiveValue(uris.toTypedArray())
        } else {
          fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
      }

    setContent {
      MyApplicationTheme {
        RepoSyncMainScreen(
          onRegisterWebView = { webView ->
            activeWebView = webView
            configureWebView(webView)
          },
          onReload = {
            activeWebView?.reload()
          }
        )
      }
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun configureWebView(webView: WebView) {
    webView.settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      allowFileAccess = true
      allowContentAccess = true
      databaseEnabled = true
      useWideViewPort = true
      loadWithOverviewMode = true
      cacheMode = WebSettings.LOAD_DEFAULT
      mediaPlaybackRequiresUserGesture = false
    }

    webView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url != null && (url.startsWith("https://github.com/") || url.startsWith("http://") || url.startsWith("https://"))) {
          try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            return true
          } catch (_: Exception) {
          }
        }
        return false
      }
    }

    webView.webChromeClient = object : WebChromeClient() {
      override fun onShowFileChooser(
        view: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
      ): Boolean {
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = filePathCallback

        val intent = try {
          fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
          }
        } catch (_: Exception) {
          Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
          }
        }

        return try {
          filePickerLauncher.launch(intent)
          true
        } catch (_: Exception) {
          fileUploadCallback?.onReceiveValue(null)
          fileUploadCallback = null
          false
        }
      }
    }

    webView.loadUrl("file:///android_asset/index.html")
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSyncMainScreen(
  onRegisterWebView: (WebView) -> Unit,
  onReload: () -> Unit
) {
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  BackHandler(enabled = webViewRef?.canGoBack() == true) {
    webViewRef?.goBack()
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Zulfiqar Repo Sync",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primary,
          titleContentColor = MaterialTheme.colorScheme.onPrimary,
          actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = {
          IconButton(
            onClick = onReload,
            modifier = Modifier.testTag("reload_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Reload App"
            )
          }
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            onRegisterWebView(this)
            webViewRef = this
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("repo_sync_webview")
      )
    }
  }
}
