package com.bxt.di

import com.bxt.data.api.ApiService
import com.bxt.data.api.AuthInterceptor
import com.bxt.data.api.TokenAuthenticator
import com.bxt.data.local.DataStoreManager
import com.bxt.ui.components.InstantAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://10.0.2.2:8080/api/"
    private const val TIMEOUT = 10L


    @Provides
    @Singleton
    @Named("BaseUrl")
    fun provideBaseUrl(): String = "http://10.0.2.2:8080/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, tokenAuthenticator: TokenAuthenticator): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantAdapter())
            .create()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    @Provides
    @Singleton
    fun provideAuthInterceptor(dataStoreManager: DataStoreManager): AuthInterceptor {
        return AuthInterceptor(dataStoreManager)
    }
}