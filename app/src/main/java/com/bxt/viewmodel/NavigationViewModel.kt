//package com.bxt.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.bxt.data.local.DataStoreManager
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//sealed class NavigationState {
//    object Loading : NavigationState()
//    data class Ready(val startDestination: String) : NavigationState()
//}
//
//@HiltViewModel
//class NavigationViewModel @Inject constructor(
//    private val dataStoreManager: DataStoreManager
//) : ViewModel() {
//
//    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
//    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
//
//    init {
//        determineStartDestination()
//    }
//
//    private fun determineStartDestination() {
//        viewModelScope.launch {
//            dataStoreManager.isFirstTimeFlow.combine(dataStoreManager.isLoggedIn) { isFirstTime, isLoggedIn ->
//                when {
//                    isFirstTime -> "welcome"
//                    isLoggedIn -> "home"
//                    else -> "login"
//                }
//            }.collect { destination ->
//                _navigationState.value = NavigationState.Ready(destination)
//            }
//        }
//    }
//
//    // Method để reset về welcome screen nếu cần
//    suspend fun resetToWelcome() {
//        dataStoreManager.clear()
//        // Có thể thêm logic reset first time nếu cần
//    }
//
//    // Method để logout và quay về login
//    suspend fun logout() {
//        dataStoreManager.clear()
//        _navigationState.value = NavigationState.Ready("login")
//    }
//
//    // Method để check current state
//    fun getCurrentDestination(): String? {
//        return when (val state = _navigationState.value) {
//            is NavigationState.Ready -> state.startDestination
//            is NavigationState.Loading -> null
//        }
//    }
//}