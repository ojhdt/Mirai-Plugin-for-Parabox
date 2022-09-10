package com.ojhdtapp.miraipluginforparabox.di

import android.app.Application
import androidx.room.Room
import com.ojhdtapp.miraipluginforparabox.data.local.AppDatabase
import com.ojhdtapp.miraipluginforparabox.data.remote.api.FileDownloadService
import com.ojhdtapp.miraipluginforparabox.data.repository.MainRepositoryImpl
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(
            app, AppDatabase::class.java,
            "main_db"
        ).build()

    @Provides
    @Singleton
    fun provideMainRepository(database: AppDatabase) : MainRepository =
        MainRepositoryImpl(database.secretDao, database.deviceInfoDao, database.miraiMessageDao)

//    @Provides
//    @Singleton
//    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideRetrofit() : Retrofit
    = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideFileDownloadService(retrofit: Retrofit): FileDownloadService
    = retrofit.create(FileDownloadService::class.java)

}