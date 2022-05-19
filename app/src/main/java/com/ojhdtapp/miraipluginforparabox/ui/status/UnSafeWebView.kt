package com.ojhdtapp.miraipluginforparabox.ui.status

import android.util.Log
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.ojhdtapp.miraipluginforparabox.domain.util.GsonParser
import com.ojhdtapp.miraipluginforparabox.domain.util.JsonParser
import javax.inject.Inject

@Composable
fun UnSafeWebView(modifier: Modifier = Modifier, url: String, onConfirm : (res : String) -> Unit) {
    val viewModel: StatusPageViewModel = hiltViewModel()
    AndroidView(factory = { context ->
        WebView.setWebContentsDebuggingEnabled(true)
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        """
                        mqq.invoke = function(a,b,c){ return bridge.invoke(a,b,JSON.stringify(c))}
                        """.trimIndent()
                    ) {}
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    val msg = consoleMessage?.message()
                    // 按下回到qq按钮之后会打印这句话，于是就用这个解决了。。。。
                    if (msg?.startsWith("手Q扫码验证") == true) {
                        onConfirm("success")
                    }
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            addJavascriptInterface(Bridge(GsonParser()) {
                onConfirm(it)
            }, "bridge")
            loadUrl(url)
        }
    })
}

class Bridge(
    private val parser: JsonParser,
    val onFinished: (ticket: String) -> Unit
) {
    @JavascriptInterface
    fun invoke(cls: String?, method: String?, data: String?) {
        if (data != null) {
            val obj = parser.fromJson<JsonElement>(data, object : TypeToken<JsonElement>() {}.type)
            if (method == "onVerifyCAPTCHA") {
                obj?.let {
                    onFinished(it.asJsonObject["ticket"].asString)
                }
            }
        }
    }
}