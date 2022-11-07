package com.ojhdtapp.miraipluginforparabox.ui.license

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.miraipluginforparabox.core.util.BrowserUtil
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageEvent
import com.ojhdtapp.miraipluginforparabox.ui.util.NormalPreference
import com.ojhdtapp.miraipluginforparabox.ui.util.SharedAxisZTransition
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun AnimatedVisibilityScope.LicensePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    onEvent: (StatusPageEvent) -> Unit
) {
    val viewModel = hiltViewModel<LicensePageViewModel>()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = "开放源代码许可") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValue ->
        LazyColumn(
            contentPadding = paddingValue,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = rememberLazyListState()
        ) {
            items(items = viewModel.licenseList) {
                NormalPreference(
                    title = it.name,
                    subtitle = it.license
                ) {
                    BrowserUtil.launchURL(context, it.url)
                }
            }
        }
    }
}

data class License(
    val name: String,
    val url: String,
    val license: String,
)