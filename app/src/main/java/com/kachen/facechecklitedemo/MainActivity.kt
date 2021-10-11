package com.kachen.facechecklitedemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kachen.facechecklitedemo.model.ErrorModel
import com.kachen.facechecklitedemo.model.TokenResponseModel
import com.kachen.facechecklitedemo.util.NetworkUtil
import com.kachen.facechecklitedemo.util.PreferenceUtil
import com.kachen.facechecklitedemo.util.Util.Companion.setContext
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        version.setText("version : "+BuildConfig.VERSION_NAME)
        setContext(this)
        if (PreferenceUtil.getToken().equals("")) {
            NetworkUtil.getToken(object : NetworkUtil.Companion.NetworkLisener<TokenResponseModel> {
                override fun onResponse(response: TokenResponseModel) {
//                TODO("Not yet implemented")
                }

                override fun onError(errorModel: ErrorModel) {
//                TODO("Not yet implemented")
                }

                override fun onExpired() {
//                TODO("Not yet implemented")
                }
            })
        }
        identify.setOnClickListener {
            startActivity(Intent(this@MainActivity, IdentifyActivity::class.java))
        }

        enroll.setOnClickListener {
            startActivity(Intent(this@MainActivity, EnrollActivity::class.java))
        }

        verify.setOnClickListener {
            startActivity(Intent(this@MainActivity, VerifyActivity::class.java))
        }

    }
}