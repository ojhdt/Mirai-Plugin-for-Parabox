package com.ojhdtapp.miraipluginforparabox.ui.status

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret

@Composable
fun AccountDialog(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    accountList: List<Secret>,
    onAddSecret: (secret: Secret) -> Unit,
    onDeleteSecret: (secret: Secret) -> Unit,
    onUpdateSelectedSecret: (selectedIndex: Int, accountList: List<Secret>) -> Unit,
) {
    var selectedIndex by remember {
        TODO("always return -1")
        mutableStateOf(accountList.indexOfFirst { it.selected })
    }
    Log.d("parabox", accountList.toString())
    var isEditing by remember {
        mutableStateOf(false)
    }
    if (isOpen) {
        AlertDialog(onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = {
                    onUpdateSelectedSecret(selectedIndex, accountList)
                    onDismissRequest()
                }) {
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
                Column(
                    modifier = modifier
                        .verticalScroll(rememberScrollState())
//                        .weight(weight = 1f, fill = false)
                ) {
                    if (accountList.isEmpty()) {
                        Text(text = "无数据，请先至少添加一个账户。")
                    }
                    accountList.forEachIndexed { index, secret ->
                        AccountItem(
                            data = secret,
                            selected = selectedIndex == index,
                            index = index,
                            onOptionSelected = { newIndex -> selectedIndex = newIndex },
                            onDelete = {
                                onDeleteSecret(accountList[it])
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        )
    }

    AddAccountDialog(
        isOpen = isEditing,
        onDismissRequest = { isEditing = false },
        onAddSecret = {
            onDismissRequest()
            onAddSecret(it.apply {
                if (accountList.isEmpty()) selected = true
            })
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountItem(
    modifier: Modifier = Modifier,
    data: Secret,
    selected: Boolean,
    index: Int,
    onOptionSelected: (index: Int) -> Unit,
    onDelete: (index: Int) -> Unit,
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                onClick = {
                    onOptionSelected(index)
                    if (selected) menuExpanded = true
                },
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(text = "删除") },
                onClick = { onDelete(index) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "delete"
                    )
                })
        }
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
    onAddSecret: (secret: Secret) -> Unit
) {
    var accountNum by remember {
        mutableStateOf("")
    }
    var accountNumError by remember {
        mutableStateOf(false)
    }
    var passwd by remember {
        mutableStateOf("")
    }
    var passwdError by remember {
        mutableStateOf(false)
    }
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = "添加账户") },
            text = {
                Column(modifier = modifier.verticalScroll(rememberScrollState())) {
                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(
                        value = accountNum,
                        onValueChange = {
                            if (accountNumError) accountNumError = false
                            accountNum = it
                        },
                        label = {
                            Text(
                                text = "账号"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = accountNumError
                    )
                    Spacer(modifier = modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwd,
                        onValueChange = {
                            if (passwdError) passwdError = false
                            passwd = it
                        },
                        label = {
                            Text(
                                text = "密码"
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                checkAndSubmit(
                                    accountNum,
                                    passwd,
                                    {
                                        onAddSecret(it)
                                        accountNum = ""
                                        passwd = ""
                                    },
                                    onDismissRequest,
                                    { accountNumError = it },
                                    { passwdError = it }
                                )
                            }
                        ),
                        isError = passwdError
                    )
                    Spacer(modifier = modifier.height(24.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = modifier.height(16.dp))
                    Text(
                        text = "软件将会保存你的账号和密码并在服务启动时完成登录。一切信息仅将被保存于本地。",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    checkAndSubmit(
                        accountNum,
                        passwd,
                        {
                            onAddSecret(it)
                            accountNum = ""
                            passwd = ""
                        },
                        onDismissRequest,
                        { accountNumError = it },
                        { passwdError = it }
                    )
                }) {
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

private fun checkAndSubmit(
    accountNum: String,
    passwd: String,
    onAddSecret: (secret: Secret) -> Unit,
    onDismissRequest: () -> Unit,
    onAccountNumError: (Boolean) -> Unit,
    onPasswdError: (Boolean) -> Unit
) {
    if (accountNum.isNotBlank() && passwd.isNotBlank()) {
        onAddSecret(
            Secret(
                account = accountNum.toLong(),
                password = passwd
            )
        )
        onDismissRequest()
    } else {
        onAccountNumError(accountNum.isBlank())
        onPasswdError(passwd.isBlank())
    }
}