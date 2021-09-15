package com.example.mychat.ui.friend

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import cn.leancloud.LCFriendshipRequest
import cn.leancloud.LCQuery
import cn.leancloud.LCUser
import cn.leancloud.im.v2.*
import com.example.mychat.FriendAdapter
import com.example.mychat.R
import com.example.mychat.chat.ChatUI
import com.example.mychat.database.Friend
import com.example.mychat.databinding.FriendFragmentBinding
import com.example.mychat.showLog
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class FriendFragment : Fragment() {

    private val myTag = "FriendFragment"

    private lateinit var viewModel: FriendViewModel
    private lateinit var binding: FriendFragmentBinding

    companion object {
        private var curUser: LCUser = LCUser.currentUser()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this,FriendViewModelFactory(requireActivity())).get(FriendViewModel::class.java)
        binding = FriendFragmentBinding.inflate(layoutInflater,null,false)
        setMessageHandler()

        val friendRecyclerView = binding.friendRecyclerView
        val addFriendButton = binding.addFriendButton

        viewModel.friends.observe(viewLifecycleOwner,{ result ->
            if (result != null) {
                val layoutManager = LinearLayoutManager(activity)
                friendRecyclerView.layoutManager = layoutManager
                val adapter = FriendAdapter(this,result,object : FriendAdapter.ItemListener {
                    override fun onItemClick(position: Int, friend: Pair<Friend, Boolean>) {
                        val intent = Intent(activity, ChatUI::class.java)
                        intent.putExtra("friendName",friend.first.friendName)
                        intent.putExtra("friendAvatar",friend.first.friendAvatar)
                        startActivity(intent)
                        viewModel.notifyMessageRead(friend.first.friendName)
                    }
                })
                friendRecyclerView.adapter = adapter
            }
        })
        viewModel.friendshipRequests.observe(viewLifecycleOwner, { result ->
            if (result != null) {
                for (request in result) {
                    askForAcceptance(request)
                }
            }
        })

        addFriendButton.setOnClickListener {
            val editText = EditText(activity)
            AlertDialog.Builder(activity).apply {
                setTitle("请输入用户名")
                setView(editText)
                setPositiveButton("OK"){_,_->
                    val friendName = editText.text.toString()
                    if(friendName.isNotEmpty()) {
                        viewModel.sendFriendshipRequestByName(friendName)
                    }
                }
                show()
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setMessageHandler()
    }

    private fun setMessageHandler() {
        LCIMMessageManager.setConversationEventHandler(object : LCIMConversationEventHandler() {
            override fun onMemberLeft(
                client: LCIMClient?,
                conversation: LCIMConversation?,
                members: MutableList<String>?,
                kickedBy: String?
            ) {}
            override fun onMemberJoined(
                client: LCIMClient?,
                conversation: LCIMConversation?,
                members: MutableList<String>?,
                invitedBy: String?
            ) {}
            override fun onKicked(
                client: LCIMClient?,
                conversation: LCIMConversation?,
                kickedBy: String?
            ) {}
            override fun onInvited(
                client: LCIMClient?,
                conversation: LCIMConversation?,
                operator: String?
            ) {
                "invite by operator: $operator".showLog(myTag)
            }

        })
        LCIMMessageManager.registerDefaultMessageHandler(object : LCIMMessageHandler() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onMessage(
                message: LCIMMessage,
                conversation: LCIMConversation?,
                client: LCIMClient?
            ) {
                addNotification(message.from)
                viewModel.notifyMessageUnread(message.from)
            }
        })
    }

    private fun askForAcceptance(request: LCFriendshipRequest) {
        val query = LCQuery<LCUser>("_User")
        query.whereEqualTo(LCUser.KEY_OBJECT_ID,request.sourceUser.objectId)
        query.findInBackground().subscribe(object : Observer<List<LCUser>> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: List<LCUser>) {
                if (t.isNotEmpty()) {
                    val srcUsername = t[0].username
                    val srcUserAvatar = t[0].getInt("userAvatar")
                    AlertDialog.Builder(activity).apply {
                        setTitle("是否通过申请")
                        setMessage("用户 $srcUsername 请求添加你为好友")
                        setPositiveButton("Accept") { _, _ ->
                            curUser.acceptFriendshipRequest(request, null)
                                .subscribe(object : Observer<LCFriendshipRequest> {
                                    override fun onSubscribe(d: Disposable) {}
                                    override fun onError(e: Throwable) {}
                                    override fun onComplete() {}
                                    override fun onNext(t: LCFriendshipRequest) {
                                        viewModel.refreshFriendList(srcUsername,srcUserAvatar)
                                    }
                                })
                        }
                        setNegativeButton("Decline") { _, _ ->
                            curUser.declineFriendshipRequest(request)
                                .subscribe(object : Observer<LCFriendshipRequest> {
                                    override fun onSubscribe(d: Disposable) {}
                                    override fun onNext(t: LCFriendshipRequest) {}
                                    override fun onError(e: Throwable) {}
                                    override fun onComplete() {}
                                })
                        }
                        show()
                    }

                }
            }
            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addNotification(friendName: String) {
        val manager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("messageArrive","消息提醒",NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(requireActivity(),"messageArrive")
            .setContentTitle("MyChat")
            .setContentText("你有一条来自${friendName}的新消息")
            .setSmallIcon(R.drawable.small_icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources,R.drawable.large_icon))
            .build()
        manager.notify(System.currentTimeMillis().toInt(),notification)
    }

}