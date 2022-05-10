package com.ojhdtapp.miraipluginforparabox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ojhdtapp.miraipluginforparabox.core.util.BrowserUtil
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPage
import com.ojhdtapp.miraipluginforparabox.ui.theme.MiraiPluginForParaboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiraiPluginForParaboxTheme {
                // A surface container using the 'background' color from the theme
                StatusPage(
                    onLoginBtnCLicked = { accountNum, passwd ->
                        startService(Intent(this, ConnService::class.java).apply {
                            putExtra("data", Bundle().apply {
                                putLong("accountNum", accountNum)
                                putString("passwd", passwd)
                            })
                        })
                    },
                    onKillBtnCLicked = { ConnService.stop() },
                    onSolvingLoginClick = ::initUnsafeLoginWebView
                )
            }
        }
    }

    fun initUnsafeLoginWebView(url: String) {
        BrowserUtil.launchURL(this, url.replace("verify", "qrcode"))
    }
}