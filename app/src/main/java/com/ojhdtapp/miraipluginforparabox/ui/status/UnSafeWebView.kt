package com.ojhdtapp.miraipluginforparabox.ui.status

import android.net.Uri
import android.util.Log
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.U
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.Json
import javax.inject.Inject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UnSafeWebView(modifier: Modifier = Modifier, url: String, onConfirm : (res : String) -> Unit) {
    val viewModel: StatusPageViewModel = hiltViewModel()
    AndroidView(factory = { context ->
        WebView.setWebContentsDebuggingEnabled(true)
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = onJsBridgeInvoke(request.url)

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String
                ): Boolean = onJsBridgeInvoke(Uri.parse(url))

                fun onJsBridgeInvoke(request: Uri): Boolean {
                    if (request.path.equals("/onVerifyCAPTCHA")) {
                        val p = request.getQueryParameter("p") ?: return false
                        val jsData = JsonParser.parseString(p).asJsonObject
                        onConfirm(jsData["ticket"].asString)
                    }
                    return false
                }
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            loadUrl(url)
        }
    },
    modifier = modifier
    )
}



//class Bridge(
//    private val parser: JsonParser,
//    val onFinished: (ticket: String) -> Unit
//) {
//    @JavascriptInterface
//    fun invoke(cls: String?, method: String?, data: String?) {
//        if (data != null) {
//            val obj = parser.fromJson<JsonElement>(data, object : TypeToken<JsonElement>() {}.type)
//            if (method == "onVerifyCAPTCHA") {
//                obj?.let {
//                    onFinished(it.asJsonObject["ticket"].asString)
//                }
//            }
//        }
//    }
//}