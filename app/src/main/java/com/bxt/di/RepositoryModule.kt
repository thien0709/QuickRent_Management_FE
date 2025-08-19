package com.bxt.di

import android.content.Context
import com.bxt.data.api.ApiCallExecutor // Import lớp mới
import com.bxt.data.api.ApiService
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.AuthRepository
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.data.repository.UserRepository
import com.bxt.data.repository.impl.* // Import tất cả các implementation
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
    fun provideAuthRepo(
        apiService: ApiService,
        apiCallExecutor: ApiCallExecutor,
        dataStoreManager: DataStoreManager
    ): AuthRepository {
        return AuthRepositoryImpl(apiService, apiCallExecutor, dataStoreManager)
    }

    @Singleton
    @Provides
    fun provideUserRepo(
        apiService: ApiService,
        apiCallExecutor: ApiCallExecutor
    ): UserRepository {
        return UserRepositoryImpl(apiService, apiCallExecutor)
    }

    @Singleton
    @Provides
    fun provideLocationRepo(
        apiService: ApiService,
        apiCallExecutor: ApiCallExecutor,
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepositoryImpl(apiService, apiCallExecutor, context)
    }

    @Singleton
    @Provides
    fun provideCategoryRepo(
        apiService: ApiService,
        apiCallExecutor: ApiCallExecutor
    ): CategoryRepository {
        return CategoryRepositoryImpl(apiService, apiCallExecutor)
    }

    @Singleton
    @Provides
    fun provideItemRepo(
        apiService: ApiService,
        apiCallExecutor: ApiCallExecutor
    ): ItemRepository {
        return ItemRepositoryImpl(apiService, apiCallExecutor)
    }

    @Singleton
    @Provides
    fun provideRentalRequestRepository(
        apiService: ApiService,
        apiCallExecutor: ApiCallExecutor
    ): RentalRequestRepository {
        return RentalRequestRepositoryImpl(apiService, apiCallExecutor)
    }
}