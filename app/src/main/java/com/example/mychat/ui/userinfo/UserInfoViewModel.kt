package com.example.mychat.ui.userinfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.leancloud.LCUser

class UserInfoViewModel : ViewModel() {

    val userAvatar = MutableLiveData<Int>()
    val userName = MutableLiveData<String>()
    private val curUser = LCUser.getCurrentUser()

    init {
        userAvatar.value = curUser.getInt("userAvatar")
        userName.value = curUser.username
    }

}