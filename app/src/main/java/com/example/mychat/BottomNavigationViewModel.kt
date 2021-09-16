package com.example.mychat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.leancloud.LCFriendship
import cn.leancloud.LCObject
import cn.leancloud.LCUser
import com.example.mychat.database.Friend
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class BottomNavigationViewModel : ViewModel() {

    val friends = MutableLiveData<ArrayList<Pair<Friend,Boolean>>>()
    private var curUser: LCUser = LCUser.currentUser()
    private val tag = "BottomNavigationViewModel"

    init {
        loadAllFriends()
    }

    private fun loadAllFriends() {
        friends.value = ArrayList()
        val query = curUser.friendshipQuery(false)!!
        query.whereEqualTo(LCFriendship.ATTR_FRIEND_STATUS, true)
        query.orderByDescending("lastContact")
        query.findInBackground().subscribe(object : Observer<List<LCObject?>?> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: List<LCObject?>) {
                val fList = ArrayList<Pair<Friend,Boolean>>()
                for (lcObject in t) {
                    val friend = lcObject?.getLCObject<LCUser>("followee")
                    val friendName  = friend?.getString("username")!!
                    val friendAvatar = friend.getInt("userAvatar")
                    fList.add(Pair(Friend(friendName,friendAvatar),false))
                }
                friends.value = fList
            }

            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        })
    }

    fun notifyMessageUnread(friendName: String) {
        val friendList = ArrayList<Pair<Friend,Boolean>>()
        for (friend in friends.value!!) {
            if (friend.first.friendName == friendName) {
                friendList.add(0,Pair(friend.first,true))
            }
            else {
                friendList.add(friend)
            }
        }
        friends.value = friendList
    }

    fun notifyMessageRead(friendName: String) {
        val friendList = ArrayList<Pair<Friend,Boolean>>()
        for (friend in friends.value!!) {
            if (friend.first.friendName == friendName) {
                friendList.add(Pair(friend.first,false))
            }
            else {
                friendList.add(friend)
            }
        }
        friends.value = friendList
    }

    fun refreshFriendList(friendName: String, friendAvatar: Int) {
        val fList = friends.value
        if (fList != null) {
            fList.add(0,Pair(Friend(friendName,friendAvatar),false))
            friends.value = fList
        }
    }

}