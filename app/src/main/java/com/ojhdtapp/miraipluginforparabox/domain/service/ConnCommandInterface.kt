package com.ojhdtapp.miraipluginforparabox.domain.service

import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus

interface ConnCommandInterface {

    suspend fun miraiStart() : ServiceStatus

    suspend fun miraiStop() : ServiceStatus

    suspend fun miraiLogin() : ServiceStatus

    fun onLoginStateChanged(resource: LoginResource)

    suspend fun submitVerificationResult(result: String): ServiceStatus
}