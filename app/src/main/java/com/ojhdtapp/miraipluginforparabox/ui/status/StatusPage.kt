package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.ojhdtapp.miraipluginforparabox.R
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.destinations.LicensePageDestination
import com.ojhdtapp.miraipluginforparabox.ui.theme.fontSize
import com.ojhdtapp.miraipluginforparabox.ui.util.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun AnimatedVisibilityScope.StatusPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    viewModel: StatusPageViewModel,
    onEvent: (StatusPageEvent) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    // snackBar & navigation
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is StatusPageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    // topBar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colorTransitionFraction = scrollBehavior.state.collapsedFraction
    val appBarContainerColor by rememberUpdatedState(
        lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            FastOutLinearInEasing.transform(colorTransitionFraction)
        )
    )
    // accountDialog
    var openAccountDialog by remember {
        mutableStateOf(false)
    }
    val selectedIndexState by viewModel.selectedIndexFlow.collectAsState(initial = -1)
    AccountDialog(
        onEvent = onEvent,
        isOpen = openAccountDialog,
        onDismissRequest = { openAccountDialog = false },
        accountList = viewModel.accountFLow.collectAsState(
            initial = emptyList()
        ).value,
        initialSelectedIndex = selectedIndexState,
        onAddSecret = viewModel::addNewAccount,
        onDeleteSecret = viewModel::deleteAccount,
        onUpdateSelectedSecret = viewModel::updateAccounts
    )

    // menu
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    // login resource
    val loginResource by viewModel.loginResourceStateFlow.collectAsState()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.top_app_bar_title),
                    )
                },
                modifier = Modifier
                    .background(appBarContainerColor)
                    .statusBarsPadding(),
                actions = {
                    IconButton(onClick = {
                        if (viewModel.serviceStatusStateFlow.value is ServiceStatus.Stop || viewModel.serviceStatusStateFlow.value is ServiceStatus.Error) {
                            openAccountDialog = true
                        } else {
                            onEvent(StatusPageEvent.OnShowToast(context.getString(R.string.account_setting_error_toast)))
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "account"
                        )
                    }
                    Box() {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.force_stop_service)) },
                                onClick = {
                                    onEvent(StatusPageEvent.OnServiceForceStop)
                                    viewModel.emitToUiEventFlow(
                                        StatusPageUiEvent.ShowSnackBar(
                                            context.getString(R.string.service_forcibly_stopped)
                                        )
                                    )
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "stop service"
                                    )
                                })
                        }
                    }
                },
                navigationIcon = {
                    if (viewModel.isMainAppInstalled.value) {
                        IconButton(onClick = { onEvent(StatusPageEvent.LaunchMainApp) }) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "back"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { paddingValues ->

        LazyColumn(
            modifier = modifier,
            contentPadding = paddingValues,
            state = rememberLazyListState()
        ) {
            item(key = "main_switch") {
                MainSwitch(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onEvent = onEvent,
                    checked = viewModel.mainSwitchState.value,
                    onCheckedChange = {
                        viewModel.setMainSwitchState(it)
                        if (it) onEvent(StatusPageEvent.OnServiceStart)
                        else onEvent(StatusPageEvent.OnServiceStop)
                    },
                    enabled = viewModel.mainSwitchEnabledState.value
                )
            }
            item(key = "status_indicator") {
                StatusIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    status = viewModel.serviceStatusStateFlow.collectAsState().value,
                    shouldShowRetryButton = viewModel.shouldShowRetryButtonStateFlow.collectAsState().value,
                    onRetryButtonClick = {
                        viewModel.onRetryButtonClicked()
                        onEvent(StatusPageEvent.OnServiceLogin)
                    }
                )
            }

            item(key = "login_resource") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    when (loginResource) {
                        is LoginResource.None -> {
                        }

                        is LoginResource.PicCaptcha -> {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    modifier = Modifier.weight(1f),
                                    value = viewModel.picCaptchaValue.value,
                                    onValueChange = viewModel::setPicCaptchaValue
                                )
                                Image(
                                    modifier = Modifier.size(96.dp, 48.dp),
                                    bitmap = (loginResource as LoginResource.PicCaptcha).captchaBitMap.asImageBitmap(),
                                    contentDescription = "PicCaptcha"
                                )
                                Button(onClick = {
                                    onEvent(
                                        StatusPageEvent.OnLoginResourceConfirm(
                                            viewModel.picCaptchaValue.value,
                                            (loginResource as LoginResource.PicCaptcha).metadata
                                        )
                                    )
                                }) {
                                    Text(text = "Confirm")
                                }
                            }
                        }

                        is LoginResource.UnsafeDeviceLoginVerify -> {
                            UnSafeWebView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .background(Color.Black),
                                url = (loginResource as LoginResource.UnsafeDeviceLoginVerify).url,
                                onConfirm = {
                                    onEvent(
                                        StatusPageEvent.OnLoginResourceConfirm(
                                            it,
                                            (loginResource as LoginResource.UnsafeDeviceLoginVerify).metadata
                                        )
                                    )
                                }
                            )
                        }

                        is LoginResource.SliderCaptcha -> {
                            UnSafeWebView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .background(Color.Black),
                                url = (loginResource as LoginResource.SliderCaptcha).url,
                                onConfirm = {
                                    onEvent(
                                        StatusPageEvent.OnLoginResourceConfirm(
                                            it,
                                            (loginResource as LoginResource.SliderCaptcha).metadata
                                        )
                                    )
                                }
                            )
                        }

                        is LoginResource.Sms -> {
                            var input by remember {
                                mutableStateOf("")
                            }
                            var submitted by remember {
                                mutableStateOf(false)
                            }
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = input, onValueChange = { input = it })
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    enabled = input.length >= 6 && !submitted,
                                    onClick = {
                                        submitted = true
                                        onEvent(
                                            StatusPageEvent.OnDeviceVerificationSmsConfirm(
                                                input,
                                                (loginResource as LoginResource.Sms).metadata
                                            )
                                        )
                                    }) {
                                    Text(text = stringResource(R.string.submit))
                                }
                            }
                        }

                        is LoginResource.Fallback -> {
                            val state =
                                rememberWebViewState((loginResource as LoginResource.Fallback).url)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    modifier = Modifier.padding(16.dp),
                                    text = stringResource(R.string.login_resource_fallback_notice),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                WebView(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 360.dp),
                                    state = state
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(modifier = Modifier.align(Alignment.Center), onClick = {
                                        onEvent(
                                            StatusPageEvent.OnDeviceVerificationFallbackConfirm((loginResource as LoginResource.Fallback).metadata)
                                        )
                                    }) {
                                        Text(text = stringResource(R.string.confirm))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "info") {
                AnimatedVisibility(
                    visible = !viewModel.mainSwitchState.value || !viewModel.mainSwitchEnabledState.value,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(24.dp, 16.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.extension_info),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "settings") {
                Column() {
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferencesCategory(text = stringResource(R.string.action))
                    SwitchPreference(
                        title = stringResource(R.string.automatic_login),
                        subtitle = stringResource(R.string.automatic_login_sub),
                        checked = viewModel.autoLoginSwitchFlow.collectAsState(initial = false).value,
                        enabled = selectedIndexState != -1,
                        onCheckedChange = viewModel::setAutoLoginSwitch
                    )
                    SwitchPreference(
                        title = stringResource(R.string.foreground_service),
                        subtitle = stringResource(R.string.foreground_service_sub),
                        checked = viewModel.foregroundServiceSwitchFLow.collectAsState(
                            initial = false
                        ).value,
                        enabled = !viewModel.mainSwitchState.value && viewModel.mainSwitchEnabledState.value,
                        onCheckedChange = viewModel::setForegroundServiceSwitch
                    )
                    SwitchPreference(
                        title = stringResource(R.string.contact_cache),
                        subtitle = stringResource(R.string.contact_cache_sub),
                        checked = viewModel.contactCacheSwitchFlow.collectAsState(initial = false).value,
                        enabled = !viewModel.mainSwitchState.value && viewModel.mainSwitchEnabledState.value,
                        onCheckedChange = viewModel::setContactCacheSwitch
                    )
                    SimpleMenuPreference<Int>(
                        title = stringResource(R.string.protocol_options),
                        optionsMap = viewModel.protocolOptionsMap,
                        selectedKey = viewModel.protocolSimpleMenuFLow.collectAsState(
                            initial = null
                        ).value,
                        enabled = !viewModel.mainSwitchState.value && viewModel.mainSwitchEnabledState.value,
                        onSelect = viewModel::setProtocolSimpleMenu
                    )
//                    SwitchPreference(
//                        title = "取消超时",
//                        subtitle = "取消登录各流程的超时错误，但可能导致不可预见的问题",
//                        checked = viewModel.cancelTimeoutSwitchFlow.collectAsState(initial = false).value,
//                        enabled = !viewModel.mainSwitchState.value && viewModel.mainSwitchEnabledState.value,
//                        onCheckedChange = viewModel::setCancelTimeoutSwitch
//                    )
                    NormalPreference(
                        title = stringResource(R.string.request_ignore_battery_optimizations),
                        subtitle = stringResource(
                            R.string.request_ignore_battery_optimizations_sub
                        )
                    ) {
                        onEvent(StatusPageEvent.OnRequestIgnoreBatteryOptimizations)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferencesCategory(text = stringResource(R.string.trouble_shooting))
                    NormalPreference(title = stringResource(R.string.faq), subtitle = stringResource(
                                            R.string.faq_sub)
                                        ) {
                        onEvent(StatusPageEvent.OnLaunchBrowser("https://github.com/ojhdt/Mirai-Plugin-for-Parabox/blob/main/FAQ.md"))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferencesCategory(text = stringResource(R.string.about))
                    NormalPreference(title = stringResource(R.string.version), subtitle = viewModel.appVersion) {

                    }
                    NormalPreference(
                        title = stringResource(R.string.mirai_core_version),
                        subtitle = viewModel.miraiCoreVersion
                    ) {
                        onEvent(StatusPageEvent.OnLaunchBrowser("https://github.com/mamoe/mirai"))
                    }
                    NormalPreference(title = stringResource(R.string.github_repository)) {
                        onEvent(StatusPageEvent.OnLaunchBrowser("https://github.com/ojhdt/Mirai-Plugin-for-Parabox"))
                    }
                    NormalPreference(title = stringResource(R.string.license)) {
                        navigator.navigate(LicensePageDestination())
                    }
                }
            }
        }

//        val scrollState = rememberScrollState()
//        Column(
//            modifier = Modifier
//                .padding(paddingValues)
//                .verticalScroll(scrollState)
//        ) {
//            MainSwitch(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .fillMaxWidth(),
//                onEvent = onEvent,
//                checked = viewModel.mainSwitchState.value,
//                onCheckedChange = {
//                    viewModel.setMainSwitchState(it)
//                    if (it) onEvent(StatusPageEvent.OnServiceStart)
//                    else onEvent(StatusPageEvent.OnServiceStop)
//                },
//                enabled = viewModel.mainSwitchEnabledState.value
//            )
//            StatusIndicator(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                status = viewModel.serviceStatusStateFlow.collectAsState().value
//            )
//            val loginResource by viewModel.loginResourceStateFlow.collectAsState()
//            when (loginResource) {
//                is LoginResource.None -> {
//                }
//                is LoginResource.PicCaptcha -> {
//                    Row() {
////                    TextField(
////                        modifier = Modifier.weight(1f),
////                        value = viewModel.loginTextState.value,
////                        onValueChange = viewModel::setLoginTextState
////                    )
//                        Image(
//                            bitmap = (loginResource as LoginResource.PicCaptcha).captchaBitMap.asImageBitmap(),
//                            contentDescription = "PicCaptcha"
//                        )
//                        Button(onClick = {
//                            onEvent(
//                                StatusPageEvent.OnLoginResourceConfirm(
//                                    "",
//                                    loginResource.timestamp
//                                )
//                            )
//                        }) {
//                            Text(text = "Confirm")
//                        }
//                    }
//                }
//                is LoginResource.UnsafeDeviceLoginVerify -> {
//                    UnSafeWebView(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(300.dp)
//                            .background(Color.Black),
//                        url = (loginResource as LoginResource.UnsafeDeviceLoginVerify).url,
//                        onConfirm = {
//                            onEvent(
//                                StatusPageEvent.OnLoginResourceConfirm(
//                                    it,
//                                    loginResource.timestamp
//                                )
//                            )
//                        }
//                    )
//                }
//                is LoginResource.SliderCaptcha -> {
//                    UnSafeWebView(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(300.dp)
//                            .background(Color.Black),
//                        url = (loginResource as LoginResource.SliderCaptcha).url,
//                        onConfirm = {
//                            onEvent(
//                                StatusPageEvent.OnLoginResourceConfirm(
//                                    it,
//                                    loginResource.timestamp
//                                )
//                            )
//                        }
//                    )
//                }
//            }
//            AnimatedVisibility(visible = !viewModel.mainSwitchState.value || !viewModel.mainSwitchEnabledState.value) {
//                Column(modifier = Modifier.padding(24.dp, 16.dp)) {
//                    Icon(
//                        imageVector = Icons.Outlined.Info,
//                        contentDescription = "info",
//                        tint = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Text(
//                        text = "本插件将为 Parabox 添加 Mirai 支持，需首先安装主端",
//                        style = MaterialTheme.typography.labelLarge,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
////            AnimatedVisibility(visible = viewModel.mainSwitchEnabledState.value && viewModel.mainSwitchState.value) {}
//            Spacer(modifier = Modifier.height(16.dp))
//            PreferencesCategory(text = "行为")
//            SwitchPreference(
//                title = "自动登录",
//                subtitle = "应用启动时同时以默认账户启动服务",
//                checked = viewModel.autoLoginSwitchState.value,
//                onCheckedChange = viewModel::setAutoLoginSwitchState
//            )
//            SwitchPreference(
//                title = "前台服务",
//                subtitle = "可提高后台留存能力",
//                checked = viewModel.foregroundServiceSwitchState.value,
//                onCheckedChange = viewModel::setForegroundServiceSwitchState
//            )
//            SwitchPreference(
//                title = "列表缓存",
//                subtitle = "可大幅加速登陆进程，但可能引起列表不同步问题",
//                checked = viewModel.contactCacheSwitchState.value,
//                onCheckedChange = viewModel::setContactCacheSwitchState
//            )
//            NormalPreference(title = "切换登陆协议", subtitle = "不明原因登录失败时可尝试切换协议\n通常情况下不需要更改") {
//
//            }
//            NormalPreference(title = "忽略电池优化", subtitle = "可提高后台留存能力") {
//
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            PreferencesCategory(text = "故障排除")
//            NormalPreference(title = "疑难解答", subtitle = "常见问题及其解决方案") {
//
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            PreferencesCategory(text = "关于")
//            NormalPreference(title = "版本", subtitle = viewModel.appVersion) {
//
//            }
//            NormalPreference(title = "项目地址") {
//
//            }
//            NormalPreference(title = "开放源代码许可") {
//
//            }
//        }
    }
}

@Composable
fun MainSwitch(
    modifier: Modifier = Modifier,
    onEvent: (StatusPageEvent) -> Unit,
    checked: Boolean,
    onCheckedChange: (value: Boolean) -> Unit,
    enabled: Boolean
) {
    val context = LocalContext.current
    val switchColor by animateColorAsState(targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .clickable {
                if (enabled) onCheckedChange(!checked)
                else onEvent(StatusPageEvent.OnShowToast(context.getString(R.string.repeat_click_toast)))
            },
        color = switchColor,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.enable_extension),
                style = MaterialTheme.typography.titleLarge,
                fontSize = MaterialTheme.fontSize.title,
                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}
