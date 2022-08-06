package com.ojhdtapp.miraipluginforparabox.ui.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.ojhdtapp.miraipluginforparabox.ui.theme.fontSize

@Composable
fun PreferencesCategory(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier
            .padding(24.dp, 8.dp)
            .fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (value: Boolean) -> Unit
) {
    val textColor by animateColorAsState(targetValue = if(enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
    Row(
        modifier = modifier
            .clickable {
                if (enabled) {
                    onCheckedChange(!checked)
                }
            }
            .padding(24.dp, 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = MaterialTheme.fontSize.title,
                color = textColor
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
        Spacer(modifier = Modifier.width(48.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun NormalPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(24.dp, 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = MaterialTheme.fontSize.title
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun <T> SimpleMenuPreference(
    modifier: Modifier = Modifier,
    title: String,
    selectedKey: T? = null,
    optionsMap: Map<T, String>,
    enabled: Boolean,
    onSelect: (selected: T) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    val textColor by animateColorAsState(targetValue = if(enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
    Row(
        modifier = modifier
            .clickable { if(enabled){
                expanded = true}
            }
            .padding(24.dp, 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropdownMenu(
            expanded = enabled && expanded,
            onDismissRequest = { expanded = false }
        ) {
            for ((key, value) in optionsMap) {
                DropdownMenuItem(text = { Text(text = value) }, onClick = {
                    expanded = false
                    onSelect(key)
                }, enabled = enabled)
            }
        }
        Column() {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = MaterialTheme.fontSize.title,
                color = textColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedKey?.let { optionsMap[it] } ?: optionsMap.values.first(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )

        }
    }
}