package com.example.mychat.ui.userinfo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cn.leancloud.LCUser
import cn.leancloud.im.v2.LCIMClient
import cn.leancloud.im.v2.LCIMException
import cn.leancloud.im.v2.callback.LCIMClientCallback
import com.example.mychat.MainActivity
import com.example.mychat.MyApplication
import com.example.mychat.databinding.UserInfoFragmentBinding
import com.example.mychat.showLog
import com.example.mychat.showToast

class UserInfoFragment : Fragment() {

    private val myTag = "UserInfoFragment"
    private lateinit var viewModel: UserInfoViewModel
    private lateinit var binding: UserInfoFragmentBinding
    private val curUser = LCUser.currentUser()
    private val client = LCIMClient.getInstance(curUser.username)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(UserInfoViewModel::class.java)
        binding = UserInfoFragmentBinding.inflate(layoutInflater)
        val exitButton = binding.exitButton

        viewModel.userName.observe(viewLifecycleOwner, { result ->
            binding.userName.text = result
        })
        viewModel.userAvatar.observe(viewLifecycleOwner, { result ->
            binding.userAvatar.setImageResource(result)
        })

        exitButton.setOnClickListener {
            activity?.let { it1 -> "Exit".showToast(it1) }
            client.close(object : LCIMClientCallback() {
                override fun done(client: LCIMClient?, e: LCIMException?) {}
            })
            LCUser.logOut()
            "${LCUser.currentUser()}".showLog(myTag)
            val intent2Login = Intent(MyApplication.context,MainActivity::class.java)
            startActivity(intent2Login)
        }
        return binding.root
    }

}