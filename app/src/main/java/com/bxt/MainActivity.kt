package com.bxt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.navigation.AppNavigation
import com.bxt.ui.theme.QuickRent_Management_FETheme
import com.bxt.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val homeViewModel: HomeViewModel = hiltViewModel()
            val isDarkMode by homeViewModel.isDarkModeEnabled.collectAsState()

            QuickRent_Management_FETheme(darkTheme = isDarkMode) {
                AppNavigation()
            }
        }
    }
}
