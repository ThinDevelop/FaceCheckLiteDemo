package com.kachen.facechecklitedemo.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceUtil {
    private fun PreferenceUtil() {}



    companion object {
        private val PREFERENCE_KEY_TOKEN = "token"
        private var mAppContext: Context? = null

        fun init(appContext: Context?) {
            mAppContext = appContext
        }
        private fun getSharedPreferences(): SharedPreferences {
            return mAppContext!!.getSharedPreferences("demo_lite", Context.MODE_PRIVATE)
        }


        fun setToken(token: String?) {
            val editor: SharedPreferences.Editor = PreferenceUtil.getSharedPreferences().edit()
            editor.putString(PREFERENCE_KEY_TOKEN, token)
                .apply()
        }

        fun getToken(): String? {
            return getSharedPreferences().getString(PREFERENCE_KEY_TOKEN, "")
        }
    }

}