package com.kachen.facechecklitedemo.util

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.util.Log
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.gson.Gson
import com.kachen.facechecklitedemo.app.DemoApplication
import com.kachen.facechecklitedemo.model.*
import org.json.JSONObject

class NetworkUtil {
    companion object {
        var progressdialog: ProgressDialog? = null
        private val URL_DOMAIN = "https://api.facecheck.tech/BAAC"
        val URL_GET_TOKEN = "$URL_DOMAIN/token"
        val URL_ENROLL = "$URL_DOMAIN/Enroll"
        val URL_VERIFY = "$URL_DOMAIN/Verify"
        val URL_IDENTIFY = "$URL_DOMAIN/Identify"
        val URL_DELETE = "$URL_DOMAIN/Delete"


        fun getToken(listener: NetworkLisener<TokenResponseModel>) {
            showLoadingDialog()
            AndroidNetworking.post(URL_GET_TOKEN)
                .addBodyParameter("username", "admin")
                .addBodyParameter("password", "admin1234")
                .addBodyParameter("grant_type", "password")
                .setTag("getToken")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(getResponseListener(TokenResponseModel::class.java, listener))
        }

        fun enroll(requestModel: EnrollRequestModel, listener: NetworkLisener<EnrollResponseModel>) {
            showLoadingDialog()
            AndroidNetworking.post(URL_ENROLL)
                .addHeaders("Authorization", "Bearer " + PreferenceUtil.getToken())
                .addHeaders("Content-type", "application/json")
                .addHeaders("Accept", "application/json")
                .addBodyParameter("NationalID", requestModel.nationalID)
                .addBodyParameter("EnrollImage", requestModel.enrollImage)
                .addBodyParameter("Image", requestModel.image)
                .addBodyParameter("FirstName", requestModel.firstName)
                .addBodyParameter("LastName", requestModel.lastName)
                .addBodyParameter("Address", requestModel.address)
                .setTag("enroll")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(getResponseListener(EnrollResponseModel::class.java, listener))
        }

        fun verify(id: String, image: String, listener: NetworkLisener<VerifyResponseModel>) {
            showLoadingDialog()
            AndroidNetworking.post(URL_VERIFY)
                .addHeaders("Authorization", "Bearer " + PreferenceUtil.getToken())
                .addHeaders("Content-type", "application/json")
                .addHeaders("Accept", "application/json")
                .addBodyParameter("NationalID", id)
                .addBodyParameter("Image", image)
                .setTag("verify")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(getResponseListener(VerifyResponseModel::class.java, listener))
        }

        fun identify(image: String, listener: NetworkLisener<IdentifyResponseModel>) {
            showLoadingDialog()
            AndroidNetworking.post(URL_IDENTIFY)
                .addHeaders("Authorization", "Bearer " + PreferenceUtil.getToken())
                .addHeaders("Content-type", "application/json")
                .addHeaders("Accept", "application/json")
                .addBodyParameter("Image", image)
                .setTag("identify")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(getResponseListener(IdentifyResponseModel::class.java, listener))
        }

        fun delete(id: String, listener: NetworkLisener<DeleteResponseModel>) {
            showLoadingDialog()
            AndroidNetworking.delete(URL_DELETE)
                .addHeaders("Authorization", "Bearer " + PreferenceUtil.getToken())
                .addHeaders("Content-type", "application/json")
                .addHeaders("Accept", "application/json")
                .addBodyParameter("NationalID", id)
                .setTag("delete")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(getResponseListener(DeleteResponseModel::class.java, listener))
        }


        private fun <T : BaseResponseModel> getResponseListener(kClass: Class<T>, listener: NetworkLisener<T>): JSONObjectRequestListener {
            return object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    hideLoadingDialog()
                    Log.e("api response", response.toString())
                    response?.let {
                        if ("0001".equals(it.getString("status"))) {
                            if (kClass.isAssignableFrom(TokenResponseModel::class.java)) {
                                val token = it.optString("access_token")
                                PreferenceUtil.setToken(token)
                                listener.onResponse(Gson().fromJson(it.toString(), kClass))
                            } else {
                                listener.onResponse(Gson().fromJson(it.toString(), kClass))
                            }
                        } else {
                            val status = it.optString("status")
                            val obj = JSONObject().put("error_code", status)
                                .put("msg", it.optString("message", it.toString()))
                            val errorModel = Gson().fromJson(obj.toString(), ErrorModel::class.java)
                            listener.onError(errorModel)
                            Log.e("ERROR", "code : " + errorModel.error_code + ", msg : " + errorModel.msg)

                        }
                    }
                }

                override fun onError(anError: ANError?) {
                    hideLoadingDialog()
                    anError?.let {
                        it.printStackTrace()
                        if (401 == it.errorCode) {
                            getToken(object : NetworkLisener<TokenResponseModel> {
                                override fun onResponse(response: TokenResponseModel) {
                                    listener.onExpired()
                                }

                                override fun onError(errorModel: ErrorModel) {
                                    val obj = JSONObject().put("status", errorModel.error_code)
                                        .put("msg", errorModel.msg)
                                    val errorModel = Gson().fromJson(obj.toString(), ErrorModel::class.java)
                                    listener.onError(errorModel)
                                }

                                override fun onExpired() {
                                }
                            })
                        } else {
                            var msg = ""
                            if (it.errorBody != null) {
                                val body = JSONObject(it.errorBody)
                                msg = body.optString("message", "เกิดข้อผิดพลาดบางอย่างกรุณาลองใหม่")
                            }

                            val obj = JSONObject().put("status", it.errorCode)
                                .put("msg", msg)
                            val errorModel = Gson().fromJson(obj.toString(), ErrorModel::class.java)
                            listener.onError(errorModel)

                            Log.e("ERROR", "onError code : " + errorModel.error_code + ", msg : " + errorModel.msg)
                        }
                    }
                }
            }
        }

        interface NetworkLisener<T> {
            fun onResponse(response: T)
            fun onError(errorModel: ErrorModel)
            fun onExpired()
        }

        private fun showLoadingDialog() {
            progressdialog = ProgressDialog(Util.activityContext)
            progressdialog?.setMessage("Please Wait....")
            progressdialog?.show()
        }

        private fun hideLoadingDialog() {
            try {
                progressdialog?.let {
                    if (it.isShowing) {
                        it.dismiss()
                    }
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }
}