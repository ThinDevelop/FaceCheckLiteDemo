package com.kachen.facechecklitedemo

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.RemoteException
import android.util.Log
import android.view.WindowManager
import com.centerm.centermposoversealib.thailand.AidlIdCardTha
import com.centerm.centermposoversealib.thailand.ThaiInfoListerner
import com.centerm.centermposoversealib.thailand.ThaiPhotoListerner
import com.centerm.smartpos.aidl.iccard.AidlICCard
import com.centerm.smartpos.aidl.sys.AidlDeviceManager
import com.centerm.smartpos.constant.Constant
import com.google.gson.Gson
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCapture
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCaptureResult
import com.kachen.facechecklitedemo.model.*
import com.kachen.facechecklitedemo.util.ImageUtil
import com.kachen.facechecklitedemo.util.NetworkUtil
import com.kachen.facechecklitedemo.util.Util
import kotlinx.android.synthetic.main.activity_enroll.*
import kotlinx.android.synthetic.main.activity_verify.*
import kotlinx.android.synthetic.main.activity_verify.edt_address
import kotlinx.android.synthetic.main.activity_verify.idnumber
import kotlinx.android.synthetic.main.activity_verify.lastname
import kotlinx.android.synthetic.main.activity_verify.name
import kotlinx.android.synthetic.main.activity_verify.readcard
import kotlinx.android.synthetic.main.activity_verify.submit
import kotlinx.android.synthetic.main.activity_verify.takephoto
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VerifyActivity : BaseFaceCheckLiteActivity() {
    private var aidlIdCardTha: AidlIdCardTha? = null
    private var aidlIcCard: AidlICCard? = null
    private var mediaPlayer: MediaPlayer? = null
    private var aidlReady = false
    private var mLoading: ProgressDialog? = null
    var cardModel: CardModel? = null
    val REQUEST_CODE_TAKEPHOTO = 11

    var scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)
        mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound)

        Util.setContext(this)
        mLoading = ProgressDialog(this)
        mLoading?.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mLoading?.setCanceledOnTouchOutside(false)
        mLoading?.setMessage("Reading...")

        takephoto.setOnClickListener {
            startDetect()
//            startActivityForResult(Intent(this@VerifyActivity, TakePhotoActivity::class.java), REQUEST_CODE_TAKEPHOTO)
        }

        submit.setOnClickListener {
            //call Api
            cardModel?.let {
                val enrollImage = ImageUtil.encodeImg(takephoto)
                if (enrollImage != null) {
                    NetworkUtil.verify(it.CitizenId, enrollImage, object : NetworkUtil.Companion.NetworkLisener<VerifyResponseModel> {
                        override fun onResponse(response: VerifyResponseModel) {
                            Log.e("api", "verify onResponse MatchStatus : "+response.MatchingStatus +", MatchingScore : "+ response.MatchingScore)

                            var message = "ไม่ใช่คนเดียวกัน"
                            if (response.MatchingStatus) {
                                message = "เป็นบุคคลเดียวกัน ด้วยคะแนน " + response.MatchingScore
                            }
                            Util.alertDialogShow(this@VerifyActivity, "ผลการตรวจสอบ", message , object : Util.DialogActionListener {
                            override fun action() {
                                this@VerifyActivity.finish()
                            }
                        })
                    }

                        override fun onError(errorModel: ErrorModel) {
                            Log.e("api", "verify onError "+ errorModel.error_code+" : "+errorModel.msg)
                            Util.alertDialogShow(this@VerifyActivity, "เกิดข้อผิดพลาด", errorModel.msg, object : Util.DialogActionListener {
                                override fun action() {

                                }
                            })
                        }

                        override fun onExpired() {
                            Log.e("api", "verify onExpired")
                            Util.alertErrorDialogShow(this@VerifyActivity)
                        }
                    })
                }
            }
        }

    }


    private val callback: MLLivenessCapture.Callback = object : MLLivenessCapture.Callback {
        override fun onSuccess(p0: MLLivenessCaptureResult?) {
            takephoto.setImageBitmap(null)
            p0?.let {
                Log.e("response","score : "+ it.score)

                if (it.isLive) {
                    takephoto.setImageBitmap(it.bitmap)
                } else {
                    Util.alertDialogShow(this@VerifyActivity, "พบความผิดปกติ", "กด 'OK' เพื่อตรวจสอบใบหน้าอีกครั้ง", object : Util.DialogActionListener {
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
        startActivityForResult(Intent(this@VerifyActivity, CheckLivenessActivity::class.java), REQUEST_CODE_TAKEPHOTO)

    }

    override fun onDeviceConnected(deviceManager: AidlDeviceManager?, cpay: Boolean) {
        try {
            if (cpay) {
                aidlIcCard = AidlICCard.Stub.asInterface(deviceManager?.getDevice(Constant.DEVICE_TYPE.DEVICE_TYPE_ICCARD))
                if (aidlIcCard != null) {
                    Log.e("MY", "IcCard bind success!")
                    //This is the IC card service object!!!!
                    //I am do nothing now and it is not null.
                    //you can do anything by yourselef later.
                    d()
                } else {
                    Log.e("MY", "IcCard bind fail!")
                }
            } else {
                aidlIdCardTha = AidlIdCardTha.Stub.asInterface(deviceManager?.getDevice(com.centerm.centermposoversealib.constant.Constant.OVERSEA_DEVICE_CODE.OVERSEA_DEVICE_TYPE_THAILAND_ID))
                aidlReady = aidlIdCardTha != null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    fun d() {
        val job: Runnable = object : Runnable {
            var _read = false
            override fun run() {
                try {
                    aidlIcCard!!.open()
                    if (aidlIcCard!!.status().toInt() == 1) {
                        if (!_read) {
                            _read = true
                            runOnUiThread {
                                try {
                                    if (!this@VerifyActivity.isFinishing()) {
                                        try {
                                            mLoading?.show()
                                        } catch (e: WindowManager.BadTokenException) {
                                            Log.e("WindowManagerBad ", e.toString())
                                        }
                                    }
                                    readcard.setImageBitmap(null)
                                    if (aidlIdCardTha != null) {
                                        aidlIdCardTha!!.stopSearch()
                                        aidlIdCardTha!!.searchIDCardInfo(6000, object : ThaiInfoListerner.Stub() {
                                            @Throws(RemoteException::class)
                                            override fun onResult(i: Int, s: String) {
                                                Log.e("DATA", "onResult : $s")
                                                cardModel = Gson().fromJson(s, CardModel::class.java)
                                                cardModel?.let {
                                                    runOnUiThread {
                                                        val id = it.CitizenId.substring(0, 7)+"xxx"+it.CitizenId.substring(it.CitizenId.length-3)
                                                        idnumber.setText(id)
                                                        name.setText(it.ThaiFirstName)
                                                        lastname.setText(it.ThaiLastName)
                                                        edt_address.setText(it.Address.replace("#"," "))
                                                    }
                                                }

                                                mediaPlayer?.start()
                                                mLoading?.dismiss()
                                            }
                                        })
                                        Handler().postDelayed({ searchPhoto() }, 2000)
                                        //                                        aidlIdCardTha.searchIDCardPhoto(6000, new ThaiPhotoListerner.Stub() {
                                        //                                            @Override
                                        //                                            public void onResult(int i, Bitmap bitmap) throws RemoteException {
                                        //                                                Log.e("DATA", "onResult photo");
                                        //                                                Bitmap rebmp = Bitmap.createScaledBitmap(bitmap, 85, 100, false);
                                        //                                                showPhoto(rebmp);
                                        //                                            }
                                        //                                        });
                                    }
                                } catch (e: RemoteException) {
                                    Log.e("DATA info", "RemoteException")
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else {
                        _read = false
                        runOnUiThread {
                            mLoading?.dismiss()
                            c()
                        }
                    }
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                } finally {
                    if (mLoading != null && mLoading!!.isShowing()) {
                        mLoading?.dismiss()
                    }
                }
            }
        }
        scheduledExecutor.scheduleAtFixedRate(job, 1000, 1000, TimeUnit.MILLISECONDS)
    }

    private fun searchPhoto() {
        try {
            Log.e("DATA", "searchPhoto")
            aidlIdCardTha!!.stopSearch()
            aidlIdCardTha!!.searchIDCardPhoto(6000, object : ThaiPhotoListerner.Stub() {
                @Throws(RemoteException::class)
                override fun onResult(i: Int, bitmap: Bitmap) {
                    showPhoto(bitmap)
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun showPhoto(bitmap: Bitmap) {
        runOnUiThread {
            readcard.setImageBitmap(bitmap)
        }
    }

    private fun c() {
        idnumber.setText("")
        name.setText("")
        lastname.setText("")
        edt_address.setText("")
        readcard.setImageDrawable(getDrawable(R.drawable.card_photo))
        readcard.destroyDrawingCache()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        if (requestCode == REQUEST_CODE_TAKEPHOTO) {
            val status = data?.getStringExtra("status")?:""
            if (status.equals("retry")) {
                startDetect()
            } else {
                val path = data?.getStringExtra("image_path")?:""
                takephoto.setImageBitmap(ImageUtil.getBitmapFromgetAbsolutePath(path))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor.shutdownNow()
        if (aidlIcCard != null) {
            try {
                aidlIcCard!!.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/liteDemo")
        storageDir?.delete()
    }
}