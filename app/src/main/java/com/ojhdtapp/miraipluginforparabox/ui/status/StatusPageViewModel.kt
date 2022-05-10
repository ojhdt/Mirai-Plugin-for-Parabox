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
    private val loginSolver = ConnService.instance.mLoginSolver

    fun onEvent(event: StatusPageEvent) {
        when (event) {
            is StatusPageEvent.OnLoginClick -> {

            }
            is StatusPageEvent.OnPicCaptchaConfirm -> {
                event.captcha?.let{
                    loginSolver.submitVerificationResult(it)
                }
            }
            is StatusPageEvent.OnSliderCaptchaConfirm -> {

            }
            is StatusPageEvent.OnUnsafeDeviceLoginVerifyConfirm -> {

            }
            else -> {

            }
        }
    }

    private var _loginResourceStateFlow = loginSolver.getLoginResourceStateFlow()
    val loginResourceStateFlow = _loginResourceStateFlow.asStateFlow()

    private val _loginTextState = mutableStateOf<String>("")
    val loginTextState : State<String> = _loginTextState

    fun setLoginTextState(value: String){
        _loginTextState.value = value
    }
}