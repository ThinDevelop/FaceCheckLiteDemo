package com.kachen.facechecklitedemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCapture
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCaptureResult
import com.kachen.facechecklitedemo.model.DeleteResponseModel
import com.kachen.facechecklitedemo.model.ErrorModel
import com.kachen.facechecklitedemo.model.IdentifyResponseModel
import com.kachen.facechecklitedemo.util.ImageUtil
import com.kachen.facechecklitedemo.util.NetworkUtil
import com.kachen.facechecklitedemo.util.Util
import kotlinx.android.synthetic.main.activity_enroll.*
import kotlinx.android.synthetic.main.activity_identify.*
import kotlinx.android.synthetic.main.activity_identify.edt_address
import kotlinx.android.synthetic.main.activity_identify.idnumber
import kotlinx.android.synthetic.main.activity_identify.lastname
import kotlinx.android.synthetic.main.activity_identify.name


class IdentifyActivity : AppCompatActivity() {
    var response: IdentifyResponseModel? = null
    val REQUEST_CODE_TAKEPHOTO = 11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identify)

        Util.setContext(this)
//        startActivityForResult(Intent(this@IdentifyActivity, TakePhotoActivity::class.java), REQUEST_CODE_TAKEPHOTO)
        back.setOnClickListener {
            this@IdentifyActivity.finish()
        }
        startDetect()
        //delete
    }


    private val callback: MLLivenessCapture.Callback = object : MLLivenessCapture.Callback {
        override fun onSuccess(p0: MLLivenessCaptureResult?) {
            p0?.let {
                Log.e("response","score : "+ it.score)

                if (it.isLive) {
                    identify(it.bitmap)
                } else {
                    Util.alertDialogShow(this@IdentifyActivity, "พบความผิดปกติ", "กด 'OK' เพื่อตรวจสอบใบหน้าอีกครั้ง", object : Util.DialogActionListener {
                        override fun action() {
                            startDetect()
                        }
                    })
                }
            }
        }

        override fun onFailure(p0: Int) {

        }
    }
    private fun startDetect() {
//        val capture: MLLivenessCapture = MLLivenessCapture.getInstance()
//        capture.startDetect(activity, callback)
        startActivityForResult(Intent(this@IdentifyActivity, CheckLivenessActivity::class.java), REQUEST_CODE_TAKEPHOTO)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_identify, menu)
        // return true so that the menu pop up is opened
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                response?.let {
                    delete(it.NationalID)
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            this@IdentifyActivity.finish()
        } else {
            if (requestCode == REQUEST_CODE_TAKEPHOTO) {

                val status = data?.getStringExtra("status")?:""
                if (status.equals("retry")) {
                    startDetect()
                } else {
                    val path = data?.getStringExtra("image_path") ?: ""
                    val face = ImageUtil.getBitmapFromgetAbsolutePath(path)
                    face?.let {
                        identify(it)
                    }
                }
            }
        }
    }

    fun identify(face: Bitmap){
//        photo.setImageBitmap(face)
        val base64 = ImageUtil.encodeImg(face)
        base64?.let {
            NetworkUtil.identify(it, object : NetworkUtil.Companion.NetworkLisener<IdentifyResponseModel> {
                override fun onResponse(model: IdentifyResponseModel) {
                    response = model
                    val face64 = ImageUtil.base64ToBitmap(model.Image)
                    edt_address.setText(model.Address)
                    lastname.setText(model.LastName)
                    name.setText(model.FirstName)
                    val nId = model.NationalID
                    val id = nId.substring(0, 7) + "xxx" + nId.substring(nId.length - 3)
                    idnumber.setText(id)
                    photo.setImageBitmap(face64)
                }

                override fun onError(errorModel: ErrorModel) {
                    Log.e("api", "identify onError " + errorModel.error_code + " : " + errorModel.msg)
                    Util.alertDialogShow(this@IdentifyActivity, "เกิดข้อผิดพลาด", errorModel.msg, object : Util.DialogActionListener {
                        override fun action() {
                        }
                    })
                }

                override fun onExpired() {
                    Log.e("api", "identify onExpired")
                    Util.alertErrorDialogShow(this@IdentifyActivity)
//                    NetworkUtil.identify(it, object : NetworkUtil.Companion.NetworkLisener<IdentifyResponseModel> {
//                        override fun onResponse(response: IdentifyResponseModel) {
//                            val face64 = ImageUtil.base64ToBitmap(response.Image)
//                            edt_address.setText(response.Address)
//                            lastname.setText(response.LastName)
//                            name.setText(response.FirstName)
//                            idnumber.setText(response.NationalID)
//                            photo.setImageBitmap(face64)
//                        }
//
//                        override fun onError(errorModel: ErrorModel) {
//                            Log.e("api", "identify onError " + errorModel.error_code + " : " + errorModel.msg)
//                            Util.alertDialogShow(this@IdentifyActivity, "เกิดข้อผิดพลาด", errorModel.msg, object : Util.DialogActionListener {
//                                override fun action() {
//                                }
//                            })
//                        }
//
//                        override fun onExpired() {
//                            Log.e("api", "identify onExpired")
//                            Util.alertErrorDialogShow(this@IdentifyActivity)
//
//                        }
//                    })
                }
            })
        }
    }

    fun delete(id: String){

            NetworkUtil.delete(id, object : NetworkUtil.Companion.NetworkLisener<DeleteResponseModel> {
                override fun onResponse(response: DeleteResponseModel) {
                    Log.e("api", "identify onResponse" + response)
                    Util.alertDialogShow(this@IdentifyActivity, "Face deleted", response.message, object : Util.DialogActionListener {
                        override fun action() {
                        }
                    })
                }

                override fun onError(errorModel: ErrorModel) {
                    Log.e("api", "identify onError")
                    Util.alertDialogShow(this@IdentifyActivity, "", errorModel.msg, object : Util.DialogActionListener {
                        override fun action() {
                        }
                    })
                }

                override fun onExpired() {
                    Log.e("api", "identify onExpired")
                }
            })
    }


}