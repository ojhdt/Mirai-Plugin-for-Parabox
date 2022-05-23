package com.ojhdtapp.miraipluginforparabox.ui.status

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.miraipluginforparabox.core.util.DataStoreKeys
import com.ojhdtapp.miraipluginforparabox.core.util.dataStore
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.MiraiProtocol
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    // Service Status
    private val _serviceStatusStateFlow = MutableStateFlow<ServiceStatus>(ServiceStatus.Stop)
    val serviceStatusStateFlow = _serviceStatusStateFlow.asStateFlow()
    fun updateServiceStatusStateFlow(value: ServiceStatus) {
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
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.AUTO_LOGIN] = value
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
        settings[DataStoreKeys.FOREGROUND_SERVICE] ?: false
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
        settings[DataStoreKeys.CONTACT_CACHE] ?: false
    }

    fun setContactCacheSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.CONTACT_CACHE] = value
            }
        }
    }

    val protocolOptionsMap = mapOf<Int, String>(
        MiraiProtocol.Phone to "Android 手机（默认）",
        MiraiProtocol.Pad to "Android 平板",
        MiraiProtocol.Watch to "Android 手表"
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
        settings[DataStoreKeys.PROTOCOL] ?: MiraiProtocol.Phone
    }

    fun setProtocolSimpleMenu(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.PROTOCOL] = value
            }
        }
    }

    val cancelTimeoutSwitchFlow : Flow<Boolean> = context.dataStore.data
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

    fun setCancelTimeoutSwitch(value: Boolean){
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.CANCEL_TIMEOUT] = value
            }
        }
    }

    val appVersion = com.ojhdtapp.miraipluginforparabox.BuildConfig.VERSION_NAME
}