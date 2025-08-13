package com.bxt.di

import android.content.Context
import com.bxt.data.api.ApiService
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.AuthRepository
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.UserRepository
import com.bxt.data.repository.impl.AuthRepositoryImpl
import com.bxt.data.repository.impl.CategoryRepositoryImpl
import com.bxt.data.repository.impl.ItemRepositoryImpl
import com.bxt.data.repository.impl.LocationRepositoryImpl
import com.bxt.data.repository.impl.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Singleton
    @Provides
    fun provideLocationRepo(
        apiService: ApiService,
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepositoryImpl(apiService ,context)
    }

    @Singleton
    @Provides
    fun provideCategoryRepo(
        apiService: ApiService,
    ): CategoryRepository {
        return CategoryRepositoryImpl(apiService)
    }

    @Singleton
    @Provides
    fun provideItemRepo(
        apiService: ApiService,
    ): ItemRepository {
        return ItemRepositoryImpl(apiService)
    }
}