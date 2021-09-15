package com.example.mychat.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(var senderName: String, var receiverName: String,
                   var content: String, var timestamp: Long) {

    @PrimaryKey(autoGenerate = true)
    var messageId: Long = 0

}