package com.ojhdtapp.miraipluginforparabox.ui.license

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageEvent
import com.ojhdtapp.miraipluginforparabox.ui.util.SharedAxisZTransition
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle

@OptIn(ExperimentalMaterial3Api::class)
@Destination(style = SharedAxisZTransition::class)
@Composable
fun AnimatedVisibilityScope.LicensePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    onEvent: (StatusPageEvent) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollFraction = scrollBehavior.state.overlappedFraction
    val topAppBarColor by TopAppBarDefaults.smallTopAppBarColors().containerColor(scrollFraction)
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                modifier = Modifier
                    .background(topAppBarColor)
                    .statusBarsPadding(),
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
            val list = (0..75).map { it.toString() }
            items(count = list.size) {
                Text(
                    text = list[it],
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            item { 
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}