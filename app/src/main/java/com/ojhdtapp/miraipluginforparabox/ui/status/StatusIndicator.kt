package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus

@Composable
fun StatusIndicator(modifier: Modifier = Modifier, status: ServiceStatus) {
    AnimatedVisibility(visible = status !is ServiceStatus.Stop,
    enter = expandVertically(),
    exit = shrinkVertically()
    ) {
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
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { }
            .padding(24.dp, 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.padding(PaddingValues(end = 24.dp)),
                imageVector = when (status) {
                    is ServiceStatus.Error -> Icons.Outlined.Warning
                    is ServiceStatus.Loading -> Icons.Outlined.Refresh
                    is ServiceStatus.Running -> Icons.Outlined.CheckCircle
                    is ServiceStatus.Stop -> Icons.Outlined.CheckCircle
                    is ServiceStatus.Pause -> Icons.Outlined.Warning
                },
                contentDescription = when (status) {
                    is ServiceStatus.Error -> "error"
                    is ServiceStatus.Loading -> "loading"
                    is ServiceStatus.Running -> "running"
                    is ServiceStatus.Stop -> "stop"
                    is ServiceStatus.Pause -> "pause"
                },
                tint = textColor
            )
            Column() {
                Text(
                    text = when (status) {
                        is ServiceStatus.Error -> "启动服务时发生问题"
                        is ServiceStatus.Loading -> "服务正在启动"
                        is ServiceStatus.Running -> "服务正常运行"
                        is ServiceStatus.Stop -> "服务已停止"
                        is ServiceStatus.Pause -> "等待事务处理"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}