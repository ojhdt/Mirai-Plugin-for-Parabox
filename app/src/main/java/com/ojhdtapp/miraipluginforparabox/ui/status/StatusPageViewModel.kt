package com.ojhdtapp.miraipluginforparabox.ui.status

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusPageViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    // emit to this when wanting toasting
    private val _uiEventFlow = MutableSharedFlow<StatusPageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    // Login Resource
    private var _loginResourceStateFlow = MutableStateFlow<LoginResource>(LoginResource.None)
    val loginResourceStateFlow = _loginResourceStateFlow.asStateFlow()
    fun updateLoginResourceStateFlow(value: LoginResource) {
        _loginResourceStateFlow.tryEmit(value)
    }

    // Service Status
    private val _serviceStatusStateFlow = MutableStateFlow<ServiceStatus>(ServiceStatus.Stop)
    val serviceStatusStateFlow = _serviceStatusStateFlow.asStateFlow()
    fun updateServiceStatusStateFlow(value: ServiceStatus){
        _serviceStatusStateFlow.tryEmit(value)
    }

    // Account Dialog
    val accountFLow = repository.getAccountListFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedIndexFlow = repository.getAccountListFlow().mapLatest {
        it.indexOfFirst { secret -> secret.selected }
    }

    fun addNewAccount(secret: Secret) {
        viewModelScope.launch {
            repository.addNewAccount(secret)
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar("已成功添加账户"))
        }
    }

    fun deleteAccount(secret: Secret) {
        viewModelScope.launch {
            repository.deleteAccount(secret)
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar("已成功删除账户"))
        }
    }

    fun updateAccounts(selectedIndex: Int, accountList: List<Secret>) {
        viewModelScope.launch {
            repository.addAllAccounts(accountList.toList().mapIndexed { index, secret ->
                secret.apply {
                    selected = index == selectedIndex
                }
            })
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar("已保存更改"))
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
    private val _mainSwitchEnabledState = mutableStateOf<Boolean>(true)
    val mainSwitchEnabledState : State<Boolean> = _mainSwitchEnabledState

    fun setMainSwitchEnabledState(value : Boolean){
        _mainSwitchEnabledState.value = value
    }
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