package com.ojhdtapp.miraipluginforparabox.ui.status

import android.widget.EditText
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource

@Composable
fun StatusPage(
    modifier: Modifier = Modifier,
    onLoginBtnCLicked: (accountNum: Long, passwd: String) -> Unit,
    onSolvingLoginClick : (url :String) -> Unit,
    onKillBtnCLicked: () -> Unit
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

        Button(onClick = { onLoginBtnCLicked("3560863379".toLong(), "a20011007") }) {
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
                        viewModel.onEvent(StatusPageEvent.OnPicCaptchaConfirm(viewModel.loginTextState.value))
                    }) {
                        Text(text = "Confirm")
                    }
                }
            }
            is LoginResource.UnsafeDeviceLoginVerify-> {
                Button(
                    enabled = (loginResource as LoginResource.UnsafeDeviceLoginVerify).url.isNotEmpty()
                    ,onClick = {
                    onSolvingLoginClick((loginResource as LoginResource.UnsafeDeviceLoginVerify).url)
                }) {
                    Text(text = "Solve")
                }
            }
            is LoginResource.SliderCaptcha-> {
                Button(
                    enabled = (loginResource as LoginResource.SliderCaptcha).url.isNotEmpty()
                    ,onClick = {
                    onSolvingLoginClick((loginResource as LoginResource.SliderCaptcha).url)
                }) {
                    Text(text = "Solve")
                }
            }
        }
    }
}