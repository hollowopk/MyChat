package com.example.mychat.chat

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.leancloud.LCUser
import cn.leancloud.im.v2.*
import cn.leancloud.im.v2.callback.LCIMConversationCallback
import cn.leancloud.im.v2.callback.LCIMConversationCreatedCallback
import cn.leancloud.im.v2.callback.LCIMMessagesQueryCallback
import cn.leancloud.im.v2.messages.LCIMTextMessage
import com.example.mychat.database.AppDatabase
import com.example.mychat.database.Message
import com.example.mychat.showLog
import kotlin.concurrent.thread

class ChatUIViewModel(val context: Context, val friendName: String) : ViewModel() {

    val messages = MutableLiveData<ArrayList<Message>>()
    private val curUser = LCUser.getCurrentUser()
    private val client = LCIMClient.getInstance(curUser.username)
    private val tag = "ChatUIViewModel"

    init {
        setMessageHandler()
        loadAllMessages()
    }

    private fun loadAllMessages() {
        /*
        val appDatabase = AppDatabase.getDatabase(context)
        val messageDao = appDatabase.message()
        val messageList = messageDao.loadAllMessagesByName(curUser.username,friendName)
        val messageArrayList = messages.value
        for (message in messageList) {
            messageArrayList?.add(message)
        }
        var lastTimestamp : Long = 0
        if (!messageArrayList.isNullOrEmpty()) {
            lastTimestamp = messageArrayList.last().timestamp
        }
         */
        messages.value = ArrayList()
        client.createConversation(listOf(friendName),"${curUser.username} & $friendName",
                                    null, false, true,object :
            LCIMConversationCreatedCallback() {
            override fun done(conversation: LCIMConversation?, e: LCIMException?) {
                conversation?.queryMessages(object : LCIMMessagesQueryCallback() {
                    override fun done(mList: MutableList<LCIMMessage>?, e: LCIMException?) {
                        if (e == null && mList != null) {
                            for (message in mList) {
                                val textMessage = message as LCIMTextMessage
                                when (message.from) {
                                    curUser.username -> {
                                        val curMessage = Message(curUser.username,
                                            friendName,textMessage.text,message.timestamp)
                                        storeMessage(curMessage)
                                    }
                                    friendName -> {
                                        val curMessage = Message(friendName,
                                            curUser.username,textMessage.text,message.timestamp)
                                        storeMessage(curMessage)
                                    }
                                }
                            }
                        }
                    }

                })
            }
        })
    }

    private fun setMessageHandler() {
        LCIMMessageManager.registerDefaultMessageHandler(object : LCIMMessageHandler() {
            override fun onMessage(
                message: LCIMMessage,
                conversation: LCIMConversation?,
                client: LCIMClient?
            ) {
                val senderName = message.from
                val textMsg = message as LCIMTextMessage
                val curMsg = Message(senderName, curUser.username,
                                    textMsg.text, message.timestamp)
                storeMessage(curMsg)
            }
        })
    }


    fun sendMessage(content: String) {
        client.createConversation(listOf(friendName),"${curUser.username} & $friendName",
                            null, false, true,object :
                                    LCIMConversationCreatedCallback() {
                override fun done(conversation: LCIMConversation?, e: LCIMException?) {
                    val message = LCIMTextMessage()
                    message.text = content
                    val msg = Message(curUser.username,friendName,content,System.currentTimeMillis())
                    conversation?.sendMessage(message,object : LCIMConversationCallback() {
                        override fun done(e: LCIMException?) {
                            "4".showLog(tag)
                            storeMessage(msg)
                        }
                    })
                }
            })
    }

    fun storeMessage(message: Message) {
        val appDatabase = AppDatabase.getDatabase(context)
        val messageDao = appDatabase.message()
        "store message : ${message.content}".showLog(tag)
        thread {
            messageDao.insertMessage(message)
            "message store".showLog(tag)
        }
        refreshMessage(message)
    }

    private fun refreshMessage(message: Message) {
        "curmessage size ${messages.value?.size}".showLog(tag)
        val messageList = messages.value
        messageList?.add(message)
        messages.value = messageList!!
        "nowmessage size ${messages.value?.size}".showLog(tag)
    }

}