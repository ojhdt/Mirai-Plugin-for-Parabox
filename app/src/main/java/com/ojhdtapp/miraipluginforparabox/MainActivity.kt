package com.ojhdtapp.miraipluginforparabox

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ojhdtapp.miraipluginforparabox.core.MIRAI_CORE_VERSION
import com.ojhdtapp.miraipluginforparabox.core.util.*
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.miraipluginforparabox.ui.NavGraphs
import com.ojhdtapp.miraipluginforparabox.ui.destinations.StatusPageDestination
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageEvent
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageViewModel
import com.ojhdtapp.miraipluginforparabox.ui.theme.AppTheme
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxActivity
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ParaboxActivity<ConnService>(ConnService::class.java) {

    private lateinit var notificationUtil: NotificationUtilForActivity
    private lateinit var permissionRequester: ActivityResultLauncher<Array<String>>
    private val viewModel: StatusPageViewModel by viewModels()

    private val batteryUtil: BatteryUtil by lazy {
        BatteryUtil(this)
    }

    override fun onParaboxServiceConnected() {
        getState()
    }

    override fun onParaboxServiceDisconnected() {
    }

    override fun onParaboxServiceStateChanged(state: Int, message: String?) {
        Log.d("parabox", "received update state: $state")
        val serviceState = when (state) {
            ParaboxKey.STATE_ERROR -> ServiceStatus.Error(message)
            ParaboxKey.STATE_LOADING -> ServiceStatus.Loading(message)
            ParaboxKey.STATE_PAUSE -> ServiceStatus.Pause(message)
            ParaboxKey.STATE_STOP -> ServiceStatus.Stop
            ParaboxKey.STATE_RUNNING -> ServiceStatus.Running(message)

            else -> ServiceStatus.Stop
        }
        viewModel.updateServiceStatusStateFlow(serviceState)
        when (state) {
            ParaboxKey.STATE_ERROR, ParaboxKey.STATE_STOP, ParaboxKey.STATE_RUNNING -> {
                viewModel.updateLoginResourceStateFlow(LoginResource.None)
            }
        }
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        val obj = msg.obj as Bundle
        when (msg.what) {
            ConnService.REQUEST_SOLVE_PIC_CAPTCHA -> {
                val bm = obj.getParcelable<Bitmap>("bitmap")
                if (bm == null) {
                    sendRequestResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                    )
                } else {
                    viewModel.updateLoginResourceStateFlow(LoginResource.PicCaptcha(bm, metadata))
                }
            }

            ConnService.REQUEST_SOLVE_SLIDER_CAPTCHA -> {
                val url = obj.getString("url")
                if (url == null) {
                    sendRequestResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                    )
                } else {
                    viewModel.updateLoginResourceStateFlow(
                        LoginResource.SliderCaptcha(
                            url,
                            metadata
                        )
                    )
                }
            }

            ConnService.REQUEST_SOLVE_UNSAFE_DEVICE_LOGIN_VERIFY -> {
                val url = obj.getString("url")
                if (url == null) {
                    sendRequestResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                    )
                } else {
                    viewModel.updateLoginResourceStateFlow(
                        LoginResource.UnsafeDeviceLoginVerify(
                            url,
                            metadata
                        )
                    )
                }
            }

            ConnService.REQUEST_SOLVE_DEVICE_VERIFICATION_SMS -> {
                val number = obj.getString("number")
                viewModel.updateLoginResourceStateFlow(
                    LoginResource.Sms(number ?: "null", metadata)
                )
            }

            ConnService.REQUEST_SOLVE_DEVICE_VERIFICATION_FALLBACK -> {
                val url = obj.getString("url")
                if (url == null) {
                    sendRequestResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                    )
                } else {
                    viewModel.updateLoginResourceStateFlow(
                        LoginResource.Fallback(url, metadata)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationUtil = NotificationUtilForActivity(this)
        permissionRequester = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.all { it.value }) {
                // all permission granted
                Log.d("parabox", "all permission granted")
            } else {
                // permission denied
                Log.d("parabox", "permission denied")
                finish()
            }
        }
        checkMainAppInstallation()
//        checkMainAppOnBackStack()
        // request notification permission
        requestNotificationPermission()

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
                        enterTransition = { slideInHorizontally { 100 } + fadeIn() },
                        exitTransition = { slideOutHorizontally { -100 } + fadeOut() },
                        popEnterTransition = { slideInHorizontally { -100 } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { 100 } + fadeOut() }
//                        enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 0.9f) },
//                        exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 1.1f) },
//                        popEnterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 1.1f) },
//                        popExitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 0.9f) }
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

    override fun onStart() {
        super.onStart()
        // bind Service
        bindParaboxService()
    }

    override fun onStop() {
        super.onStop()
        // unbind service
        unbindParaboxService()
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

            is StatusPageEvent.OnServiceLogin -> {
                serviceLogin()
            }

            is StatusPageEvent.OnRequestIgnoreBatteryOptimizations -> {
                batteryUtil.ignoreBatteryOptimization()
            }

            is StatusPageEvent.OnLoginResourceConfirm -> {
                sendRequestResponse(
                    isSuccess = true,
                    metadata = event.metadata,
                    extra = Bundle().apply {
                        putString("result", event.res)
                    }
                )
            }

            is StatusPageEvent.OnDeviceVerificationSmsConfirm -> {
                sendRequestResponse(
                    isSuccess = true,
                    metadata = event.metadata,
                    extra = Bundle().apply {
                        putString("result", event.res)
                    }
                )
            }

            is StatusPageEvent.OnDeviceVerificationFallbackConfirm -> {
                sendRequestResponse(
                    isSuccess = true,
                    metadata = event.metadata
                )
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

    private fun serviceStart() {
        viewModel.updateServiceStatusStateFlow(ServiceStatus.Running("正在建立与服务的连接"))
        notificationUtil.cancelAll()
        sendCommand(
            command = ParaboxKey.COMMAND_START_SERVICE,
            onResult = {
                if (it is ParaboxResult.Fail) {
                    val errorMessage = when (it.errorCode) {
                        ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> "资源丢失"
                        ParaboxKey.ERROR_DISCONNECTED -> "与服务的连接断开"
                        ParaboxKey.ERROR_REPEATED_CALL -> "重复正在执行的操作"
                        ParaboxKey.ERROR_TIMEOUT -> "操作超时"
                        else -> "未知错误"
                    }
                    viewModel.updateServiceStatusStateFlow(ServiceStatus.Error(errorMessage))
                    notificationUtil.sendNotification("执行指令时发生错误", errorMessage)
                }
            }
        )
    }

    private fun serviceStop() {
        sendCommand(ParaboxKey.COMMAND_STOP_SERVICE,
            onResult = {
                if (it is ParaboxResult.Fail) {
                    val errorMessage = when (it.errorCode) {
                        ParaboxKey.ERROR_DISCONNECTED -> "与服务的连接断开"
                        ParaboxKey.ERROR_REPEATED_CALL -> "重复正在执行的操作"
                        ParaboxKey.ERROR_TIMEOUT -> "操作超时"
                        else -> "未知错误"
                    }
                    viewModel.updateServiceStatusStateFlow(ServiceStatus.Error(errorMessage))
                    viewModel.updateLoginResourceStateFlow(LoginResource.None)
                    notificationUtil.sendNotification("执行指令时发生错误", errorMessage)
                }
            })
    }

    private fun serviceForceStop() {
        sendCommand(ParaboxKey.COMMAND_FORCE_STOP_SERVICE,
            onResult = {
                if (it is ParaboxResult.Fail) {
                    val errorMessage = when (it.errorCode) {
                        ParaboxKey.ERROR_DISCONNECTED -> "与服务的连接断开"
                        ParaboxKey.ERROR_REPEATED_CALL -> "重复正在执行的操作"
                        ParaboxKey.ERROR_TIMEOUT -> "操作超时"
                        else -> "未知错误"
                    }
                    viewModel.updateServiceStatusStateFlow(ServiceStatus.Error(errorMessage))
                    viewModel.updateLoginResourceStateFlow(LoginResource.None)
                    notificationUtil.sendNotification("执行指令时发生错误", errorMessage)
                }
            })

//        viewModel.updateLoginResourceStateFlow(LoginResource.None)
//        viewModel.updateServiceStatusStateFlow(ServiceStatus.Stop)
//        viewModel.setMainSwitchEnabledState(true)
//        viewModel.setMainSwitchState(false)
    }

    private fun serviceLogin(){
        sendCommand(
            command = ConnService.COMMAND_SERVICE_LOGIN,
            onResult = {
                if (it is ParaboxResult.Fail) {
                    val errorMessage = when (it.errorCode) {
                        ParaboxKey.ERROR_DISCONNECTED -> "与服务的连接断开"
                        ParaboxKey.ERROR_REPEATED_CALL -> "重复正在执行的操作"
                        ParaboxKey.ERROR_TIMEOUT -> "操作超时"
                        else -> "未知错误"
                    }
                    viewModel.updateServiceStatusStateFlow(ServiceStatus.Error(errorMessage))
                    notificationUtil.sendNotification("执行指令时发生错误", errorMessage)
                }
            }
        )
    }

    private fun requestNotificationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionRequester.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
        }
    }
}