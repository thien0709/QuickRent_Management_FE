package com.bxt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.navigation.AppNavigation
import com.bxt.ui.components.FcmRegistrationGate
import com.bxt.ui.components.NotificationPermissionGate
import com.bxt.ui.theme.QuickRent_Management_FETheme
import com.bxt.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private fun extractRouteFromIntent(intent: Intent): String? {
        intent.getStringExtra("deeplink_route")?.let { return it }
        intent.data?.let { uri ->
            if (uri.scheme == "quickrent") {
                val host = uri.host ?: return null
                val path = uri.path ?: ""
                return (host + path).removePrefix("/")
            }
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialRoute = extractRouteFromIntent(intent)

        enableEdgeToEdge()
        setContent {
            val homeViewModel: HomeViewModel = hiltViewModel()
            val isDarkMode by homeViewModel.isDarkModeEnabled.collectAsState()

            NotificationPermissionGate()
            FcmRegistrationGate()

            QuickRent_Management_FETheme(darkTheme = isDarkMode) {
                AppNavigation(initialDeeplinkRoute = initialRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
