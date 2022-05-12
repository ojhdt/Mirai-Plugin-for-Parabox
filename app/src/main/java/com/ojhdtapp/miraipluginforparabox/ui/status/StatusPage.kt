package com.ojhdtapp.miraipluginforparabox.ui.status

import android.widget.EditText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource

@Composable
fun StatusPage(
    modifier: Modifier = Modifier,
    onLoginBtnCLicked: (accountNum: Long, passwd: String) -> Unit,
    onKillBtnCLicked: () -> Unit,
    onLogBot: () -> Unit,
) {
    val viewModel: StatusPageViewModel = hiltViewModel()
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                else -> {
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val loginResource by viewModel.loginResourceStateFlow.collectAsState()

        Button(onClick = { onLoginBtnCLicked("2371065280".toLong(), "b20011007") }) {
            Text(text = "Login")
        }
        Button(onClick = { onKillBtnCLicked() }) {
            Text(text = "Kill")
        }
        when (loginResource) {
            is LoginResource.None -> {
            }
            is LoginResource.PicCaptcha -> {
                Row() {
                    TextField(
                        modifier = modifier.weight(1f),
                        value = viewModel.loginTextState.value,
                        onValueChange = viewModel::setLoginTextState
                    )
                    Image(
                        bitmap = (loginResource as LoginResource.PicCaptcha).captchaBitMap.asImageBitmap(),
                        contentDescription = "PicCaptcha"
                    )
                    Button(onClick = {

                    }) {
                        Text(text = "Confirm")
                    }
                }
            }
            is LoginResource.UnsafeDeviceLoginVerify -> {
                UnSafeWebView(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black),
                    url = (loginResource as LoginResource.UnsafeDeviceLoginVerify).url
                )
            }
            is LoginResource.SliderCaptcha -> {
                UnSafeWebView(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black),
                    url = (loginResource as LoginResource.SliderCaptcha).url
                )
            }
        }
        Button(onClick = { onLogBot() }) {
            Text(text = "Log")
        }
    }
}