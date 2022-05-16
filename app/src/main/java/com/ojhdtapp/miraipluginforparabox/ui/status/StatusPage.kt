package com.ojhdtapp.miraipluginforparabox.ui.status

import android.widget.EditText
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
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
    // snackBar & navigation
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                else -> {
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
    if (openAccountDialog) {
        Dialog(onDismissRequest = { openAccountDialog = false }) {
            Column() {
                Text("选择账号")
            }
        }
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
                    IconButton(onClick = { openAccountDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "account"
                        )
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "menu")
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
        content = {
//            val scrollState = rememberScrollState()
            Column(
                modifier = modifier
                    .padding(it)
                    .fillMaxSize()
//                    .scrollable(
//                        state = scrollState,
//                        orientation = Orientation.Vertical,
//                        enabled = true
//                    )
            ) {
                MainSwitch(
                    modifier = modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    checked = viewModel.mainSwitchState.value,
                    viewModel::setMainSwitchState
                )
                if (viewModel.mainSwitchState.value) {
                    Spacer(modifier = modifier.height(16.dp))
                    PreferencesCategory(text = "行为")
                    SwitchPreference(
                        title = "自动登录",
                        subtitle = "以默认账户启动服务",
                        checked = viewModel.autoLoginSwitchState.value,
                        onCheckedChange = viewModel::setAutoLoginSwitchState
                    )
                    SwitchPreference(
                        title = "前台服务",
                        subtitle = "提高后台留存能力",
                        checked = viewModel.foregroundServiceSwitchState.value,
                        onCheckedChange = viewModel::setForegroundServiceSwitchState
                    )
                    NormalPreference(title = "忽略电池优化", subtitle = "提高后台留存能力") {

                    }
                    Spacer(modifier = modifier.height(16.dp))
                    PreferencesCategory(text = "故障排除")
                    NormalPreference(title = "疑难解答", subtitle = "常见问题及其解决方案") {

                    }
                } else {
                    Column(modifier = modifier.padding(24.dp, 0.dp)) {
                        Icon(imageVector = Icons.Outlined.Info, contentDescription = "info", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = modifier.height(4.dp))
                        Text(
                            text = "本插件将为 Parabox 添加 Mirai 支持，需首先安装主端",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
//            LazyColumn(modifier = modifier.fillMaxSize(),contentPadding = it) {
//                item {
//
//                }
//                item {
//
//                }
//                item{
//
//                }
//            }
        }
    )

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
    checked: Boolean,
    onCheckedChange: (value: Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onCheckedChange(!checked) }
            .padding(24.dp, 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "启用插件",
            style = MaterialTheme.typography.titleLarge,
            color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
