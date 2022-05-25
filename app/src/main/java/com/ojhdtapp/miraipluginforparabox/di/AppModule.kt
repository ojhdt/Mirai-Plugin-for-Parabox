package com.ojhdtapp.miraipluginforparabox.di

import android.app.Application
import androidx.room.Room
import com.ojhdtapp.miraipluginforparabox.data.local.SecretDatabase
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
    fun provideSecretsDatabase(app: Application): SecretDatabase =
        Room.databaseBuilder(
            app, SecretDatabase::class.java,
            "secret_db"
        ).build()

    @Provides
    @Singleton
    fun provideMainRepository(database: SecretDatabase) : MainRepository =
        MainRepositoryImpl(database.dao)

//    @Provides
//    @Singleton
//    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

}