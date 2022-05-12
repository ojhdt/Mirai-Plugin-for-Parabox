package com.ojhdtapp.miraipluginforparabox.di

import android.app.Application
import androidx.room.Room
import com.ojhdtapp.miraipluginforparabox.data.local.entity.SecretsDatabase
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
}