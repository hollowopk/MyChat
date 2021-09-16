package com.example.mychat.chat

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.leancloud.LCUser
import cn.leancloud.im.v2.*
import cn.leancloud.im.v2.callback.LCIMConversationCallback
import cn.leancloud.im.v2.callback.LCIMConversationCreatedCallback
import cn.leancloud.im.v2.callback.LCIMConversationQueryCallback
import cn.leancloud.im.v2.callback.LCIMMessagesQueryCallback
import cn.leancloud.im.v2.messages.LCIMTextMessage
import com.example.mychat.database.Message
import com.example.mychat.ui.friend.FriendFragment

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
        messages.value = ArrayList()
        val query1 = client.conversationsQuery
        val query2 = client.conversationsQuery
        query1.whereEqualTo("name","${curUser.username} & $friendName")
        query2.whereEqualTo("name","$friendName & ${curUser.username}")
        val orQuery = LCIMConversationsQuery.or(listOf(query1,query2))
        orQuery.findInBackground(object : LCIMConversationQueryCallback() {
            override fun done(conversations: MutableList<LCIMConversation>?, e: LCIMException?) {
                if (e == null && !conversations.isNullOrEmpty()) {
                    if (conversations.size == 1) {
                        val conversation = conversations[0]
                        conversation.queryMessages(object : LCIMMessagesQueryCallback() {
                            override fun done(mList: MutableList<LCIMMessage>?, e: LCIMException?) {
                                if (e == null && mList != null) {
                                    for (message in mList) {
                                        val textMessage = message as LCIMTextMessage
                                        when (message.from) {
                                            curUser.username -> {
                                                val curMessage = Message(curUser.username,
                                                    friendName,textMessage.text,message.timestamp)
                                                refreshMessage(curMessage)
                                            }
                                            friendName -> {
                                                val curMessage = Message(friendName,
                                                    curUser.username,textMessage.text,message.timestamp)
                                                refreshMessage(curMessage)
                                            }
                                        }
                                    }
                                }
                            }

                        })
                    }
                }
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
                refreshMessage(curMsg)
                FriendFragment.updateLastContactTime(friendName,System.currentTimeMillis())
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
                    conversation?.sendMessage(message,object : LCIMConversationCallback() {
                        override fun done(e: LCIMException?) {
                            refreshMessage(Message(curUser.username,friendName,content,System.currentTimeMillis()))
                            FriendFragment.updateLastContactTime(friendName,System.currentTimeMillis())
                        }
                    })
                }
            })
    }

    private fun refreshMessage(message: Message) {
        val messageList = messages.value!!
        messageList.add(message)
        messages.value = messageList
    }

}