package com.ojhdtapp.miraipluginforparabox.di

import android.app.Application
import androidx.room.Room
import com.ojhdtapp.miraipluginforparabox.data.local.AppDatabase
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
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(
            app, AppDatabase::class.java,
            "main_db"
        ).build()

    @Provides
    @Singleton
    fun provideMainRepository(database: AppDatabase) : MainRepository =
        MainRepositoryImpl(database.secretDao, database.deviceInfoDao)

//    @Provides
//    @Singleton
//    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

}