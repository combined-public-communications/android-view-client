package com.combinedpublic.mobileclient

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.combinedpublic.mobileclient.Classes.CallManager
import com.combinedpublic.mobileclient.Classes.Configuration
import com.combinedpublic.mobileclient.Classes.User


class Calling : AppCompatActivity() {

    var mediaPlayer: MediaPlayer? = null
    internal val LOG_TAG = "CallingLogs"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "reject") {
                val text = intent.getStringExtra("text")
                if (text != null && text != "") {
                    showToast(text)
                }
                closeCalling()
            }
            else if (action == "manuallyCloseCalling"){
                closeCalling()
            }
            else if (action == "stopSound"){
                stopSound()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        Configuration.activityCallingResumed()
    }

    override fun onStop() {
        super.onStop()
        Configuration.activityCallingPaused()
    }

    override fun onDestroy() {
        Configuration.activityCallingPaused()
        stopSound()
        super.onDestroy()
        Log.d(LOG_TAG, "onDestroy")
        unregisterReceiver(receiver)

        User.getInstance()._isCallingShowed = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter()
        filter.addAction("reject")
        filter.addAction("manuallyCloseCalling")
        filter.addAction("stopSound")
        registerReceiver(receiver, filter)

        playSound()
        setContentView(R.layout.activity_calling)

        val myIntent = intent
        if (myIntent.hasExtra("isInitiator")) {
            val isInitiator = myIntent.getBooleanExtra("isInitiator",true)
            val incomLbl = findViewById<TextView>(R.id.incomLbl)
            val cancelBtn = findViewById<Button>(R.id.CancelBtn)

            if (isInitiator) {
                cancelBtn.visibility = View.VISIBLE
                incomLbl.text = "Calling"
            } else {
                cancelBtn.visibility = View.GONE
                incomLbl.text = "Calling From"
            }
        }
        if (myIntent.hasExtra("contactName")) {
            val contactName = myIntent.getStringExtra("contactName")
            val nameLbl = findViewById<TextView>(R.id.nameLbl)
            nameLbl.text = contactName
        }

        val declineBtn = findViewById<Button>(R.id.DeclineBtn)
        declineBtn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                didHangUp()
            }
        })

        val acceptBtn = findViewById<Button>(R.id.AcceptBtn)
        acceptBtn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                stopSound()
                val intent = Intent()
                intent.action = "send_ra_answer"
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                sendBroadcast(intent)
                intent.action = "suspendApp"
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                sendBroadcast(intent)
                closeCalling()
            }
        })

        val cancelBtn = findViewById<Button>(R.id.CancelBtn)
        cancelBtn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                //var callManager = CallManager.getInstance()
                Log.d(LOG_TAG,"CancelBtn didTapped")
                didHangUp()
                User.getInstance()._isCallingShowed = false

                val intent = Intent()
                intent.action = "closeCallingView"
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                sendBroadcast(intent)
            }
        })


    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }

    fun didHangUp() {
        if (CallManager.getInstance()._conversationId != null) {
            stopSound()
            val intent = Intent()
            intent.action = "hangUpMessage"
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            sendBroadcast(intent)
            val output = Intent()
            setResult(Activity.RESULT_OK, output)
            closeCalling()
        }
    }

    fun closeCalling() {
        finish()
        Configuration.activityCallingPaused()
        User.getInstance()._isCallingShowed = false
    }

    fun playSound() {

        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
        }

        if (CallManager.getInstance()._isInitiator == null) {
            return
        }

        var resID = resources.getIdentifier("telephone_ring", "raw", packageName)
        if (CallManager.getInstance()._isInitiator) {
            resID = resources.getIdentifier("dial", "raw", packageName)
        }

        mediaPlayer = MediaPlayer.create(this@Calling, resID)
        mediaPlayer!!.isLooping = true
        mediaPlayer!!.start()
    }

    fun stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
        }
    }

    override fun onPause() {
        Configuration.activityVideoCallPaused()
        sendMsg("appPause")
        CallManager.getInstance().isMinimized = true
        super.onPause()
    }

    override fun onResume() {
        sendMsg("appResume")
        CallManager.getInstance().isMinimized = false
        super.onResume()
    }

    fun sendMsg(str: String){
        val intent = Intent()
        intent.action = str
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
    }

    fun showToast(msg: String) {
        try {
            runOnUiThread { Toast.makeText(this@Calling, msg, Toast.LENGTH_SHORT).show() }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }



}
