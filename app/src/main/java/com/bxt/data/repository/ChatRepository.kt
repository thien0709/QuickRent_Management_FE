package com.bxt.data.repository

import com.google.firebase.database.ChildEventListener

interface ChatRepository {
    fun sendMessage(
        myUserId: String,
        otherUserId: String,
        messageMap: Map<String, Any?>
    )

    fun getInitialMessages(
        myUserId: String,
        otherUserId: String,
        onResult: (List<Map<String, Any?>>) -> Unit
    )

    fun listenForMessages(
        myUserId: String,
        otherUserId: String,
        onNewMessage: (Map<String, Any?>) -> Unit
    ): ChildEventListener

    fun removeMessagesListener(
        myUserId: String,
        otherUserId: String,
        listener: ChildEventListener
    )

    fun getChatList(
        myUserId: String,
        onResult: (Map<String, Map<String, Any?>>) -> Unit
    )
}