package com.combinedpublic.mobileclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.combinedpublic.mobileclient.Classes.CpcApplication
import com.combinedpublic.mobileclient.Classes.User
import com.combinedpublic.mobileclient.services.ConnectionManager

class ServiceTriggerActivity : AppCompatActivity() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "service is up") {
                finishActivity(0)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        val filter = IntentFilter()
        filter.addAction("service is up")
        registerReceiver(receiver, filter)

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (CpcApplication.IS_APP_IN_FOREGROUND && !User.getInstance()._isService) {
            startService(Intent(this, ConnectionManager::class.java))
        }
    }
}
