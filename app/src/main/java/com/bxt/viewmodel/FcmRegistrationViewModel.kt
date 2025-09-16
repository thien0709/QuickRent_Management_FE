package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.FcmRepository
import com.google.android.gms.tasks.Task
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
            val userId = dataStore.userId.firstOrNull() ?: return@launch
            runCatching {
                val token = getFcmToken()
                fcmRepo.registerToken(token, userId)
            }
        }
    }


    fun cleanupOnLogout() {
        viewModelScope.launch {
            val token = runCatching { getFcmToken() }.getOrNull()
            runCatching { deleteFcmToken() }          // xóa token local
            if (!token.isNullOrBlank()) {
                runCatching { fcmRepo.unregisterToken(token) } // gỡ trên server
            }
        }
    }


    private suspend fun getFcmToken(): String = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private suspend fun deleteFcmToken(): Unit = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().deleteToken()
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}
