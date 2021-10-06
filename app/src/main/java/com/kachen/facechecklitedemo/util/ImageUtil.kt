package com.kachen.facechecklitedemo.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImageUtil {

    companion object {
        fun saveImage(bm: Bitmap): File {
            val root: String = Environment.getExternalStorageDirectory()
                .toString()

            val myDir = File("$root/liteDemo")
            myDir.mkdirs()
            val n = System.currentTimeMillis()
            val fname = "$n.jpg"
            val file = File(myDir, fname)
            Log.i("image", "" + file)
            if (file.exists()) file.delete()
            try {
                val out = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return file
        }

        fun getBitmapFromgetAbsolutePath(path: String): Bitmap? {
            val bmOptions = BitmapFactory.Options()
            try {
                return BitmapFactory.decodeFile(path, bmOptions)
            } catch (e: IOException) {
                return null
            }
        }


        fun encodeImg(type: ImageView): String? {
            var encoded: String? = ""
            type.invalidate()
            val drawable = type.drawable as BitmapDrawable
            if (drawable.bitmap != null) {
                val bitmap = drawable.bitmap
                encoded = encodeImg(bitmap)
            }
            return encoded
        }

        fun encodeImg(bitmap: Bitmap): String? {
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                var encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)
            return encoded
        }

        fun base64ToBitmap(encodedImage: String): Bitmap {
            val decodedString: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }
    }
}