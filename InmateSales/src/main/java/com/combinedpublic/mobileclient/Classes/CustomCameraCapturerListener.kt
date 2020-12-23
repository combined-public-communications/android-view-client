package com.combinedpublic.mobileclient.Classes

import android.util.Log
import com.twilio.video.CameraCapturer

class CustomCameraCapturerListener: CameraCapturer.Listener {

    private val LOG_TAG = "CustomCameraCapturerListener"

    override fun onFirstFrameAvailable() {
        Log.d(LOG_TAG, "onFirstFrameAvailable")
    }

    override fun onCameraSwitched(newCameraId: String) {
        Log.d(LOG_TAG, "onCameraSwitched : $newCameraId")
    }

    override fun onError(errorCode: Int) {
        Log.d(LOG_TAG, "errorCode : $errorCode")
    }
}