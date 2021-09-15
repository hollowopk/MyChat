package com.example.mychat.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChatUIViewModelFactory(private val context: Context, private val friendName: String)
    :ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatUIViewModel(context,friendName) as T
    }

}