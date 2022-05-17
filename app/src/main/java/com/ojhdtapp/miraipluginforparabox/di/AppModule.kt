package com.ojhdtapp.miraipluginforparabox.di

import android.app.Application
import androidx.room.Room
import com.ojhdtapp.miraipluginforparabox.data.local.entity.SecretsDatabase
import com.ojhdtapp.miraipluginforparabox.data.repository.MainRepositoryImpl
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideSecretsDatabase(app: Application): SecretsDatabase =
        Room.databaseBuilder(
            app, SecretsDatabase::class.java,
            "secret_db"
        ).build()

    @Provides
    @Singleton
    fun provideMainRepository(database: SecretsDatabase) : MainRepository =
        MainRepositoryImpl(database.dao)

}