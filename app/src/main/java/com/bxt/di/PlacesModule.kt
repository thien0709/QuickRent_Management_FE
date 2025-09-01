//// di/PlacesModule.kt
//package com.bxt.di
//
//import android.content.Context
//import com.google.android.libraries.places.api.Places
//import com.google.android.libraries.places.api.net.PlacesClient
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object PlacesModule {
//
//    @Provides
//    @Singleton
//    fun providePlacesClient(@ApplicationContext context: Context): PlacesClient {
//        // Thay thế KEY của bạn vào đây hoặc lấy từ BuildConfig
//        val apiKey = "YOUR_API_KEY"
//
//        if (!Places.isInitialized()) {
//            Places.initialize(context, apiKey)
//        }
//        return Places.createClient(context)
//    }
//}