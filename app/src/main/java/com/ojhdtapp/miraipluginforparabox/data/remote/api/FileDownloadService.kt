package com.ojhdtapp.miraipluginforparabox.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface FileDownloadService {

    @Streaming
    @GET
    suspend fun download(@Url url: String): Response<ResponseBody>
}