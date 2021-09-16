package com.example.mychat.ui.friend

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.leancloud.LCFriendshipRequest
import cn.leancloud.LCQuery
import cn.leancloud.LCUser
import com.example.mychat.showLog
import com.example.mychat.showToast
import io.reactivex.Observer
import io.reactivex.disposables.Disposable




class FriendViewModel(private val context: Context) : ViewModel() {

    val friendshipRequests = MutableLiveData<ArrayList<LCFriendshipRequest>>()

    private val myTag = "FriendViewModel"
    private var curUser: LCUser = LCUser.currentUser()

    init {
        loadAllFriendshipRequests()
    }

    private fun loadAllFriendshipRequests() {
        val requestList = ArrayList<LCFriendshipRequest>()
        val query = LCQuery<LCFriendshipRequest>("_FriendshipRequest")
        query.whereEqualTo(LCFriendshipRequest.ATTR_FRIEND, curUser)
        query.whereEqualTo(LCFriendshipRequest.ATTR_STATUS, "pending")
        query.findInBackground().subscribe(object : Observer<List<LCFriendshipRequest>> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: List<LCFriendshipRequest>) {
                for (LCFriendshipRequest in t) {
                    requestList.add(LCFriendshipRequest)
                    friendshipRequests.value = requestList
                    "load".showLog(myTag)
                }
            }

            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        })
    }

    fun sendFriendshipRequestByName(friendName: String) {
        val query = LCQuery<LCUser>("_User")
        query.whereEqualTo(LCUser.ATTR_USERNAME,friendName)
        query.findInBackground().subscribe(object : Observer<List<LCUser>> {
            override fun onSubscribe(disposable: Disposable) {}
            override fun onError(throwable: Throwable) {}
            override fun onComplete() {}
            override fun onNext(t: List<LCUser>) {
                if (t.isNotEmpty()) {
                    sendFriendshipRequest(t[0].objectId,friendName)
                }
                else {
                    "用户不存在！".showToast(context)
                }
            }
        })
    }

    fun sendFriendshipRequest(friendObjectId: String, friendName: String) {
        val friend = LCUser.createWithoutData(LCUser::class.java,friendObjectId)
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["lastContact"] = System.currentTimeMillis()
        attributes["friendName"] = friendName
        attributes["username"] = curUser.username
        curUser.applyFriendshipInBackground(friend,attributes).subscribe(object :
            Observer<LCFriendshipRequest>{
            override fun onSubscribe(d: Disposable) {}

            override fun onNext(t: LCFriendshipRequest) {
                "好友请求已发送".showToast(context)
            }

            override fun onError(e: Throwable) {
                e.localizedMessage?.showToast(context)
            }

            override fun onComplete() {}

        })
    }

}