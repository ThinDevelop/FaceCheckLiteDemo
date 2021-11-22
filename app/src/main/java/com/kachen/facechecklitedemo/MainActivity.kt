package com.kachen.facechecklitedemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kachen.facechecklitedemo.model.ErrorModel
import com.kachen.facechecklitedemo.model.TokenResponseModel
import com.kachen.facechecklitedemo.util.NetworkUtil
import com.kachen.facechecklitedemo.util.PreferenceUtil
import com.kachen.facechecklitedemo.util.Util.Companion.setContext
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 999 // Return code after asking for permission
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) // permission needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        version.setText("version : "+BuildConfig.VERSION_NAME)
        setContext(this)
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
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

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * This gets called after the Camera permission pop up is shown.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                // Exit the app if permission is not granted
                // Best practice is to explain and offer a chance to re-request but this is out of
                // scope in this sample. More details:
                // https://developer.android.com/training/permissions/usage-notes
                Toast.makeText(this, getString(R.string.permission_deny_text), Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }
}