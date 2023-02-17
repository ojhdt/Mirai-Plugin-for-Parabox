package com.ojhdtapp.miraipluginforparabox.ui.status

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.miraipluginforparabox.R
import com.ojhdtapp.miraipluginforparabox.core.MIRAI_CORE_VERSION
import com.ojhdtapp.miraipluginforparabox.core.util.DataStoreKeys
import com.ojhdtapp.miraipluginforparabox.core.util.dataStore
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.MiraiProtocol
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mamoe.mirai.utils.BotConfiguration
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class StatusPageViewModel @Inject constructor(
    private val repository: MainRepository,
    @ApplicationContext val context: Context,
) : ViewModel() {

    // emit to this when showing snackBar
    private val _uiEventFlow = MutableSharedFlow<StatusPageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()
    fun emitToUiEventFlow(event: StatusPageUiEvent) {
        viewModelScope.launch {
            _uiEventFlow.emit(event)
        }
    }

    // Login Resource
    private var _loginResourceStateFlow = MutableStateFlow<LoginResource>(LoginResource.None)
    val loginResourceStateFlow = _loginResourceStateFlow.asStateFlow()
    fun updateLoginResourceStateFlow(value: LoginResource) {
        _loginResourceStateFlow.tryEmit(value)
    }

    private val _picCaptchaValue = mutableStateOf<String>("")
    val picCaptchaValue: State<String> = _picCaptchaValue

    fun setPicCaptchaValue(value: String) {
        _picCaptchaValue.value = value
    }

    // MainApp Installation
    private val _isMainAppInstalled = mutableStateOf<Boolean>(false)
    val isMainAppInstalled: State<Boolean> = _isMainAppInstalled
    fun setIsMainAppInstalled(value: Boolean) {
        _isMainAppInstalled.value = value
    }

    private val _isMainAppOnStack = mutableStateOf<Boolean>(false)
    val isMainAppOnStack: State<Boolean> = _isMainAppOnStack
    fun setIsMainAppOnStack(value: Boolean) {
        _isMainAppOnStack.value = value
    }

    // Service Status
    private val _serviceStatusStateFlow = MutableStateFlow<ServiceStatus>(ServiceStatus.Stop)
    val serviceStatusStateFlow = _serviceStatusStateFlow.asStateFlow()
    fun updateServiceStatusStateFlow(value: ServiceStatus) {
        when(value){
            is ServiceStatus.Error, ServiceStatus.Stop -> {
                setMainSwitchState(false)
                setMainSwitchEnabledState(true)
            }
            is ServiceStatus.Running -> {
                setMainSwitchState(true)
                setMainSwitchEnabledState(true)
            }
            is ServiceStatus.Pause -> {
                setMainSwitchState(true)
                setMainSwitchEnabledState(false)
            }
            is ServiceStatus.Loading -> {
                setMainSwitchState(true)
                setMainSwitchEnabledState(false)
            }
        }
        _serviceStatusStateFlow.tryEmit(value)
    }

    // Account Dialog
    val accountFLow = repository.getAccountListFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedIndexFlow = repository.getAccountListFlow().mapLatest {
        it.indexOfFirst { secret -> secret.selected }
    }.onEach {
        // Update AutoLogin Switch
        if(it == -1){
            setAutoLoginSwitch(false)
        }
    }

    fun addNewAccount(secret: Secret) {
        viewModelScope.launch {
            repository.addNewAccount(secret)
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar(context.getString(R.string.account_added)))
        }
    }

    fun deleteAccount(secret: Secret) {
        viewModelScope.launch {
            repository.deleteAccount(secret)
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar(context.getString(R.string.account_deleted)))
        }
    }

    fun updateAccounts(selectedIndex: Int, accountList: List<Secret>) {
        viewModelScope.launch {
            repository.addAllAccounts(accountList.toList().mapIndexed { index, secret ->
                secret.apply {
                    selected = index == selectedIndex
                }
            })
            _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar(context.getString(R.string.changed_saved)))
        }
    }

    // Switches
    private val _mainSwitchEnabledState = mutableStateOf<Boolean>(true)
    val mainSwitchEnabledState: State<Boolean> = _mainSwitchEnabledState
    fun setMainSwitchEnabledState(value: Boolean) {
        _mainSwitchEnabledState.value = value
    }

    private val _mainSwitchState = mutableStateOf<Boolean>(false)
    val mainSwitchState: State<Boolean> = _mainSwitchState
    fun setMainSwitchState(value: Boolean) {
        _mainSwitchState.value = value
    }

    val autoLoginSwitchFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.AUTO_LOGIN] ?: false
        }

    fun setAutoLoginSwitch(value: Boolean) {
        viewModelScope.launch {
            val secret = withContext(Dispatchers.IO) {
                repository.getSelectedAccount()
            }
            if (secret == null) {
                context.dataStore.edit { settings ->
                    settings[DataStoreKeys.AUTO_LOGIN] = false
                }
                _uiEventFlow.emit(StatusPageUiEvent.ShowSnackBar(context.getString(R.string.automatic_login_account_not_selected_notice)))
            } else {
                context.dataStore.edit { settings ->
                    settings[DataStoreKeys.AUTO_LOGIN] = value
                }
            }
        }
    }

    val foregroundServiceSwitchFLow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.FOREGROUND_SERVICE] ?: true
        }

    fun setForegroundServiceSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.FOREGROUND_SERVICE] = value
            }
        }
    }

    val contactCacheSwitchFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CONTACT_CACHE] ?: true
        }

    fun setContactCacheSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.CONTACT_CACHE] = value
            }
        }
    }

    val protocolOptionsMap = mapOf<Int, String>(
        MiraiProtocol.Phone to context.getString(R.string.protocol_phone),
        MiraiProtocol.Pad to context.getString(R.string.protocol_tablet),
        MiraiProtocol.Watch to context.getString(R.string.protocol_watch)
    )
    val protocolSimpleMenuFLow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.PROTOCOL] ?: MiraiProtocol.Pad
        }

    fun setProtocolSimpleMenu(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.PROTOCOL] = value
            }
        }
    }

    val cancelTimeoutSwitchFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CANCEL_TIMEOUT] ?: false
        }

    fun setCancelTimeoutSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.CANCEL_TIMEOUT] = value
            }
        }
    }

    val appVersion = com.ojhdtapp.miraipluginforparabox.BuildConfig.VERSION_NAME
    val miraiCoreVersion = MIRAI_CORE_VERSION
}