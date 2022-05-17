package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.miraipluginforparabox.data.repository.MainRepositoryImpl
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusPageViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    // emit to this when wanting toasting
    private val _uiEventFlow = MutableSharedFlow<StatusPageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private var _loginResourceStateFlow = MutableStateFlow<LoginResource>(LoginResource.None)
    val loginResourceStateFlow = _loginResourceStateFlow.asStateFlow()
    fun updateLoginResourceStateFlow(value: LoginResource) {
        _loginResourceStateFlow.tryEmit(value)
    }

    // Account Dialog
    val accountFLow = repository.getAccountListFlow()
    fun addNewAccount(secrets: Secrets) {
        viewModelScope.launch {
            repository.addNewAccount(secrets)
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar("已成功添加账户"))
        }
    }

    fun deleteAccount(secrets: Secrets) {
        viewModelScope.launch {
            repository.deleteAccount(secrets)
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar("已成功删除账户"))
        }
    }

    private val _accountNum = mutableStateOf<String>("")
    val accountNum: State<String> = _accountNum

    fun setAccountNum(value: String) {
        _accountNum.value = value
    }

    private val _passwd = mutableStateOf<String>("")
    val passwd: State<String> = _passwd

    fun setPasswd(value: String) {
        _passwd.value = value
    }

    // Switches
    private val _mainSwitchState = mutableStateOf<Boolean>(false)
    val mainSwitchState: State<Boolean> = _mainSwitchState
    fun setMainSwitchState(value: Boolean) {
        _mainSwitchState.value = value
    }

    private val _autoLoginSwitchState = mutableStateOf<Boolean>(false)
    val autoLoginSwitchState: State<Boolean> = _autoLoginSwitchState
    fun setAutoLoginSwitchState(value: Boolean) {
        _autoLoginSwitchState.value = value
    }

    private val _foregroundServiceSwitchState = mutableStateOf<Boolean>(false)
    val foregroundServiceSwitchState: State<Boolean> = _foregroundServiceSwitchState
    fun setForegroundServiceSwitchState(value: Boolean) {
        _foregroundServiceSwitchState.value = value
    }

    val appVersion = "1.0"
}