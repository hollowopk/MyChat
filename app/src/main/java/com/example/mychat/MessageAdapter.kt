package com.example.mychat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.leancloud.LCUser
import com.example.mychat.database.Message

class MessageAdapter(private val messageList: ArrayList<Message>,
                        private val friendAvatar: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val myTag = "MessageAdapter"
    private val MESSAGE_SENT = 0
    private val MESSAGE_RECEIVED = 1
    private lateinit var context: Context
    private val curUser = LCUser.getCurrentUser()

    init {

    }

    inner class LeftViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val leftMsg: TextView = view.findViewById(R.id.leftMsg)
        val leftAvatar: ImageView = view.findViewById(R.id.leftAvatar)
    }

    inner class RightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rightMsg: TextView = view.findViewById(R.id.rightMsg)
        val rightAvatar: ImageView = view.findViewById(R.id.rightAvatar)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        if(message.senderName == curUser.username) {
            return MESSAGE_SENT
        }
        else {
            return MESSAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        if (viewType == MESSAGE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_right_item,parent,false)
            return RightViewHolder(view)
        }
        else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_left_item,parent,false)
            return LeftViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        when (holder) {
            is LeftViewHolder -> {
                holder.leftMsg.text = message.content
                holder.leftAvatar.setImageResource(friendAvatar)
            }
            is RightViewHolder -> {
                holder.rightMsg.text = message.content
                holder.rightAvatar.setImageResource(curUser.getInt("userAvatar"))
            }
        }
    }

    override fun getItemCount() = messageList.size

}