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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import cn.leancloud.*
import cn.leancloud.im.v2.*
import cn.leancloud.livequery.LCLiveQuery
import cn.leancloud.livequery.LCLiveQueryEventHandler
import cn.leancloud.livequery.LCLiveQuerySubscribeCallback
import com.example.mychat.BottomNavigationViewModel
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
    private val activityViewModel : BottomNavigationViewModel by activityViewModels()

    companion object {
        private val curUser: LCUser = LCUser.currentUser()
        fun updateLastContactTime(friendName: String, time: Long) {
            val query = LCQuery<LCObject>("_Followee")
            query.whereEqualTo(LCFriendship.ATTR_FRIEND_STATUS, true)
            query.whereEqualTo("friendName", friendName)
            query.whereEqualTo("username", curUser.username)
            query.findInBackground().subscribe(object : Observer<List<LCObject>> {
                override fun onSubscribe(d: Disposable) {}
                override fun onError(e: Throwable) {}
                override fun onComplete() {}
                override fun onNext(friendshipList: List<LCObject>) {
                    if (!friendshipList.isNullOrEmpty()) {
                        for (friendship in friendshipList) {
                            friendship.put("lastContact", time)
                            friendship.saveInBackground().subscribe(object : Observer<LCObject> {
                                override fun onSubscribe(d: Disposable) {}
                                override fun onNext(t: LCObject) {}
                                override fun onError(e: Throwable) {}
                                override fun onComplete() {}

                            })
                        }
                    }
                }
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this,FriendViewModelFactory(requireActivity())).get(FriendViewModel::class.java)
        binding = FriendFragmentBinding.inflate(layoutInflater,null,false)

        val friendRecyclerView = binding.friendRecyclerView
        val addFriendButton = binding.addFriendButton

        activityViewModel.friends.observe(viewLifecycleOwner,{ result ->
            if (result != null) {
                val layoutManager = LinearLayoutManager(activity)
                friendRecyclerView.layoutManager = layoutManager
                val adapter = FriendAdapter(this,result,object : FriendAdapter.ItemListener {
                    override fun onItemClick(position: Int, friend: Pair<Friend, Boolean>) {
                        val intent = Intent(activity, ChatUI::class.java)
                        intent.putExtra("friendName",friend.first.friendName)
                        intent.putExtra("friendAvatar",friend.first.friendAvatar)
                        startActivity(intent)
                        activityViewModel.notifyMessageRead(friend.first.friendName)
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
        subscribeFriendshipRequests()
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
                val friendName = message.from
                addNotification(friendName)
                activityViewModel.notifyMessageUnread(friendName)
                updateLastContactTime(friendName,System.currentTimeMillis())
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
                    if (srcUsername != curUser.username) {
                        val srcUserAvatar = t[0].getInt("userAvatar")
                        AlertDialog.Builder(activity).apply {
                            setTitle("是否通过申请")
                            setMessage("用户 $srcUsername 请求添加你为好友")
                            setCancelable(false)
                            setPositiveButton("Accept") { _, _ ->
                                val attributes: MutableMap<String, Any> = HashMap()
                                attributes["lastContact"] = System.currentTimeMillis()
                                attributes["friendName"] = srcUsername
                                attributes["username"] = curUser.username
                                curUser.acceptFriendshipRequest(request, attributes)
                                    .subscribe(object : Observer<LCFriendshipRequest> {
                                        override fun onSubscribe(d: Disposable) {}
                                        override fun onError(e: Throwable) {}
                                        override fun onComplete() {}
                                        override fun onNext(t: LCFriendshipRequest) {
                                            activityViewModel.refreshFriendList(
                                                srcUsername,
                                                srcUserAvatar
                                            )
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

    private fun subscribeFriendshipRequests() {
        val requestQuery1 = LCQuery<LCFriendshipRequest>("_FriendshipRequest")
        requestQuery1.whereEqualTo(LCFriendshipRequest.ATTR_FRIEND, curUser)
        requestQuery1.whereEqualTo(LCFriendshipRequest.ATTR_STATUS, "pending")
        val requestQuery2 = LCQuery<LCFriendshipRequest>("_FriendshipRequest")
        requestQuery2.whereEqualTo(LCFriendshipRequest.ATTR_USER, curUser)
        requestQuery2.whereEqualTo(LCFriendshipRequest.ATTR_STATUS, "pending")
        val requestQuery = LCQuery.or(listOf(requestQuery1,requestQuery2))
        val requestLiveQuery = LCLiveQuery.initWithQuery(requestQuery)
        requestLiveQuery.setEventHandler(object : LCLiveQueryEventHandler() {
            override fun onObjectCreated(request: LCObject) {
                val query = LCQuery<LCFriendshipRequest>(LCFriendshipRequest.CLASS_NAME)
                query.whereEqualTo(LCFriendshipRequest.KEY_OBJECT_ID, request.objectId)
                query.firstInBackground.subscribe(object : Observer<LCFriendshipRequest> {
                    override fun onSubscribe(d: Disposable) {}
                    override fun onNext(t: LCFriendshipRequest) {
                        askForAcceptance(t)
                    }
                    override fun onError(e: Throwable) {}
                    override fun onComplete() {}
                })
            }

            override fun onObjectLeave(LCObject: LCObject?, updateKeyList: MutableList<String>?) {
                if (LCObject != null) {
                    val query = LCQuery<LCFriendshipRequest>(LCFriendshipRequest.CLASS_NAME)
                    query.whereEqualTo(LCFriendshipRequest.KEY_OBJECT_ID, LCObject.objectId)
                    query.firstInBackground.subscribe(object : Observer<LCFriendshipRequest> {
                        override fun onSubscribe(d: Disposable) {}
                        override fun onNext(request: LCFriendshipRequest) {
                            if (request.getString("status") == "accepted") {
                                val friendId = request.friend.objectId
                                updateFriendListByFriendId(friendId)
                            }
                        }
                        override fun onError(e: Throwable) {}
                        override fun onComplete() {}
                    })
                }
            }
        })
        requestLiveQuery.subscribeInBackground(object : LCLiveQuerySubscribeCallback() {
            override fun done(e: LCException?) {}
        })
    }

    private fun updateFriendListByFriendId(friendId: String) {
        val query = LCQuery<LCUser>(LCUser.CLASS_NAME)
        query.whereEqualTo(LCUser.KEY_OBJECT_ID,friendId)
        query.firstInBackground.subscribe(object : Observer<LCUser> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(friend: LCUser) {
                activityViewModel.refreshFriendList(friend.username,friend.getInt("userAvatar"))
            }
            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        })
    }

}