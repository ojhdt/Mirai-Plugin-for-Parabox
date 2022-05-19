package com.ojhdtapp.miraipluginforparabox.domain.service

import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus

interface ConnCommandInterface {

    fun miraiStart()

    fun miraiStop()

    fun miraiLogin()

    fun onLoginStateChanged(resource: LoginResource)

    fun submitVerificationResult(result: String)
}