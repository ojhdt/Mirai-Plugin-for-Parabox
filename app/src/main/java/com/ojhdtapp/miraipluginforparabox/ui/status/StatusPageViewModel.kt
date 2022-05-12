package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StatusPageViewModel @Inject constructor(

) : ViewModel() {

    // emit to this when wanting toasting
    private val _uiEventFlow = MutableSharedFlow<StatusPageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    fun onEvent(event: StatusPageEvent) {
        when (event) {
            is StatusPageEvent.OnLoginClick -> {

            }
            is StatusPageEvent.OnPicCaptchaConfirm -> {
                event.captcha?.let{
                    ConnService.instance.mLoginSolver.submitVerificationResult(it)
                }
            }
            is StatusPageEvent.OnSliderCaptchaConfirm -> {
                ConnService.instance.mLoginSolver.submitVerificationResult(event.ticket)
            }
            is StatusPageEvent.OnUnsafeDeviceLoginVerifyConfirm -> {
                ConnService.instance.mLoginSolver.submitVerificationResult("success")
            }
            else -> {

            }
        }
    }

    private var _loginResourceStateFlow = ConnService.loginResourceStateFlow
    val loginResourceStateFlow = _loginResourceStateFlow.asStateFlow()

    private val _loginTextState = mutableStateOf<String>("")
    val loginTextState : State<String> = _loginTextState

    fun setLoginTextState(value: String){
        _loginTextState.value = value
    }
}