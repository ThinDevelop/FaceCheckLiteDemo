package com.kachen.facechecklitedemo

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
import com.kachen.facechecklitedemo.model.CardModel
import com.kachen.facechecklitedemo.model.EnrollRequestModel
import com.kachen.facechecklitedemo.model.EnrollResponseModel
import com.kachen.facechecklitedemo.model.ErrorModel
import com.kachen.facechecklitedemo.util.ImageUtil
import com.kachen.facechecklitedemo.util.NetworkUtil
import com.kachen.facechecklitedemo.util.Util
import kotlinx.android.synthetic.main.activity_enroll.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EnrollActivity : BaseFaceCheckLiteActivity() {
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
        setContentView(R.layout.activity_enroll)
        Util.setContext(this)
        mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound)

        mLoading = ProgressDialog(this)
        mLoading?.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mLoading?.setCanceledOnTouchOutside(false)
        mLoading?.setMessage("Reading...")

        takephoto.setOnClickListener {
            startActivityForResult(Intent(this@EnrollActivity, TakePhotoActivity::class.java), REQUEST_CODE_TAKEPHOTO)
        }

        submit.setOnClickListener {
            cardModel?.let {
                val image = ImageUtil.encodeImg(readcard)
                val enrollImage = ImageUtil.encodeImg(takephoto)
                if (image != null && enrollImage != null) {
                    val model = EnrollRequestModel(it.CitizenId,
                                                                       enrollImage,
                                                                       image,
                                                                       it.ThaiFirstName,
                                                                       it.ThaiLastName,
                                                                       it.Address)
                    NetworkUtil.enroll(model, object : NetworkUtil.Companion.NetworkLisener<EnrollResponseModel> {
                        override fun onResponse(response: EnrollResponseModel) {
                            Log.e("api", "enroll onResponse")
                            val message = "การลงทะเบียนสำเร็จ"
                            Util.alertDialogShow(this@EnrollActivity, "ผลการลงทะเบียน", message, object : Util.DialogActionListener {
                                override fun action() {
                                    this@EnrollActivity.finish()
                                }
                            })
                        }

                        override fun onError(errorModel: ErrorModel) {
                            Log.e("api", "enroll onError "+ errorModel.error_code+" : "+errorModel.msg)
                            Util.alertDialogShow(this@EnrollActivity, "เกิดข้อผิดพลาด", errorModel.msg, object : Util.DialogActionListener {
                                override fun action() {

                                }
                            })
                        }

                        override fun onExpired() {
                            Log.e("api", "enroll onExpired")
                            Util.alertErrorDialogShow(this@EnrollActivity)

                        }
                    })
                }
            }



        }
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
                                    if (!this@EnrollActivity.isFinishing()) {
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
            val path = data?.getStringExtra("image_path")?:""
            takephoto.setImageBitmap(ImageUtil.getBitmapFromgetAbsolutePath(path))
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