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

    private val _loginTextState = mutableStateOf<String>("")
    val loginTextState : State<String> = _loginTextState
    fun setLoginTextState(value: String){
        _loginTextState.value = value
    }
}