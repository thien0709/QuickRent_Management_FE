package com.bxt // Sửa package thành com.bxt (gốc)

import android.app.Application
import com.bxt.ui.components.FcmRegistrationGate
import com.bxt.util.NotificationHelper
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuickRentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationHelper.createChannels(this)
    }
}