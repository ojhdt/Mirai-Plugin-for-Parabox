package com.ojhdtapp.miraipluginforparabox

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.ojhdtapp.miraipluginforparabox.core.util.CompletableDeferredWithTag
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
    private var listeningDeferred: CompletableDeferredWithTag<Long, String>? = null

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

//            is StatusPageEvent.OnLoginClick -> {
//
//            }
//            is StatusPageEvent.OnKillClick -> {
//
//            }
            is StatusPageEvent.OnLoginResourceConfirm -> {
                listeningDeferred?.complete(event.timestamp, event.res)
            }

//            is StatusPageEvent.OnPicCaptchaConfirm -> {
//                event.captcha?.let {
//                    listeningDeferred?.complete(,it)
////                    connector.submitVerificationResult(it)
//                }
//            }
//            is StatusPageEvent.OnSliderCaptchaConfirm -> {
//                listeningDeferred?.complete(,event.ticket)
////                connector.submitVerificationResult(event.ticket)
//            }
//            is StatusPageEvent.OnUnsafeDeviceLoginVerifyConfirm -> {
//                listeningDeferred?.complete(,"success")
////                connector.submitVerificationResult("success")
//            }
            is StatusPageEvent.OnShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun serviceStart() {
        fun cancel() {
            Log.d("parabox", "123")
            viewModel.setMainSwitchEnabledState(true)
            viewModel.setMainSwitchState(false)
            serviceStartJob?.cancel()
        }
        viewModel.setMainSwitchEnabledState(false)
        serviceStartJob = lifecycleScope.launch {
            connector.startAndBind().also {
                viewModel.updateServiceStatusStateFlow(it)
                if (it is ServiceStatus.Error || it is ServiceStatus.Stop) cancel()
            }
            connector.miraiStart().also {
                Log.d("parabox", it.toString())
                viewModel.updateServiceStatusStateFlow(it)
                if (it is ServiceStatus.Error || it is ServiceStatus.Stop) cancel()
            }
            connector.miraiLogin().also {
                viewModel.updateServiceStatusStateFlow(it)
                if (it is ServiceStatus.Error || it is ServiceStatus.Stop) cancel()
                var temp = it
                while (temp is ServiceStatus.Pause) {
                    val newTimestamp = temp.timestamp
                    listeningDeferred = CompletableDeferredWithTag(newTimestamp)
                    connector.submitVerificationResult(listeningDeferred!!.await()).also {
                        viewModel.updateServiceStatusStateFlow(it)
                        if (it is ServiceStatus.Error || it is ServiceStatus.Stop) cancel()
                        else if (it is ServiceStatus.Pause) temp = it
                    }
                }
//                else if (it is ServiceStatus.Pause) {
//                    val timestampFromServer = it.timestamp
//                    listeningDeferred = CompletableDeferredWithTag(timestampFromServer)
//                    connector.submitVerificationResult(listeningDeferred!!.await()).also {
//                        viewModel.updateServiceStatusStateFlow(it)
//                        if (it is ServiceStatus.Error || it is ServiceStatus.Stop) cancel()
//                    }
//                }
            }
            viewModel.setMainSwitchState(true)
            viewModel.setMainSwitchEnabledState(false)
        }
    }

    private fun serviceStop() {

    }

}