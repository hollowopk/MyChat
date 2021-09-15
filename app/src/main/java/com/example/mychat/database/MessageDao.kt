package com.example.mychat.database

import androidx.room.*

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: Message): Long

    @Delete
    fun deleteMessage(message: Message)

    @Query("select * from Message " +
            "where (senderName=:senderName and receiverName=:receiverName) " +
            "or (senderName=:receiverName and receiverName=:senderName) " +
            "order by messageId")
    fun loadAllMessagesByName(senderName: String, receiverName: String): List<Message>

    @Query("delete from Message where (senderName=:senderName and receiverName=:receiverName) or (senderName=:receiverName and receiverName=:senderName)")
    fun deleteMessageByUserId(senderName: String, receiverName: String)

    @Query("delete from Message where messageId=:messageId")
    fun deleteMessageByMessageId(messageId: Long)

}