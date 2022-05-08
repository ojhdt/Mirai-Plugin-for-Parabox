package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StatusPage(
    modifier: Modifier = Modifier,
    onBtnCLicked: (accountNum: Long, passwd: String) -> Unit
) {
    val viewModel: StatusPageViewModel = hiltViewModel()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { onBtnCLicked("2371065280".toLong(), "b20011007") }) {
            Text(text = "Login")
        }
    }
}