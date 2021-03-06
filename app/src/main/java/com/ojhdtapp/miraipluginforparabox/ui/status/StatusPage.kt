package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.animation.*
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.destinations.LicensePageDestination
import com.ojhdtapp.miraipluginforparabox.ui.theme.fontSize
import com.ojhdtapp.miraipluginforparabox.ui.util.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination(start = true, style = SharedAxisZTransition::class)
@Composable
fun AnimatedVisibilityScope.StatusPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    viewModel: StatusPageViewModel,
    onEvent: (StatusPageEvent) -> Unit
) {
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
    val scrollState = rememberTopAppBarScrollState()
    val scrollBehavior = remember(decayAnimationSpec) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(decayAnimationSpec, scrollState)
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
                modifier = Modifier
                    .background(
                        TopAppBarDefaults
                            .largeTopAppBarColors()
                            .containerColor(
                                scrollFraction = scrollBehavior.scrollFraction
                            ).value
                    )
                    .statusBarsPadding(),
                actions = {
                    IconButton(onClick = {
                        if (viewModel.serviceStatusStateFlow.value is ServiceStatus.Stop || viewModel.serviceStatusStateFlow.value is ServiceStatus.Error) {
                            openAccountDialog = true
                        } else {
                            onEvent(StatusPageEvent.OnShowToast("??????????????????????????????????????????"))
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
                                text = { Text(text = "??????????????????") },
                                onClick = {
                                    onEvent(StatusPageEvent.OnServiceForceStop)
                                    viewModel.emitToUiEventFlow(StatusPageUiEvent.ShowSnackBar("?????????????????????"))
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

        LazyColumn(
            modifier = modifier,
            contentPadding = paddingValues,
            state = rememberLazyListState()
        ) {
            item {
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
            item {
                StatusIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    status = viewModel.serviceStatusStateFlow.collectAsState().value
                )
            }

            item {
                val loginResource by viewModel.loginResourceStateFlow.collectAsState()
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
                                modifier = Modifier
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
                                modifier = Modifier
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
                }
            }

            item {
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
                            text = "??????????????? Parabox ?????? Mirai ??????????????????????????????",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column() {
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferencesCategory(text = "??????")
                    SwitchPreference(
                        title = "????????????",
                        subtitle = "????????????????????????????????????????????????",
                        checked = viewModel.autoLoginSwitchFlow.collectAsState(initial = false).value,
                        onCheckedChange = viewModel::setAutoLoginSwitch
                    )
                    AnimatedVisibility(
                        visible = !viewModel.mainSwitchState.value && viewModel.mainSwitchEnabledState.value,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SwitchPreference(
                                title = "????????????",
                                subtitle = "???????????????????????????",
                                checked = viewModel.foregroundServiceSwitchFLow.collectAsState(
                                    initial = false
                                ).value,
                                onCheckedChange = viewModel::setForegroundServiceSwitch
                            )
                            SwitchPreference(
                                title = "????????????",
                                subtitle = "????????????????????????????????????????????????????????????????????????",
                                checked = viewModel.contactCacheSwitchFlow.collectAsState(initial = false).value,
                                onCheckedChange = viewModel::setContactCacheSwitch
                            )
                            SimpleMenuPreference<Int>(
                                title = "??????????????????",
                                optionsMap = viewModel.protocolOptionsMap,
                                selectedKey = viewModel.protocolSimpleMenuFLow.collectAsState(
                                    initial = null
                                ).value,
                                onSelect = viewModel::setProtocolSimpleMenu
                            )
                        }
                    }
                    SwitchPreference(
                        title = "????????????",
                        subtitle = "???????????????????????????????????????????????????????????????????????????",
                        checked = viewModel.cancelTimeoutSwitchFlow.collectAsState(initial = false).value,
                        onCheckedChange = viewModel::setCancelTimeoutSwitch
                    )
                    NormalPreference(title = "??????????????????", subtitle = "???????????????????????????") {
                        onEvent(StatusPageEvent.OnRequestIgnoreBatteryOptimizations)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferencesCategory(text = "????????????")
                    NormalPreference(title = "????????????", subtitle = "??????????????????????????????") {
                        onEvent(StatusPageEvent.OnLaunchBrowser("https://www.baidu.com/"))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferencesCategory(text = "??????")
                    NormalPreference(title = "??????", subtitle = viewModel.appVersion) {

                    }
                    NormalPreference(title = "????????????") {
                        onEvent(StatusPageEvent.OnLaunchBrowser("https://www.baidu.com/"))
                    }
                    NormalPreference(title = "?????????????????????") {
                        navigator.navigate(LicensePageDestination())
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
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
//                        text = "??????????????? Parabox ?????? Mirai ??????????????????????????????",
//                        style = MaterialTheme.typography.labelLarge,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
////            AnimatedVisibility(visible = viewModel.mainSwitchEnabledState.value && viewModel.mainSwitchState.value) {}
//            Spacer(modifier = Modifier.height(16.dp))
//            PreferencesCategory(text = "??????")
//            SwitchPreference(
//                title = "????????????",
//                subtitle = "????????????????????????????????????????????????",
//                checked = viewModel.autoLoginSwitchState.value,
//                onCheckedChange = viewModel::setAutoLoginSwitchState
//            )
//            SwitchPreference(
//                title = "????????????",
//                subtitle = "???????????????????????????",
//                checked = viewModel.foregroundServiceSwitchState.value,
//                onCheckedChange = viewModel::setForegroundServiceSwitchState
//            )
//            SwitchPreference(
//                title = "????????????",
//                subtitle = "??????????????????????????????????????????????????????????????????",
//                checked = viewModel.contactCacheSwitchState.value,
//                onCheckedChange = viewModel::setContactCacheSwitchState
//            )
//            NormalPreference(title = "??????????????????", subtitle = "????????????????????????????????????????????????\n??????????????????????????????") {
//
//            }
//            NormalPreference(title = "??????????????????", subtitle = "???????????????????????????") {
//
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            PreferencesCategory(text = "????????????")
//            NormalPreference(title = "????????????", subtitle = "??????????????????????????????") {
//
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            PreferencesCategory(text = "??????")
//            NormalPreference(title = "??????", subtitle = viewModel.appVersion) {
//
//            }
//            NormalPreference(title = "????????????") {
//
//            }
//            NormalPreference(title = "?????????????????????") {
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
    val switchColor by animateColorAsState(targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(switchColor)
            .clickable {
                if (enabled) onCheckedChange(!checked)
                else onEvent(StatusPageEvent.OnShowToast("????????????????????????????????????"))
            }
            .padding(24.dp, 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "????????????",
            style = MaterialTheme.typography.titleLarge,
            fontSize = MaterialTheme.fontSize.title,
            color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
