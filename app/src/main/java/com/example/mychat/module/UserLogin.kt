package com.example.mychat.module

import android.content.Context
import android.content.Intent
import cn.leancloud.LCUser
import cn.leancloud.im.v2.LCIMClient
import cn.leancloud.im.v2.LCIMException
import cn.leancloud.im.v2.callback.LCIMClientCallback
import com.example.mychat.Avatars
import com.example.mychat.BottomNavigationActivity
import com.example.mychat.showLog
import com.example.mychat.showToast
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class UserLogin(val context: Context) {

    private val tag = "UserLogin"

    fun signUp(username: String, password: String) {
        val newUser = LCUser()
        newUser.username = username
        newUser.password = password
        newUser.put("userAvatar",Avatars.avatarList.random())
        newUser.signUpInBackground().subscribe(object : Observer<LCUser> {
            override fun onSubscribe(d: Disposable) {}

            override fun onNext(t: LCUser) {
                "注册成功".showToast(context)
                login(username,password)
            }

            override fun onError(e: Throwable) {
                e.localizedMessage.showLog(tag)
                "该用户名已被注册！请更改用户名".showToast(context)
            }

            override fun onComplete() {}

        })
    }

    fun login(username: String, password: String) {
        LCUser.logIn(username, password).subscribe(object : Observer<LCUser?> {
            override fun onSubscribe(disposable: Disposable) {}

            override fun onError(throwable: Throwable) {
                throwable.localizedMessage.showToast(context)
            }

            override fun onComplete() {}

            override fun onNext(t: LCUser) {
                "登录成功".showToast(context)
                LCIMClient.getInstance(t.username)
                    .open(object : LCIMClientCallback() {
                        override fun done(client: LCIMClient?, e: LCIMException?) {}
                    })
                val userAvatar = t.getInt("userAvatar")
                val intent = Intent(context, BottomNavigationActivity::class.java)
                intent.putExtra("username",username)
                intent.putExtra("userAvatar",userAvatar)
                context.startActivity(intent)
            }
        })
    }
}