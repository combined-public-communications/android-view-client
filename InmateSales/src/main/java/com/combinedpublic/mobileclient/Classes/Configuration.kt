package com.combinedpublic.mobileclient.Classes

import android.os.Build
import android.util.Log

class Configuration {
    companion object CombinedPublic {
        fun IceServerRequestUrl() : String = "https://view.inmatesales.com/visitation/twilio.aspx/"
        fun urlWebSite() : String = "https://view.inmatesales.com"
        fun urlForgotPassword() : String = "https://www.inmatesales.com"
        fun urlAddFunds() : String = "https://www.inmatesales.com"
        fun emailAddress() : String = "development@combinedpublic.com"
        fun wSocket() : String = "wss://wss-view.inmatesales.com:8185"
        fun turnRequestUrl() : String = "https://view.inmatesales.com/visitation/twilio.aspx"
        fun turnRequestBaseUrl() : String = "https://view.inmatesales.com/visitation"

        fun getOSInfo(): String {
            val versionRelease = Build.VERSION.RELEASE
            val osInfo = "android " + versionRelease
            Log.d("Configuration", "- getOSInfo - : $osInfo")
            return osInfo
        }

        fun isActivityVideoCallVisible(): Boolean {
            return activityVideoCallVisible
        }
        fun isActivityCallingVisible(): Boolean {
            return activityCallingVisible
        }

        fun activityVideoCallResumed() {
            activityVideoCallVisible = true
        }

        fun activityVideoCallPaused() {
            activityVideoCallVisible = false
        }

        fun activityCallingResumed() {
            activityCallingVisible = true
        }

        fun activityCallingPaused() {
            activityCallingVisible = false
        }

        private var activityVideoCallVisible: Boolean = false
        private var activityCallingVisible: Boolean = false

        fun isEmulator(): Boolean {
            return (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                    || "google_sdk" == Build.PRODUCT)
        }
    }
}