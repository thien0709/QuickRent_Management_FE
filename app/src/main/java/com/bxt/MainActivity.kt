package com.bxt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bxt.navigation.AppNavigation
import com.bxt.ui.theme.QuickRent_Management_FETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickRent_Management_FETheme {
                AppNavigation()
            }
        }
    }
}