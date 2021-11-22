package com.kachen.facechecklitedemo

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.mlsdk.livenessdetection.*
import com.kachen.facechecklitedemo.util.ImageUtil
import com.kachen.facechecklitedemo.util.Util
import kotlinx.android.synthetic.main.activity_liveness_custom_detection.*


class CheckLivenessActivity : AppCompatActivity()  {
    var mPreviewContainer : FrameLayout? = null
    var mlLivenessDetectView : MLLivenessDetectView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liveness_custom_detection)
        mPreviewContainer = findViewById(R.id.surface_layout)
        //Obtain MLLivenessDetectView
        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels
        //ObtainLLivenessDetectView
        mlLivenessDetectView = MLLivenessDetectView.Builder()
            .setContext(this) // Set whether to perform mask detection.
            .setOptions(MLLivenessDetectView.DETECT_MASK) // Set the rectangle of the face frame relative to MLLivenessDetectView.
            .setFaceFrameRect(Rect(0, 0, widthPixels, dip2px(this, 320f))) // Set the result callback.
            .setDetectCallback(object : OnMLLivenessDetectCallback {
                override fun onCompleted(result: MLLivenessCaptureResult) {
                    Log.e("response", "onCompleted with live : "+ result.isLive +", pitch : "+result.pitch+
                         ", score : "+result.score+", roll : "+result.roll+", yaw : "+result.yaw)

                    if (result.isLive) {
                        intent.putExtra("status", "success")
                        intent.putExtra("image_path", ImageUtil.saveImage(result.bitmap).absolutePath)
                        setResult(RESULT_OK, intent)
                        finish()
                    } else {
                        Util.alertDialogShow(this@CheckLivenessActivity,
                                             "พบความผิดปกติ",
                                             "กด 'ตกลง' เพื่อตรวจสอบใบหน้าอีกครั้ง",
                                             object : Util.DialogActionListener {
                                                 override fun action() {
                                                     intent.putExtra("status", "retry")
                                                     setResult(RESULT_OK, intent)
                                                     finish()
                                                 }
                                             })
                    }
                    // Result callback when the liveness detection is complete.
                }

                override fun onError(error: Int) {
                    Log.e("response", "onError")
                    // Error code callback when an error occurs during liveness detection
                }

                override fun onInfo(infoCode: Int, bundle: Bundle) {
                    Log.e("response", "onInfo : " + infoCode)
                    // Liveness detection information callback, which can be used for screen prompt.
                    if (infoCode == MLLivenessDetectInfo.NO_FACE_WAS_DETECTED) {
//                            No face is detected.
                    }
                    // ...
                }

                override fun onStateChange(state: Int, bundle: Bundle) {
                    Log.e("response", "onStateChange : " + state)
                    // Callback when the liveness detection status is switched.
                    if (state == MLLivenessDetectStates.START_DETECT_FACE) {
                        Log.e("response", "onStateChange start detection")
                    }
                    // ...
                }
            })
            .build()
        mPreviewContainer?.addView(mlLivenessDetectView)
        mlLivenessDetectView?.onCreate(savedInstanceState)
        //Scanner overlay

        var animator : ObjectAnimator? = null
        val vto: ViewTreeObserver = scannerLayout.getViewTreeObserver()
        vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scannerLayout.getViewTreeObserver()
                    .removeGlobalOnLayoutListener(this)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    scannerLayout.getViewTreeObserver()
                        .removeGlobalOnLayoutListener(this)
                } else {
                    scannerLayout.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this)
                }
                animator = ObjectAnimator.ofFloat(scannerBar,
                                                  "translationY",
                                                  scannerLayout.getY(),
                                                  (scannerLayout.getY() + scannerLayout.getHeight()))
                animator?.setRepeatMode(ValueAnimator.REVERSE)
                animator?.setRepeatCount(ValueAnimator.INFINITE)
                animator?.setInterpolator(AccelerateDecelerateInterpolator())
                animator?.setDuration(3000)
                animator?.start()
            }
        })
    }

    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
    override fun onDestroy() {
        super.onDestroy()
        mlLivenessDetectView?.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        mlLivenessDetectView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mlLivenessDetectView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mlLivenessDetectView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mlLivenessDetectView?.onStop()
    }

}