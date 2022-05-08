package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatusPageViewModel @Inject constructor(

) : ViewModel() {
    fun onEvent(event: StatusPageEvent) {
        when (event) {
            is StatusPageEvent.OnLoginClick -> {

            }
            else -> {

            }
        }
    }
}