package com.kachen.facechecklitedemo.util

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.kachen.facechecklitedemo.app.DemoApplication

class Util {
    companion object {
        var activityContext: Context? = null
        fun showToast(@StringRes resId : Int) {
            Toast.makeText(DemoApplication.appContext, DemoApplication.appContext.getString(resId), Toast.LENGTH_LONG).show()
        }

        fun setContext(context: Context) {
            activityContext = context
        }

        fun alertErrorDialogShow(context: Context?) {
            alertDialogShow(context, "เกิดข้อผิดพลาด", "กรุณาลองใหม่อีกครั้ง",object : DialogActionListener {
                override fun action() {

                }
            })

        }
        fun alertDialogShow(context: Context?, title: String, message: String?, listener: DialogActionListener) {
            val alertDialog: AlertDialog = AlertDialog.Builder(context)
                .create()
            alertDialog.setTitle(title)
            alertDialog.setMessage(message)
            alertDialog.setButton("OK") { dialog, which ->
                alertDialog.dismiss()
                listener.action()
            }
            alertDialog.show()
        }
    }

    interface DialogActionListener {
       fun action()
    }
}