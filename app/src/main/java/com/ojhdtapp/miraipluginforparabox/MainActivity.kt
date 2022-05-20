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
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPage
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageEvent
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageViewModel
import com.ojhdtapp.miraipluginforparabox.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

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
            is StatusPageEvent.OnServiceStop -> {
                serviceStop()
            }
            is StatusPageEvent.OnServiceForceStop -> {
                serviceForceStop()
            }

//            is StatusPageEvent.OnLoginClick -> {
//
//            }
//            is StatusPageEvent.OnKillClick -> {
//
//            }
            is StatusPageEvent.OnLoginResourceConfirm -> {
                Log.d(
                    "parabox",
                    "deferred:${listeningDeferred?.getCurrentTag()} now:${event.timestamp}"
                )
                listeningDeferred?.complete(event.timestamp, event.res)
            }
            is StatusPageEvent.OnShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun errorOccurred() {
        listeningDeferred?.cancel()
        serviceStartJob?.cancel()
        viewModel.setMainSwitchEnabledState(true)
        viewModel.setMainSwitchState(false)
        viewModel.updateLoginResourceStateFlow(LoginResource.None)
        Log.d("parabox", "cancel")
    }

    private fun serviceStart() {
        viewModel.setMainSwitchEnabledState(false)
        serviceStartJob = lifecycleScope.launch {
            try {
                withTimeout(1000) {
                    connector.startAndBind()
                }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                        errorOccurred()
                        return@launch
                    }
                }
                withTimeout(1000) {
                    connector.miraiStart()
                }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                        errorOccurred()
                        return@launch
                    }
                }
                withTimeout(30000) { connector.miraiLogin() }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                        errorOccurred()
                        return@launch
                    }
                    var temp = it
                    while (temp is ServiceStatus.Pause) {
                        val newTimestamp = temp.timestamp
                        listeningDeferred = CompletableDeferredWithTag(newTimestamp)
                        withTimeout(60000) { connector.submitVerificationResult(listeningDeferred!!.await()) }.also {
                            viewModel.updateServiceStatusStateFlow(it)
                            if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                                errorOccurred()
                                return@launch
                            } else if (it is ServiceStatus.Pause) temp = it
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
            } catch (e: TimeoutCancellationException) {
                viewModel.updateServiceStatusStateFlow(ServiceStatus.Error("操作超时"))
                errorOccurred()
                return@launch
            }
            Log.d("parabox", "end")
            viewModel.setMainSwitchState(true)
            viewModel.setMainSwitchEnabledState(true)
        }
    }

    private fun serviceStop() {
        viewModel.setMainSwitchEnabledState(false)
        lifecycleScope.launch {
            try {
                withTimeout(1000) { connector.miraiStop() }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                }
            } catch (e: TimeoutCancellationException) {
                viewModel.updateServiceStatusStateFlow(ServiceStatus.Error("服务响应超时，请手动检查服务状态"))
            } finally {
                listeningDeferred?.cancel()
                serviceStartJob?.cancel()
                viewModel.setMainSwitchEnabledState(true)
                viewModel.setMainSwitchState(false)
            }
        }
    }

    private fun serviceForceStop() {
        connector.miraiForceStop()
        listeningDeferred?.cancel()
        serviceStartJob?.cancel()
        viewModel.updateLoginResourceStateFlow(LoginResource.None)
        viewModel.updateServiceStatusStateFlow(ServiceStatus.Stop)
        viewModel.setMainSwitchEnabledState(true)
        viewModel.setMainSwitchState(false)
    }
}