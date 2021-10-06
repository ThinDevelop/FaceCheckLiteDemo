package com.kachen.facechecklitedemo.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecognitionListViewModel : ViewModel() {

    // This is a LiveData field. Choosing this structure because the whole list tend to be updated
    // at once in ML and not individual elements. Updating this once for the entire list makes
    // sense.
    private val resultObject = MutableLiveData<ResultObject>()
    val recognitionList: LiveData<ResultObject> = resultObject

    fun updateData(recognitions: List<Recognition>, face: Bitmap){
        resultObject.postValue(ResultObject(recognitions, face))
    }

}

/**
 * Simple Data object with two fields for the label and probability
 */
data class Recognition(val label:String, val confidence:Float) {

    // For easy logging
    override fun toString():String{
        return "$label / $probabilityString"
    }

    // Output probability as a string to enable easy data binding
    val probabilityString = String.format("%.1f%%", confidence * 100.0f)

}
data class ResultObject(val recognitions: List<Recognition>, val face: Bitmap) {

    // For easy logging
    override fun toString():String{
        return recognitions.toString()
    }

}