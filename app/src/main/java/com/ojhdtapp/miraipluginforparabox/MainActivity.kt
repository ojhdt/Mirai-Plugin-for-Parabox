package com.ojhdtapp.miraipluginforparabox

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.miraipluginforparabox.core.util.*
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.NavGraphs
import com.ojhdtapp.miraipluginforparabox.ui.destinations.StatusPageDestination
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageEvent
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageViewModel
import com.ojhdtapp.miraipluginforparabox.ui.theme.AppTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var notificationUtil: NotificationUtilForActivity
    var serviceStartJob: Job? = null
    private val viewModel: StatusPageViewModel by viewModels()
    private var listeningDeferred: CompletableDeferredWithTag<Long, String>? = null

    private val batteryUtil: BatteryUtil by lazy {
        BatteryUtil(this)
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationUtil = NotificationUtilForActivity(this)

        checkMainAppInstallation()
//        checkMainAppOnBackStack()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = !isSystemInDarkTheme()
            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = useDarkIcons
                )
            }
            AppTheme {
                val navHostEngine = rememberAnimatedNavHostEngine(
                    navHostContentAlignment = Alignment.TopCenter,
                    rootDefaultAnimations = RootNavGraphDefaultAnimations(
//                    enterTransition = { slideInHorizontally { it }},
//                    exitTransition = { slideOutHorizontally { -it }},
//                    popEnterTransition = { slideInHorizontally { -it }},
//                    popExitTransition = { slideOutHorizontally { it }},
                        enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 0.9f) },
                        exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 1.1f) },
                        popEnterTransition = { fadeIn(tween(450)) + scaleIn(tween(450), 1.1f) },
                        popExitTransition = { fadeOut(tween(450)) + scaleOut(tween(450), 0.9f) }
                    ),
                )
                DestinationsNavHost(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    navGraph = NavGraphs.root,
                    engine = navHostEngine,
                    dependenciesContainerBuilder = {
                        dependency { event: StatusPageEvent -> onEvent(event) }
                        when (destination) {
                            is StatusPageDestination -> {
                                dependency(viewModel)
                            }
                        }
                    }) {

                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkMainAppInstallation()
//        checkMainAppOnBackStack()
    }

    private fun checkMainAppInstallation() {
        val pkg = "com.ojhdtapp.parabox"
        var res = false
        try {
            packageManager.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            res = true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        viewModel.setIsMainAppInstalled(res)
    }

    private fun checkMainAppOnBackStack() {
        val pkg = "com.ojhdtapp.parabox"
        val res = packageManager.getLaunchIntentForPackage(pkg)
            ?.resolveActivityInfo(packageManager, PackageManager.GET_META_DATA) != null
        viewModel.setIsMainAppOnStack(!isTaskRoot)
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
            is StatusPageEvent.OnRequestIgnoreBatteryOptimizations -> {
                batteryUtil.ignoreBatteryOptimization()
            }
            is StatusPageEvent.OnLoginResourceConfirm -> {
                Log.d(
                    "parabox",
                    "deferred:${listeningDeferred?.getCurrentTag()} now:${event.timestamp}"
                )
                listeningDeferred?.complete(event.timestamp, event.res)
            }
            is StatusPageEvent.OnLaunchBrowser -> {
                BrowserUtil.launchURL(this, event.url)
            }
            is StatusPageEvent.OnShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is StatusPageEvent.LaunchMainApp -> {
                val pkg = "com.ojhdtapp.parabox"
                packageManager.getLaunchIntentForPackage(pkg)?.let {
                    startActivity(it.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            }
        }
    }

    private fun errorOccurred(status: ServiceStatus? = null) {
        status?.let {
            if (it is ServiceStatus.Error) {
                notificationUtil.sendNotification("启动服务时发生错误", it.message)
            }
        }
        listeningDeferred?.cancel()
        serviceStartJob?.cancel()
        viewModel.setMainSwitchEnabledState(true)
        viewModel.setMainSwitchState(false)
        viewModel.updateLoginResourceStateFlow(LoginResource.None)
        try {
            lifecycleScope.launch {
                connector.miraiStop()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        connector.initializeAllState()
        Log.d("parabox", "cancel")
    }

    private fun serviceStart() {
        viewModel.setMainSwitchEnabledState(false)
        notificationUtil.cancelAll()
        serviceStartJob = lifecycleScope.launch {
            val isTimeOutDisabled =
                dataStore.data.first()[DataStoreKeys.CANCEL_TIMEOUT] ?: false
            try {
                withTimeout(if (isTimeOutDisabled) 1800000 else 1000) {
                    connector.startAndBind()
                }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                        errorOccurred(it)
                        return@launch
                    }
                }
                withTimeout(if (isTimeOutDisabled) 1800000 else 1000) {
                    connector.miraiStart()
                }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                        errorOccurred(it)
                        return@launch
                    }
                }
                withTimeout(if (isTimeOutDisabled) 1800000 else 30000) { connector.miraiLogin() }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                    if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                        errorOccurred(it)
                        return@launch
                    }
                    var temp = it
                    while (temp is ServiceStatus.Pause) {
                        val newTimestamp = temp.timestamp
                        listeningDeferred = CompletableDeferredWithTag(newTimestamp)
                        withTimeout(if (isTimeOutDisabled) 1800000 else 60000) {
                            connector.submitVerificationResult(
                                listeningDeferred!!.await()
                            )
                        }.also {
                            viewModel.updateServiceStatusStateFlow(it)
                            if (it is ServiceStatus.Error || it is ServiceStatus.Stop) {
                                errorOccurred(it)
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
                viewModel.setMainSwitchState(true)
                viewModel.setMainSwitchEnabledState(true)
            } catch (e: TimeoutCancellationException) {
                val status = ServiceStatus.Error("操作超时")
                viewModel.updateServiceStatusStateFlow(status)
                errorOccurred(status)
                return@launch
            }
        }
    }

    private fun serviceStop() {
        viewModel.setMainSwitchEnabledState(false)
        lifecycleScope.launch {
            val isTimeOutDisabled =
                dataStore.data.first()[DataStoreKeys.CANCEL_TIMEOUT] ?: false
            try {
                withTimeout(if (isTimeOutDisabled) 1800000 else 1000) { connector.miraiStop() }.also {
                    viewModel.updateServiceStatusStateFlow(it)
                }
            } catch (e: TimeoutCancellationException) {
                viewModel.updateServiceStatusStateFlow(ServiceStatus.Error("服务响应超时，请手动检查服务状态"))
                notificationUtil.sendNotification("与服务通信时发生错误", "服务响应超时，请手动检查服务状态")
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

    companion object{
        init {
            System.loadLibrary("silkcodec")
        }
    }

}