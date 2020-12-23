package com.combinedpublic.mobileclient.Classes

import android.util.Log
import com.twilio.video.Camera2Capturer
import com.twilio.video.Camera2Capturer.Listener

class CustomCamera2CapturerListener: Listener {

    private val LOG_TAG = "CustomCamera2CapturerListener"

    override fun onFirstFrameAvailable() {
        Log.d(LOG_TAG, "onFirstFrameAvailable")
    }

    override fun onCameraSwitched(newCameraId: String) {
        Log.d(LOG_TAG, "onCameraSwitched : $newCameraId")
    }

    override fun onError(camera2CapturerException: Camera2Capturer.Exception) {
        Log.d(LOG_TAG, "errorCode : $camera2CapturerException")
    }

}