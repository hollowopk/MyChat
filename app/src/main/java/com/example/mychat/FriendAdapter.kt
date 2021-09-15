package com.example.mychat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mychat.database.Friend
import com.example.mychat.ui.friend.FriendFragment

class FriendAdapter(private val fragment: FriendFragment,
                             private val friendList: ArrayList<Pair<Friend,Boolean>>,
                                private val itemListener: ItemListener) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>(){

    private val myTag = "FriendAdapter"
    private lateinit var context: Context

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val friendsName: TextView = view.findViewById(R.id.friendsName)
        val friendsAvatar: ImageView = view.findViewById(R.id.friendsAvatar)
        val unreadPoint: ImageView = view.findViewById(R.id.unreadPoint)
    }

    interface ItemListener {
        fun onItemClick(position: Int, friend: Pair<Friend,Boolean>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_item,parent,false)
        context = parent.context
        val holder = ViewHolder(view)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            val friend = friendList[position]
            itemListener.onItemClick(position,friend)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friendList[position]
        holder.friendsAvatar.setImageResource(friend.first.friendAvatar)
        holder.friendsName.text = friend.first.friendName
        if (friend.second) {
            holder.unreadPoint.visibility = View.VISIBLE
            "visible".showLog(myTag)
        }
        else {
            holder.unreadPoint.visibility = View.GONE
        }
    }

    override fun getItemCount() = friendList.size

}