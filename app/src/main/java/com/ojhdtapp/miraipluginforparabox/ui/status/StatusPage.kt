package com.ojhdtapp.miraipluginforparabox.ui.status

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.theme.fontSize
import com.ojhdtapp.miraipluginforparabox.ui.util.NormalPreference
import com.ojhdtapp.miraipluginforparabox.ui.util.PreferencesCategory
import com.ojhdtapp.miraipluginforparabox.ui.util.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPage(
    modifier: Modifier = Modifier,
    onEvent: (StatusPageEvent) -> Unit
) {
    // viewModel
    val viewModel: StatusPageViewModel = hiltViewModel()
    val snackBarHostState = remember { SnackbarHostState() }
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
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = remember(decayAnimationSpec) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(decayAnimationSpec)
    }
    // accountDialog
    var openAccountDialog by remember {
        mutableStateOf(false)
    }
    AccountDialog(
        onEvent = onEvent,
        isOpen = openAccountDialog,
        onDismissRequest = { openAccountDialog = false },
        accountList = viewModel.accountFLow.collectAsState(
            initial = emptyList()
        ).value,
        initialSelectedIndex = viewModel.selectedIndexFlow.collectAsState(initial = -1).value,
        onAddSecret = viewModel::addNewAccount,
        onDeleteSecret = viewModel::deleteAccount,
        onUpdateSelectedSecret = viewModel::updateAccounts
    )

    // menu
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Mirai Plugin",
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (viewModel.serviceStatusStateFlow.value is ServiceStatus.Stop) {
                            openAccountDialog = true
                        } else {
                            onEvent(StatusPageEvent.OnShowToast("服务运行期间不可修改账户信息"))
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
                                text = { Text(text = "强行停止服务") },
                                onClick = {
                                    onEvent(StatusPageEvent.OnServiceForceStop)
                                    viewModel.emitToUiEventFlow(StatusPageUiEvent.ShowSnackBar("已强行停止服务"))
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
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            MainSwitch(
                modifier = modifier
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
            StatusIndicator(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                status = viewModel.serviceStatusStateFlow.collectAsState().value
            )
            val loginResource by viewModel.loginResourceStateFlow.collectAsState()
            when (loginResource) {
                is LoginResource.None -> {
                }
                is LoginResource.PicCaptcha -> {
                    Row() {
//                    TextField(
//                        modifier = modifier.weight(1f),
//                        value = viewModel.loginTextState.value,
//                        onValueChange = viewModel::setLoginTextState
//                    )
                        Image(
                            bitmap = (loginResource as LoginResource.PicCaptcha).captchaBitMap.asImageBitmap(),
                            contentDescription = "PicCaptcha"
                        )
                        Button(onClick = {
                            onEvent(
                                StatusPageEvent.OnLoginResourceConfirm(
                                    "asdf",
                                    loginResource.timestamp
                                )
                            )
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
                        url = (loginResource as LoginResource.UnsafeDeviceLoginVerify).url,
                        onConfirm = {
                            onEvent(
                                StatusPageEvent.OnLoginResourceConfirm(
                                    it,
                                    loginResource.timestamp
                                )
                            )
                        }
                    )
                }
                is LoginResource.SliderCaptcha -> {
                    UnSafeWebView(
                        modifier = modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.Black),
                        url = (loginResource as LoginResource.SliderCaptcha).url,
                        onConfirm = {
                            onEvent(
                                StatusPageEvent.OnLoginResourceConfirm(
                                    it,
                                    loginResource.timestamp
                                )
                            )
                        }
                    )
                }
            }
            AnimatedVisibility(visible = !viewModel.mainSwitchState.value || !viewModel.mainSwitchEnabledState.value) {
                Column(modifier = modifier.padding(24.dp, 16.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = modifier.height(16.dp))
                    Text(
                        text = "本插件将为 Parabox 添加 Mirai 支持，需首先安装主端",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
//            AnimatedVisibility(visible = viewModel.mainSwitchEnabledState.value && viewModel.mainSwitchState.value) {}
            Column() {
                Spacer(modifier = modifier.height(16.dp))
                PreferencesCategory(text = "行为")
                SwitchPreference(
                    title = "自动登录",
                    subtitle = "应用启动时同时以默认账户启动服务",
                    checked = viewModel.autoLoginSwitchState.value,
                    onCheckedChange = viewModel::setAutoLoginSwitchState
                )
                SwitchPreference(
                    title = "前台服务",
                    subtitle = "可提高后台留存能力",
                    checked = viewModel.foregroundServiceSwitchState.value,
                    onCheckedChange = viewModel::setForegroundServiceSwitchState
                )
                SwitchPreference(
                    title = "列表缓存",
                    subtitle = "可大幅加速登陆进程，但可能引起列表不同步问题",
                    checked = viewModel.contactCacheSwitchState.value,
                    onCheckedChange = viewModel::setContactCacheSwitchState
                )
                NormalPreference(title = "切换登陆协议", subtitle = "不明原因登录失败时可尝试切换协议\n通常情况下不需要更改") {

                }
                NormalPreference(title = "忽略电池优化", subtitle = "可提高后台留存能力") {

                }
                Spacer(modifier = modifier.height(16.dp))
                PreferencesCategory(text = "故障排除")
                NormalPreference(title = "疑难解答", subtitle = "常见问题及其解决方案") {

                }

            }
            Spacer(modifier = modifier.height(16.dp))
            PreferencesCategory(text = "关于")
            NormalPreference(title = "版本", subtitle = viewModel.appVersion) {

            }
            NormalPreference(title = "项目地址") {

            }
            NormalPreference(title = "开放源代码许可") {

            }
        }
    }

//    Column(
//        modifier = modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        val loginResource by viewModel.loginResourceStateFlow.collectAsState()
//
//        Button(onClick = { onEvent(StatusPageEvent.OnLoginClick) }) {
//            Text(text = "Login")
//        }
//        Button(onClick = { onEvent(StatusPageEvent.OnKillClick) }) {
//            Text(text = "Kill")
//        }
//        when (loginResource) {
//            is LoginResource.None -> {
//            }
//            is LoginResource.PicCaptcha -> {
//                Row() {
//                    TextField(
//                        modifier = modifier.weight(1f),
//                        value = viewModel.loginTextState.value,
//                        onValueChange = viewModel::setLoginTextState
//                    )
//                    Image(
//                        bitmap = (loginResource as LoginResource.PicCaptcha).captchaBitMap.asImageBitmap(),
//                        contentDescription = "PicCaptcha"
//                    )
//                    Button(onClick = {
//
//                    }) {
//                        Text(text = "Confirm")
//                    }
//                }
//            }
//            is LoginResource.UnsafeDeviceLoginVerify -> {
//                UnSafeWebView(
//                    modifier = modifier
//                        .fillMaxWidth()
//                        .height(300.dp)
//                        .background(Color.Black),
//                    onEvent = onEvent,
//                    url = (loginResource as LoginResource.UnsafeDeviceLoginVerify).url
//                )
//            }
//            is LoginResource.SliderCaptcha -> {
//                UnSafeWebView(
//                    modifier = modifier
//                        .fillMaxWidth()
//                        .height(300.dp)
//                        .background(Color.Black),
//                    onEvent = onEvent,
//                    url = (loginResource as LoginResource.SliderCaptcha).url
//                )
//            }
//        }
//    }
}

@Composable
fun MainSwitch(
    modifier: Modifier = Modifier,
    onEvent: (StatusPageEvent) -> Unit,
    checked: Boolean,
    onCheckedChange: (value: Boolean) -> Unit,
    enabled: Boolean
) {
    val switchColor by animateColorAsState(targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(switchColor)
            .clickable {
                if (enabled) onCheckedChange(!checked)
                else onEvent(StatusPageEvent.OnShowToast("操作进行中，请勿重复点击"))
            }
            .padding(24.dp, 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "启用插件",
            style = MaterialTheme.typography.titleLarge,
            fontSize = MaterialTheme.fontSize.title,
            color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
