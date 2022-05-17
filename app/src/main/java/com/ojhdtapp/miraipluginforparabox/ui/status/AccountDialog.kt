package com.ojhdtapp.miraipluginforparabox.ui.status

import android.widget.RadioGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AccountDialog(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    accountList: List<Secrets>
) {
    val viewModel: StatusPageViewModel = hiltViewModel()
    var selectedIndex by remember {
        mutableStateOf(0)
    }
    var isEditing by remember {
        mutableStateOf(false)
    }
    if (isOpen) {
        AlertDialog(onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = true }) {
                    Text(text = "添加账户")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "account"
                )
            },
            title = { Text(text = "选择账户") },
            text = {
                if (accountList.isEmpty()) {
                    Text(text = "无数据，请先至少添加一个账户。")
                }
                accountList.forEachIndexed { index, secrets ->
                    AccountItem(
                        data = secrets,
                        selected = selectedIndex == index,
                        index = index,
                        onOptionSelected = { newIndex -> selectedIndex = newIndex })
                }
            }
        )
    }

    AddAccountDialog(isOpen = isEditing, onDismissRequest = {isEditing = false})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountItem(
    modifier: Modifier = Modifier,
    data: Secrets,
    selected: Boolean,
    index: Int,
    onOptionSelected: (index: Int) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                onClick = { onOptionSelected(index) },
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null recommended for accessibility with screenreaders
        )
        Text(
            text = "${data.account}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
        data.bitmap?.let {
            Image(
                modifier = modifier
                    .size(48.dp)
                    .clip(CircleShape),
                bitmap = it.asImageBitmap(),
                contentDescription = "avatar"
            )
        }
    }
}

@Composable
fun AddAccountDialog(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
) {
    var accountNum by remember {
        mutableStateOf("")
    }
    var passwd by remember {
        mutableStateOf("")
    }
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = "添加账户") },
            text = {
                Column() {
                    OutlinedTextField(
                        value = accountNum,
                        onValueChange = { accountNum = it },
                        label = {
                            Text(
                                text = "账号"
                            )
                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = passwd,
                        onValueChange = { passwd = it },
                        label = {
                            Text(
                                text = "密码"
                            )
                        },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { /*TODO*/ }) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = "取消")
                }
            },
        )

    }
}