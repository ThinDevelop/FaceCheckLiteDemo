package com.kachen.facechecklitedemo.app

import android.app.Application
import android.content.Context
import com.kachen.facechecklitedemo.util.PreferenceUtil

class DemoApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        PreferenceUtil.init(applicationContext)
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }
}