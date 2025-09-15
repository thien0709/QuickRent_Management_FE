package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.FcmRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class FcmRegistrationViewModel @Inject constructor(
    private val fcmRepo: FcmRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    fun registerIfLoggedIn() {
        viewModelScope.launch {
            val userId = dataStore.userId.firstOrNull()
            if (userId == null) return@launch

            runCatching {
                val token = getFcmToken()
                fcmRepo.registerToken(token, userId)
            }
        }
    }

    private suspend fun getFcmToken(): String = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}
