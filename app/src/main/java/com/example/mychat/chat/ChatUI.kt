package com.example.mychat.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.leancloud.LCUser
import com.example.mychat.MessageAdapter
import com.example.mychat.databinding.ActivityChatUiBinding

class ChatUI : AppCompatActivity() {

    private val tag = "ChatUI"

    private lateinit var viewModel: ChatUIViewModel
    private lateinit var binding: ActivityChatUiBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private val curUser = LCUser.currentUser()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityChatUiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val friendName = intent.getStringExtra("friendName")!!
        val friendAvatar = intent.getIntExtra("friendAvatar",0)

        val inputText = binding.inputText
        val send = binding.send
        binding.title.friendName.text = friendName

        recyclerView = binding.recyclerView
        viewModel = ViewModelProvider(this,
            ChatUIViewModelFactory(this,friendName)).get(ChatUIViewModel::class.java)

        viewModel.messages.observe(this, { result ->
            if (result != null) {
                val layoutManager = LinearLayoutManager(this)
                recyclerView.layoutManager = layoutManager
                adapter = MessageAdapter(result,friendAvatar)
                recyclerView.adapter = adapter
                recyclerView.scrollToPosition(result.size - 1)
                recyclerView.
                addOnLayoutChangeListener { _, _, _, _, bottom,
                                            _, _, _, oldBottom ->
                    if (bottom < oldBottom) {
                        recyclerView.smoothScrollToPosition(result.size)
                    }
                }
            }
        })

        send.setOnClickListener {
            val messageContent = inputText.text.toString()
            if(messageContent.isNotEmpty()) {
                viewModel.sendMessage(messageContent)
                inputText.setText("")
            }
        }
    }

}