package com.developer.ev2z.util


import android.content.Context

internal object OpenUrlHolder {
    lateinit var context: Context
}

fun initOpenUrl(context: Context) {
    OpenUrlHolder.context = context.applicationContext
}