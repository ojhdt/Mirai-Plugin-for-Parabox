package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Context
import android.graphics.BitmapFactory
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.LoginSolver


class AndroidLoginSolver() : LoginSolver() {
    lateinit var verificationResult: CompletableDeferred<String>
//    lateinit var captchaData: ByteArray
//    lateinit var url: String
    private val loginResourceStateFlow = MutableStateFlow<LoginResource>(LoginResource.None)

    fun getLoginResourceStateFlow() = loginResourceStateFlow

    fun submitVerificationResult(result: String) {
        verificationResult.complete(result)
    }

    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String {
        verificationResult = CompletableDeferred()
//        captchaData = data
        val bm = BitmapFactory.decodeByteArray(data, 0, data.size)
        loginResourceStateFlow.emit(LoginResource.PicCaptcha(bm))
        return verificationResult.await()
    }

    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
        verificationResult = CompletableDeferred()
//        this.url = url
        loginResourceStateFlow.emit(LoginResource.SliderCaptcha(url))
        return verificationResult.await()
    }

    override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
        verificationResult = CompletableDeferred()
//        this.url = url
        loginResourceStateFlow.emit(LoginResource.UnsafeDeviceLoginVerify(url))
        return verificationResult.await()
    }

    override val isSliderCaptchaSupported: Boolean
        get() = true

}

