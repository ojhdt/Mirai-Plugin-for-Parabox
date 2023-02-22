package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ojhdtapp.miraipluginforparabox.R
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus

@Composable
fun StatusIndicator(
    modifier: Modifier = Modifier, status: ServiceStatus,
    shouldShowRetryButton: Boolean = false,
    onRetryButtonClick: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = status !is ServiceStatus.Stop,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val context = LocalContext.current
        val backgroundColor by animateColorAsState(
            targetValue = when (status) {
                is ServiceStatus.Error -> MaterialTheme.colorScheme.errorContainer
                is ServiceStatus.Loading -> MaterialTheme.colorScheme.primary
                is ServiceStatus.Running -> MaterialTheme.colorScheme.primary
                is ServiceStatus.Stop -> MaterialTheme.colorScheme.primary
                is ServiceStatus.Pause -> MaterialTheme.colorScheme.primary
            }
        )
        val textColor by animateColorAsState(
            targetValue = when (status) {
                is ServiceStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                is ServiceStatus.Loading -> MaterialTheme.colorScheme.onPrimary
                is ServiceStatus.Running -> MaterialTheme.colorScheme.onPrimary
                is ServiceStatus.Stop -> MaterialTheme.colorScheme.onPrimary
                is ServiceStatus.Pause -> MaterialTheme.colorScheme.onPrimary
            }
        )
        Row(modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(backgroundColor)
            .clickable { }
            .padding(24.dp, 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                is ServiceStatus.Error -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "error",
                    tint = textColor
                )
                is ServiceStatus.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(PaddingValues(end = 24.dp))
                        .size(24.dp),
                    color = textColor,
                    strokeWidth = 3.dp
                )
                is ServiceStatus.Running -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "running",
                    tint = textColor
                )
                is ServiceStatus.Stop -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "stop",
                    tint = textColor
                )
                is ServiceStatus.Pause -> Icon(
                    modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                    imageVector = Icons.Outlined.PauseCircleOutline,
                    contentDescription = "pause",
                    tint = textColor
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (status) {
                        is ServiceStatus.Error -> stringResource(R.string.service_status_error)
                        is ServiceStatus.Loading -> stringResource(R.string.service_status_loading)
                        is ServiceStatus.Running -> stringResource(R.string.service_status_running)
                        is ServiceStatus.Stop -> stringResource(R.string.service_status_stop)
                        is ServiceStatus.Pause -> stringResource(R.string.service_status_pause)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                )
                if (!status.message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status.message!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            AnimatedVisibility(visible = shouldShowRetryButton) {
                FilledTonalButton(
                    onClick = onRetryButtonClick) {
                    Text(text = "重试")
                }
            }
        }
    }
}