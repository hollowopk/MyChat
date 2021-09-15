package com.example.mychat

import android.content.Context
import android.util.Log
import android.widget.Toast

fun String.showToast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context,this,duration).show()
}

fun String.showLog(tag: String) {
    Log.d(tag,this)
}