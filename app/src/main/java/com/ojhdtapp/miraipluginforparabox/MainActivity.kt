package com.ojhdtapp.miraipluginforparabox

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.core.util.BrowserUtil
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import com.ojhdtapp.miraipluginforparabox.domain.service.ServiceConnector
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPage
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageEvent
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageViewModel
import com.ojhdtapp.miraipluginforparabox.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var connector: ServiceConnector
    var serviceStartJob: Job? = null
    private val viewModel: StatusPageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connector = ServiceConnector(this, viewModel)
        setContent {
            AppTheme() {
                // A surface container using the 'background' color from the theme
                StatusPage(onEvent = { onEvent(it) }
                )
            }
        }
    }

    private fun onEvent(event: StatusPageEvent) {
        when (event) {
            is StatusPageEvent.OnServiceStart -> {
                serviceStart()
            }

            is StatusPageEvent.OnLoginClick -> {

            }
            is StatusPageEvent.OnKillClick -> {

            }
            is StatusPageEvent.OnPicCaptchaConfirm -> {
                event.captcha?.let {
                    connector.submitVerificationResult(it)
                }
            }
            is StatusPageEvent.OnSliderCaptchaConfirm -> {
                connector.submitVerificationResult(event.ticket)
            }
            is StatusPageEvent.OnUnsafeDeviceLoginVerifyConfirm -> {
                connector.submitVerificationResult("success")
            }
            is StatusPageEvent.OnShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun serviceStart() {
        fun cancel() {
            viewModel.setMainSwitchState(false)
            viewModel.setMainSwitchEnabledState(true)
            serviceStartJob?.cancel()
        }
        viewModel.setMainSwitchEnabledState(false)
        serviceStartJob = lifecycleScope.launch {
            connector.startAndBind()
            viewModel.updateServiceStatusStateFlow(
                connector.miraiStart().also {
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) cancel()
                }
            )
            viewModel.updateServiceStatusStateFlow(
                connector.miraiLogin().also {
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) cancel()
                    else if(it is ServiceStatus.Pause) {
                        TODO()
                    }
                }
            )
            viewModel.setMainSwitchState(true)
            viewModel.setMainSwitchEnabledState(false)
        }
    }

    private fun serviceStop() {

    }

}