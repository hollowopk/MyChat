package com.example.mychat

import android.app.Application
import android.content.Context
import cn.leancloud.LCLogger
import cn.leancloud.LeanCloud

class MyApplication : Application() {

    private val tag = "MyApplication"

    companion object {
        lateinit var context: Context
        const val appId = "q8o3Mk93LS3okVA7hzJa49Ju-gzGzoHsz"
        const val appKey = "zkMVGOFWBN112Ja8hg2kKtsd"
        const val masterKey = "Q1ARTdWMDuUO5T7hMTdQcli6"
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        LeanCloud.setLogLevel(LCLogger.Level.DEBUG)
        LeanCloud.initialize(this,appId,appKey,"https://q8o3mk93.lc-cn-n1-shared.com")
    }
}