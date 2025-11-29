package com.bxt.data.repository.impl

import android.util.Log
import com.bxt.data.repository.ChatRepository
import com.google.firebase.database.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val database = FirebaseDatabase.getInstance().reference

    override fun getInitialMessages(
        myUserId: String,
        otherUserId: String,
        onResult: (List<Map<String, Any?>>) -> Unit
    ) {
        database.child("users").child(myUserId)
            .child("chats").child(otherUserId).child("messages")
            .orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { it.value as? Map<String, Any?> }
                    onResult(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("ChatRepositoryImpl", "getInitialMessages cancelled", error.toException())
                    onResult(emptyList())
                }
            })
    }

    override fun sendMessage(
        myUserId: String,
        otherUserId: String,
        messageMap: Map<String, Any?>
    ) {
        val newKey = database.child("users").child(myUserId)
            .child("chats").child(otherUserId)
            .child("messages").push().key ?: return

        val lastText = messageMap["text"] as? String ?: (if (messageMap.containsKey("attachable")) "Đã gửi một sản phẩm" else "Tin nhắn")
        val timestamp = messageMap["timestamp"] as? Long ?: System.currentTimeMillis()

        val updates = mapOf(
            "/users/$myUserId/chats/$otherUserId/messages/$newKey" to messageMap,
            "/users/$otherUserId/chats/$myUserId/messages/$newKey" to messageMap,
            "/users/$myUserId/chats/$otherUserId/lastMessage" to lastText,
            "/users/$myUserId/chats/$otherUserId/timestamp" to timestamp,
            "/users/$otherUserId/chats/$myUserId/lastMessage" to lastText,
            "/users/$otherUserId/chats/$myUserId/timestamp" to timestamp
        )

        database.updateChildren(updates)
    }

    override fun listenForMessages(
        myUserId: String,
        otherUserId: String,
        onNewMessage: (Map<String, Any?>) -> Unit
    ): ChildEventListener {
        val messagesRef = database.child("users").child(myUserId)
            .child("chats").child(otherUserId).child("messages")
            .orderByChild("timestamp")
            .startAt((System.currentTimeMillis()).toDouble())

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val messageMap = snapshot.value as? Map<String, Any?> ?: return
                onNewMessage(messageMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatRepository", "Listen cancelled", error.toException())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }

        messagesRef.addChildEventListener(listener)
        return listener
    }


    override fun removeMessagesListener(
        myUserId: String,
        otherUserId: String,
        listener: ChildEventListener
    ) {
        database.child("users").child(myUserId)
            .child("chats").child(otherUserId).child("messages")
            .removeEventListener(listener)
    }

    override fun getChatList(
        myUserId: String,
        onResult: (Map<String, Map<String, Any?>>) -> Unit
    ) {
        val chatsRef = database.child("users").child(myUserId).child("chats")

        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableMapOf<String, Map<String, Any?>>()

                for (child in snapshot.children) {
                    val otherId = child.key ?: continue
                    val chatData = child.value as? Map<String, Any?> ?: emptyMap()
                    result[otherId] = chatData
                }

                onResult(result)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatRepository", "Chat list cancelled", error.toException())
                onResult(emptyMap())
            }
        })
    }
}