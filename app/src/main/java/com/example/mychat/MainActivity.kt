package com.example.mychat

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import cn.leancloud.LCUser
import cn.leancloud.im.v2.LCIMClient
import cn.leancloud.im.v2.LCIMException
import cn.leancloud.im.v2.callback.LCIMClientCallback
import com.example.mychat.databinding.ActivityMainBinding
import com.example.mychat.databinding.OpeningBinding
import com.example.mychat.module.UserLogin
import io.reactivex.Observer
import io.reactivex.disposables.Disposable


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var openingBinding: OpeningBinding

    private val myTag = "MainActivity"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        openingBinding = OpeningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val signUp = binding.signUp
        var isLogin = true
        username.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val curUser = LCUser.currentUser()
        if (curUser != null) {
            val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = manager.activeNetwork
            if (networkInfo != null) {
                val context = this
                LCUser.becomeWithSessionTokenInBackground(curUser!!.sessionToken)
                    .subscribe(object : Observer<LCUser?> {
                        override fun onSubscribe(d: Disposable) {}

                        override fun onNext(user: LCUser) {
                            LCUser.changeCurrentUser(user, true)
                            LCIMClient.getInstance(LCUser.currentUser().username)
                                .open(object : LCIMClientCallback() {
                                    override fun done(client: LCIMClient?, e: LCIMException?) {
                                        "connect open!".showLog(myTag)
                                    }
                                })
                            openNextActivity(user)
                            return
                        }

                        override fun onError(e: Throwable) {}

                        override fun onComplete() {}
                    })
            }
            else {
                openNextActivity(curUser)
            }
        }
        else {
            setContentView(binding.root)
        }
        login.setOnClickListener {
            if (username.text.isNotEmpty() and password.text.isNotEmpty()) {
                val username = username.text.toString()
                UserLogin(this).login(username, password.text.toString())
            }
            else {
                "用户id和密码都不能为空！".showToast(this)
            }
        }
        signUp.setOnClickListener {
            if (isLogin) {
                isLogin = false
                login.visibility = View.GONE
                signUp.text = "注册并登录"
                username.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            else {
                if (username.text.isNotEmpty() and password.text.isNotEmpty()) {
                    UserLogin(this).signUp(
                        username.text.toString(),
                        password.text.toString()
                    )
                }
                else {
                    "用户名和密码都不能为空！".showToast(this)
                }
            }
        }
    }

    fun openNextActivity(user : LCUser) {
        val intent = Intent(this, BottomNavigationActivity::class.java)
        intent.putExtra("username", user.username)
        intent.putExtra("userAvatar", user.getInt("userAvatar"))
        startActivity(intent)
        finish()
    }
}