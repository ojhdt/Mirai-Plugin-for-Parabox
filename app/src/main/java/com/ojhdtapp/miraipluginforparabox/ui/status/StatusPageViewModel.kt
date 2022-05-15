package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StatusPageViewModel @Inject constructor(

) : ViewModel() {

    // emit to this when wanting toasting
    private val _uiEventFlow = MutableSharedFlow<StatusPageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private var _loginResourceStateFlow = MutableStateFlow<LoginResource>(LoginResource.None)
    val loginResourceStateFlow = _loginResourceStateFlow.asStateFlow()
    fun updateLoginResourceStateFlow(value: LoginResource){
        _loginResourceStateFlow.tryEmit(value)
    }

    private val _mainSwitchState = mutableStateOf<Boolean>(false)
    val mainSwitchState : State<Boolean> = _mainSwitchState
    fun setMainSwitchState(value : Boolean){
        _mainSwitchState.value = value
    }

    private val _autoLoginSwitchState = mutableStateOf<Boolean>(false)
    val autoLoginSwitchState : State<Boolean> = _autoLoginSwitchState
    fun setAutoLoginSwitchState(value : Boolean){
        _autoLoginSwitchState.value = value
    }
    
    private val _foregroundServiceSwitchState = mutableStateOf<Boolean>(false)
    val foregroundServiceSwitchState : State<Boolean> = _foregroundServiceSwitchState
    
    fun setForegroundServiceSwitchState(value : Boolean){
        _foregroundServiceSwitchState.value = value
    }

    val appVersion = "1.0"
}