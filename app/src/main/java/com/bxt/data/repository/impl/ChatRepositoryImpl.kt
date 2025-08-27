package com.bxt.data.repository.impl

import android.util.Log
import com.bxt.data.repository.ChatRepository
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val database by lazy { FirebaseDatabase.getInstance().reference }

    override fun sendMessage(
        myUserId: String,
        otherUserId: String,
        messageMap: Map<String, Any?>
    ) {
        val newKey = database.child("users").child(myUserId)
            .child("chats").child(otherUserId)
            .child("messages").push().key ?: return

        val lastText = messageMap["text"] as? String ?: "Đã gửi một đối tượng đính kèm"
        val ts = (messageMap["timestamp"] as? Long) ?: System.currentTimeMillis()

        val myBase    = "/users/$myUserId/chats/$otherUserId"
        val otherBase = "/users/$otherUserId/chats/$myUserId"

        // Tất cả path đều là "lá", không có path nào là tổ tiên của path khác
        val updates = hashMapOf<String, Any>(
            "$myBase/messages/$newKey" to messageMap,
            "$otherBase/messages/$newKey" to messageMap,

            "$myBase/lastMessage" to lastText,
            "$myBase/timestamp"   to ts,

            "$otherBase/lastMessage" to lastText,
            "$otherBase/timestamp"   to ts
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

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val map = snapshot.value as? Map<String, Any?> ?: return
                onNewMessage(map)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatRepository", "listenForMessages:onCancelled", error.toException())
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
                    val value = child.value as? Map<String, Any?> ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    result[otherId] = value as Map<String, Any?>
                }
                onResult(result)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatRepository", "getChatList:onCancelled", error.toException())
                onResult(emptyMap())
            }
        })
    }
}
