package com.bxt.di

import com.bxt.data.api.ApiService
import com.bxt.data.repository.AuthRepository
import com.bxt.data.repository.UserRepository
import com.bxt.data.repository.impl.AuthRepositoryImpl
import com.bxt.data.repository.impl.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideAuthRepo(apiService: ApiService): AuthRepository {
        return AuthRepositoryImpl(apiService)
    }

    @Singleton
    @Provides
    fun provideUserRepo(apiService: ApiService): UserRepository {
        return UserRepositoryImpl(apiService)
    }


}