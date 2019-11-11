package com.combinedpublic.mobileclient.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.beust.klaxon.Klaxon
import com.combinedpublic.mobileclient.Classes.CallManager
import com.combinedpublic.mobileclient.Classes.CpcApplication
import com.combinedpublic.mobileclient.Classes.User
import com.combinedpublic.mobileclient.Main
import com.combinedpublic.mobileclient.R.drawable
import com.google.gson.Gson
import com.neovisionaries.ws.client.*
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.SessionDescription
import java.net.InetAddress
import java.util.*


class ConnectionManager : Service() {

    internal val LOG_TAG = "ServiceLogs"
    var mWebSocketClient: WebSocket? = null
    var factory: WebSocketFactory? = null
    var gson = Gson()
    var user = User.getInstance()
    var userNameTemp:String? = null
    var userPassTemp:String? = null
    var notifManager:NotificationManager? = null
    var callManager = CallManager.getInstance()
    var isInternetConnection : Boolean = false


    private enum class ConnectionState { NEW, CONNECTED, CLOSED, ERROR }
    private lateinit var roomState: ConnectionState

    var getPeerListTimerNew:Timer? = null
    var getPeerListTimerTask:GetPeerList? = null


    var checkInternetConnectionTimerNew:Timer? = null
    var checkInternetConnectionTask:CheckInternetConnection? = null

    var keepAliveTimer:Timer? = null
    var keepAliveTimerTask:KeepAliveTask? = null

    var activeListenerTimer:Timer? = null
    var activeListenerTimerTask:ActiveListenerTask? = null

    //private var notificationManager: NotificationManager? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "login") {
                val login = intent.getStringExtra("login")
                val password = intent.getStringExtra("password")
                userNameTemp = login
                userPassTemp = password
                Log.d(LOG_TAG,"We Get login message log and pass is : "+login+" ,"+password)
                val register = register_name("register_name",login,User.getInstance().device,password,0)
                var message = gson.toJson(register)
                mWebSocketClient!!.sendText(message)

                suspendApp()
            }
            else if (action == "logOff") {
                val bye = bye(user.id.toString(),"test")
                val settings = getSharedPreferences("Settings", 0)
                val editor = settings.edit()
                editor.putBoolean("isLoggedIn", false)
                editor.apply()
                user.isLoggedIn = false

                var message = gson.toJson(bye)
                mWebSocketClient!!.sendText(message)
                mWebSocketClient!!.disconnect()
                roomState = ConnectionState.CLOSED

            }
            else if (action == "hangUpMessage") {

                //Log.d(LOG_TAG,"_contactId is "+ callManager._contactId)
                //Log.d(LOG_TAG,"_conversationId is "+ callManager._conversationId.toString())

                /*val hangup = hangup("hangup", user.device, user.id, callManager._contactId.toString(), callManager._conversationId.toString())
                var message = gson.toJson(hangup)
                mWebSocketClient!!.sendText(message)
                roomState = ConnectionState.CLOSED

                Log.d(LOG_TAG,"Send HangUp message")*/

                hangUp()

            }
            else if (action == "call") {

                val contactId = intent.getStringExtra("contactId")
                callManager._contactId = contactId.toLong()
                val racallMsg = ra_call("ra_call",user.device,user.id,contactId)
                var message = gson.toJson(racallMsg)
                Log.d(LOG_TAG,"Send ra_call message") //send to \""+ contactId +"\" : "+message)
                mWebSocketClient!!.sendText(message)

            }
            else if (action == "repeatRaCall") {
                if (notifManager != null) {  notifManager!!.cancel(10) }
                if (callManager.raCallMsg == null) {
                    hangUp()
                    return
                }
                continueRaCall(callManager.raCallMsg)
            }
            else if (action == "sendOffer") {

                roomState = ConnectionState.CONNECTED

                val offerType = intent.getStringExtra("type")
                val offerSdp = intent.getStringExtra("sdp")
                if (offerType != null && offerType.isNotEmpty()) {
                    //Log.d(LOG_TAG,"session desc is : "+offerSdp)

                    val payloadObj = offerPayload(offerSdp,offerType)

                    val offerMsg = offer(offerType,user.id.toLong(),
                            callManager._contactId,
                            callManager._conversationId,
                            payloadObj)
                    var message = gson.toJson(offerMsg)
                    Log.d(LOG_TAG,offerType+" message send to \""+ callManager._contactId +"\"") //: "+message)
                    mWebSocketClient!!.sendText(message)
                } else {
                    Log.d(LOG_TAG,"Can't send offer session desc is null")
                }

            }
            else if (action == "send_ra_answer") {
                sendRaAnswer()
            }
            else if (action == "sendCandidate") {

                if (roomState != ConnectionState.CONNECTED) {
                    Log.d(LOG_TAG,"Error, Sending ICE candidate in non connected state.")
                    return
                }

                val candidate = intent.getStringExtra("candidate")
                val sdpMid = intent.getStringExtra("sdpMid")
                val sdpMLineIndex = intent.getLongExtra("sdpMLineIndex",0)


                val candidateVar = Payload(candidate, sdpMid, sdpMLineIndex)

                val candidateMsg = candidate("candidate","0","","",
                        "","","",user.device,"1", "1",
                        "1", user.id.toLong(),callManager._contactId,
                        callManager._conversationId,user.id.toLong(),0,0, candidateVar)

                val message = gson.toJson(candidateMsg)
                mWebSocketClient!!.sendText(message)
                Log.d(LOG_TAG,"Send candidate")
            }
            else if (action == "sendCallconnected") {

                val callconnectedMsg = callconnected("callconnected",user.id,callManager._contactId,callManager._conversationId)

                val message = gson.toJson(callconnectedMsg)
                mWebSocketClient!!.sendText(message)
                Log.d(LOG_TAG,"Send callconnected")
            }
            else if (action == "sendAnswer") {


                roomState = ConnectionState.CONNECTED

                val answerType = intent.getStringExtra("type")
                val answerSdp = intent.getStringExtra("sdp")
                if (answerType != null && answerType.isNotEmpty()) {
                   // Log.d(LOG_TAG,"session desc is : "+answerSdp)

                    val payloadObj = offerPayload(answerSdp,answerType)

                    val answerMsg = answer(answerType,user.id.toLong(),
                            callManager._contactId,
                            callManager._conversationId,
                            payloadObj)
                    var message = gson.toJson(answerMsg)
                    Log.d(LOG_TAG,answerType+" message send to \""+ callManager._contactId +"\"")// : "+message)
                    mWebSocketClient!!.sendText(message)
                } else {
                    Log.d(LOG_TAG,"Can't send answer session desc is null")
                }

                Log.d(LOG_TAG,"Send answer")
            }
            else if (action == "appResume") {
                stopActiveListenerTask()
                //if WebSocket is onDisconnected
                if (mWebSocketClient == null || !mWebSocketClient!!.isOpen) {
                    suspendApp()
                }
            }
            else if (action == "appPause") {
                startActiveListenerTask()
            }
            else if (action == "closeCallingView") {
                if (mWebSocketClient != null && mWebSocketClient!!.state == WebSocketState.CLOSED) {
                    val intent = Intent()
                    intent.action = "manuallyCloseCalling"
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    sendBroadcast(intent)
                }
            }
            else if (action == "token"){
                val token = intent.getStringExtra("token")
                onNewToken(token)
            }
            else if (action == AppConstant.YES_ACTION) {
                if (notifManager != null) {  notifManager!!.cancel(10) }
                Log.d(LOG_TAG,"OPEN CALLED")
                Toast.makeText(context, "OPEN CALLED", Toast.LENGTH_SHORT).show()
            }
            else if (action == AppConstant.CANCEL_ACTION) {
                if (callManager._isIncommingStarted && mWebSocketClient!!.state == WebSocketState.OPEN) {hangUp()}
                if (notifManager != null) {  notifManager!!.cancel(10) }
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
                Log.d(LOG_TAG,"CANCEL CALLED")
                Toast.makeText(context, "CANCEL CALLED", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        User.getInstance()._isService = true

        roomState = ConnectionState.NEW

        val filter = IntentFilter()
        filter.addAction("login")
        filter.addAction("logOff")
        filter.addAction("hangUpMessage")
        filter.addAction("call")
        filter.addAction("sendOffer")
        filter.addAction("send_ra_answer")
        filter.addAction("sendAnswer")
        filter.addAction("sendCandidate")
        filter.addAction("sendCallconnected")
        filter.addAction("appResume")
        filter.addAction("appPause")
        filter.addAction("repeatRaCall")
        filter.addAction("closeCallingView")
        filter.addAction("token")
        filter.addAction(AppConstant.YES_ACTION)
        filter.addAction(AppConstant.CANCEL_ACTION)


        registerReceiver(receiver, filter)
        //Log.d(LOG_TAG, "onCreate")

        val intent = Intent()
        intent.action = "service is up"
        sendBroadcast(intent)


        if (this.mWebSocketClient == null || !this.mWebSocketClient!!.isOpen) {
            isInternetConnection = false
            suspendApp()
        } else {
            isInternetConnection = true
        }

        if (!user._isRestarted) {
            val settings = getSharedPreferences("Settings", 0)
            //val silent = settings.getBoolean("silentMode", false)
            user.name = settings.getString("name","")
            user.balance = settings.getString("balance","")
            user.id = settings.getString("id","")
            user.isLoggedIn = settings.getBoolean("isLoggedIn",false)
            user.userName = settings.getString("userName","")
            user.password = settings.getString("password","")
            user.device = settings.getString("device","test")
            user.token = settings.getString("token","xxxxx")
            user.pushToken = settings.getString("pushToken","xxxxx")

            User.getInstance()._isRestarted = false
        } else {
            if (mWebSocketClient != null && !CallManager.getInstance().isStarted
                    && mWebSocketClient!!.state != WebSocketState.CLOSING
                    && mWebSocketClient!!.state != WebSocketState.CLOSED
                    && callManager.isMinimized){
                mWebSocketClient!!.disconnect()
                callManager._isIncommingStarted = false
            }
        }

        //startService()

        startForeground()

        User.getInstance().device = getDeviceSuperInfo()

        factory = WebSocketFactory().setConnectionTimeout(5000)

        startClient()
        startCheckInternetConnectionTask()

    }

    private fun startForeground() {
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("ConnectionManager", "ConnectionManager")
                } else {
                    // If earlier version channel ID is not used
                    ""
                }

        val notificationBuilder = NotificationCompat.Builder(this, channelId )
        val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(drawable.icon_app_inconnect)
                .setPriority(2)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        startForeground(101, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun getDeviceSuperInfo(): String {
        var deviceStr = "empty"
        try {
            deviceStr = ""
            deviceStr += "\n OS Version: "      + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")"
            deviceStr += "\n OS API Level: "    + android.os.Build.VERSION.SDK_INT
            deviceStr += "\n Device: "          + android.os.Build.DEVICE
            deviceStr += "\n Model (and Product): " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")"
            deviceStr += "\n RELEASE: "         + android.os.Build.VERSION.RELEASE
            deviceStr += "\n BRAND: "           + android.os.Build.BRAND
            deviceStr += "\n DISPLAY: "         + android.os.Build.DISPLAY
            deviceStr += "\n UNKNOWN: "         + android.os.Build.UNKNOWN
            deviceStr += "\n HARDWARE: "        + android.os.Build.HARDWARE
            deviceStr += "\n Build ID: "        + android.os.Build.ID
            deviceStr += "\n MANUFACTURER: "    + android.os.Build.MANUFACTURER
            deviceStr += "\n USER: "            + android.os.Build.USER
            deviceStr += "\n HOST: "            + android.os.Build.HOST
            return deviceStr
        } catch (e : Exception) {
            Log.e(LOG_TAG, "Error getting Device INFO $e")
        }
        return deviceStr
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand")
        startService()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopCheckInternetConnectionTask()
        if (mWebSocketClient != null && !CallManager.getInstance().isStarted
                && mWebSocketClient!!.state != WebSocketState.CLOSING
                && mWebSocketClient!!.state != WebSocketState.CLOSED
                && callManager.isMinimized){
            mWebSocketClient!!.disconnect()
            callManager._isIncommingStarted = false
        }
        super.onDestroy()
        Log.d(LOG_TAG, "onDestroy")
        User.getInstance()._isService = false
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(LOG_TAG, "onBind")
        return null
    }

    fun startService(){
        if (CpcApplication.IS_APP_IN_FOREGROUND && !User.getInstance()._isService) {

            var intent = Intent(this, ConnectionManager::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //startForegroundService(intent)
                startForegroundService(intent)
            } else {
                startService(intent)
            }

        }
    }

    fun suspendApp() {
        val intent = Intent()
        intent.action = "suspendApp"
        sendBroadcast(intent)
    }

    fun unSuspendApp() {
        val intent = Intent()
        intent.action = "unSuspendApp"
        sendBroadcast(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.`package` = packageName

        val restartServicePendingIntent = PendingIntent.getService(applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT)
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent)

        Log.d(LOG_TAG,"onTaskRemoved")
        super.onTaskRemoved(rootIntent)
    }

    fun isInternetAvailable() : Boolean{
        return try {
            var ipAddr = InetAddress.getByName("google.com")
            !ipAddr.equals("")

        } catch (e: Exception) {
            false
        }
}


    fun startClient() {

        factory = WebSocketFactory().setConnectionTimeout(1000)
        mWebSocketClient = factory!!.createSocket(com.combinedpublic.mobileclient.Classes.Configuration.wSocket())
                .addListener(object: WebSocketAdapter() {

                    override fun onConnected(websocket: WebSocket?, headers: MutableMap<String, MutableList<String>>?) {
                        super.onConnected(websocket, headers)
                        Log.d(LOG_TAG,"WebSocket is opened ")

                        unSuspendApp()

                        if (user != null && user.isLoggedIn) {

                            val login = user.userName
                            var password = user.password
                            if (login!!.isNotEmpty() && password!!.isNotEmpty()) {
                                val intent = Intent()
                                intent.action = "login"
                                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                                intent.putExtra("login",login)
                                intent.putExtra("password",password)
                                sendBroadcast(intent)
                            }
                        }

                    }
                    override fun onTextMessage(websocket: WebSocket?, text: String?) {
                        super.onTextMessage(websocket, text)

                        if (text != null && text.isNotEmpty()) {
                            try {
                                var responseJSON = JSONObject(text)
                                Log.d("WS_Received","Message is:"+text)
                                val type = responseJSON.getString("type")
                                var status = ""
                                var statusdesc = ""

                                if (type == "registerresult") {

                                   Log.d(LOG_TAG,"Received registerresult")

                                    var isDesc:String? = null
                                    try {
                                         isDesc = responseJSON.getString("statusdesc")
                                    } catch (e:Exception) {

                                    }

                                    if (isDesc != null && isDesc != "") {

                                        var registerresultMsg: registerresultdesc? = null

                                        try{
                                            registerresultMsg = Klaxon()
                                                    .parse<registerresultdesc>(text)
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                        }
                                        Log.d(LOG_TAG,"registerresult error : "+registerresultMsg!!.statusdesc)

                                        val intent = Intent()
                                        intent.action = "unSuspendApp"
                                        intent.putExtra("toast",registerresultMsg!!.statusdesc)
                                        sendBroadcast(intent)

                                    } else {

                                        var registerresultMsg: registerresult? = null

                                        try{
                                            registerresultMsg = Klaxon()
                                                    .parse<registerresult>(text)
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                        }

                                        unSuspendApp()

                                        Log.d(LOG_TAG,"Parse registerresult , status is "+registerresultMsg!!.status)

                                        if (registerresultMsg!!.status == "ok") {

                                            user.id = registerresultMsg.id!!
                                            user.balance = registerresultMsg.balance
                                            user.name = registerresultMsg.displayname
                                            user.userName = userNameTemp
                                            user.password = userPassTemp
                                            user.isLoggedIn = true

                                            val settings = getSharedPreferences("Settings", 0)
                                            val editor = settings.edit()
                                            editor.putBoolean("isLoggedIn", true)
                                            editor.putString("userName",user.userName)
                                            editor.putString("password",user.password)
                                            editor.putString("name",user.name)
                                            editor.putString("balance",user.balance)
                                            editor.putString("id",user.id)
                                            editor.putString("device",user.device)
                                            editor.putString("token",user.token)
                                            editor.putString("pushToken",user.pushToken)
                                            editor.apply()

                                            onSuccessful()
                                        }

                                    }

                                }
                                else if (type == "getpeerlistresult") {


                                    status = responseJSON.getString("status")

                                    Log.d(LOG_TAG,"Received getpeerlistresult")

                                    val str = text.replace("\\","").replace("\"[","[").replace("]\"","]").replace("\"\"","0")

                                    Log.d(LOG_TAG,"Getpeerlistresult is "+str)

                                    if (status == "error") {

                                        statusdesc = responseJSON.getString("statusdesc")

                                        if (statusdesc == "Unauthorized") {
                                            Log.d(LOG_TAG,"Unauthorized, make logoff")
                                            val intent = Intent()
                                            intent.action = "Unauthorized"
                                            sendBroadcast(intent)
                                            return
                                        }
                                    }

                                    var getpeerlistresultMsg: getpeerlistresult? = null

                                    try{
                                        getpeerlistresultMsg = Klaxon()
                                                .parse<getpeerlistresult>(str)
                                    } catch (e: JSONException) {
                                        Log.d(LOG_TAG,"error is "+e.toString())
                                    }

                                    if (getpeerlistresultMsg!!.status == "ok") {

                                        Log.d(LOG_TAG,"getpeerlistresultMsg Status is ok")
                                        val settings = getSharedPreferences("Settings", 0)
                                        val editor = settings.edit()

                                        user.id = getpeerlistresultMsg!!.id
                                        user.balance = getpeerlistresultMsg!!.balance

                                        val intent = Intent()
                                        intent.action = "refreshContacts"
                                        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                                        intent.putExtra("Contacts",getpeerlistresultMsg!!.payload!!)
                                        sendBroadcast(intent)
                                    }

                                }
                                else if (type == "ra_call") {

                                    Log.d(LOG_TAG,"Received ra_call")

                                    callManager._isIncommingStarted = true

                                    var ra_call_rcvMsg: ra_call_rcv? = null

                                    try{
                                        ra_call_rcvMsg = Klaxon()
                                                .parse<ra_call_rcv>(text)

                                        if (ra_call_rcvMsg != null) {
                                            callManager._contactId = ra_call_rcvMsg.id!!.toLong()
                                            callManager._conversationId = ra_call_rcvMsg.conversationid!!.toLong()
                                            callManager._contactName = ra_call_rcvMsg.displayname!!
                                            callManager._isInitiator = false


                                            callManager.raCallMsg = ra_call_rcvMsg

                                            continueRaCall(ra_call_rcvMsg!!)
                                        }

                                    } catch (e: JSONException) {
                                        Log.d(LOG_TAG,"error is "+e.toString())
                                    }

                                }
                                else if (type == "deviceinforesult") {

                                    Log.d(LOG_TAG,"Received deviceinforesult")
                                }
                                else if (type == "ra_callresult") {

                                    Log.d(LOG_TAG,"Received ra_callresult")

                                    var ra_callresultMsg: ra_callresult? = null
                                    var ra_callresultErrorMsg: ra_callresult_error? = null

                                    status = responseJSON.getString("status")

                                    if (status == "ok") {
                                        try{
                                            ra_callresultMsg = Klaxon()
                                                    .parse<ra_callresult>(text)

                                            if (ra_callresultMsg!!.status == "ok") {
                                                callManager._conversationId = ra_callresultMsg!!.conversationid!!.toLong()
                                                Log.d(LOG_TAG,"ra_callresult status is ok , get conversationid")
                                            } else {
                                                Log.d(LOG_TAG,"ra_callresult status is not ok , "+ra_callresultMsg)
                                            }

                                        } catch (e: JSONException) {
                                            Log.d(LOG_TAG,"error is "+e.toString())
                                        }
                                    } else {
                                        try{
                                            ra_callresultErrorMsg = Klaxon()
                                                    .parse<ra_callresult_error>(text)

                                                Log.d(LOG_TAG,"ra_callresult status error: "+ra_callresultErrorMsg!!.statusdesc)
                                                val intent = Intent()
                                                intent.action = "reject"
                                                intent.putExtra("text",ra_callresultErrorMsg!!.statusdesc)
                                                sendBroadcast(intent)

                                            // GetPeerList Task is stopper , try to rerun

                                            stopGetPeerListTask()
                                            startGetPeerListTask()

                                        } catch (e: JSONException) {
                                            Log.d(LOG_TAG,"error is "+e.toString())
                                        }
                                    }


                                }
                                else if (type == "ra_reject") {

                                    stopKeepAliveTask()

                                    Log.d(LOG_TAG,"Received ra_reject")
                                    val intent = Intent()
                                    intent.action = "reject"
                                    sendBroadcast(intent)

                                }
                                else if (type == "hangup") {

                                    stopKeepAliveTask()

                                    Log.d(LOG_TAG,"Received hangup")
                                    val intent = Intent()
                                    intent.action = "reject"
                                    sendBroadcast(intent)

                                }
                                else if (type == "ra_answer") {

                                    Log.d(LOG_TAG,"Received ra_answer")
                                    val intent = Intent()
                                    intent.action = "reject"
                                    sendBroadcast(intent)

                                    Log.d(LOG_TAG,"Send call")

                                    val callObj = call("call", user.id.toLong(), callManager._contactId.toLong(), callManager._conversationId.toLong())
                                    var message = gson.toJson(callObj)
                                    mWebSocketClient!!.sendText(message)

                                    intent.action = "startCall"
                                    sendBroadcast(intent)

                                }
                                else if (type == "callresult") {
                                    Log.d(LOG_TAG,"Received callresult")
                                    var callresultMsg: callresult? = null
                                    try{
                                        callresultMsg = Klaxon()
                                                .parse<callresult>(text)

                                        if (callresultMsg!!.status == "ok" && callManager._conversationId.toString() == callresultMsg.conversationid) {
                                            Log.d(LOG_TAG,"callresultMsg status is ok , start KeepAlive")
                                            startKeepAliveTask()
                                        } else {
                                            Log.d(LOG_TAG,"callresultMsg bad status or unknow _conversationId , "+callresultMsg)
                                        }

                                    } catch (e: JSONException) {
                                        Log.d(LOG_TAG,"error is "+e.toString())
                                    }

                                }
                                else if (type == "keepaliveresult") {
                                    Log.d(LOG_TAG, "Received keepaliveresult")
                                }
                                else if (type == "offer") {
                                    Log.d(LOG_TAG, "Received offer")
                                    //Log.d(LOG_TAG, "Offer is : $text")
                                    var offerMsg: offer_rcv? = null
                                    try{
                                        offerMsg = Klaxon()
                                                .parse<offer_rcv>(text)

                                        if (offerMsg!!.payload != null) {

                                            callManager.offerSdp = SessionDescription(SessionDescription.Type.OFFER, offerMsg.payload!!.sdp)
                                            Log.d(LOG_TAG,"receivedOffer and saved to callManager.offerSdp !")
                                            val intent = Intent()

                                            val perm = JSONObject()
                                            perm.put("type",offerMsg.payload!!.type)
                                            perm.put("sdp", offerMsg.payload!!.sdp)

                                            intent.action = "receivedOffer"
                                            intent.putExtra("offer",perm.toString())
                                            sendBroadcast(intent)


                                        } else {
                                            Log.d(LOG_TAG,"Error parsing offer message.")
                                        }

                                    } catch (e: JSONException) {
                                        Log.d(LOG_TAG,"error is "+e.toString())
                                    }


                                }
                                else if (type == "answer") {
                                    Log.d(LOG_TAG, "Received answer")
                                    var answerMsg: answer_rcv? = null
                                    try{
                                        answerMsg = Klaxon()
                                                .parse<answer_rcv>(text)

                                        if (answerMsg!!.payload != null) {

                                            val perm = JSONObject()
                                            perm.put("type",answerMsg.payload!!.type)
                                            perm.put("sdp", answerMsg.payload!!.sdp)

                                            val intent = Intent()
                                            intent.action = "receivedOffer"
                                            intent.putExtra("offer",perm.toString())
                                            sendBroadcast(intent)


                                        } else {
                                            Log.d(LOG_TAG,"Error parsing offer message.")
                                        }

                                    } catch (e: JSONException) {
                                        Log.d(LOG_TAG,"error is "+e.toString())
                                    }
                                }
                                else if (type == "call") {
                                    Log.d(LOG_TAG, "Received call")

                                }
                                else if (type == "candidate") {

                                    Log.d(LOG_TAG, "Received candidate")

                                    val intent = Intent()
                                    intent.action = "receivedCandidate"
                                    intent.putExtra("candidate",text)
                                    sendBroadcast(intent)

                                }
                                else if (type == "logout") {
                                    Log.d(LOG_TAG,"Unauthorized because \"Logged in from another device.\" make logoff")
                                    val intent = Intent()
                                    intent.action = "UnauthorizedDevice"
                                    sendBroadcast(intent)
                                }
                                else {
                                    Log.d(LOG_TAG,"Error , received message is empty")
                                }

                                } catch (e:Exception) {
                                Log.d(LOG_TAG,"Error , received message with unknown type:"+text)
                            }

                        }

                        }

                    override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
                        super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
                        Log.d(LOG_TAG,"WebSocket is onDisconnected ")

                        val intent = Intent()
                        intent.action = "suspendApp"
                        sendBroadcast(intent)
                        stopGetPeerListTask()
                        stopKeepAliveTask()
                        if (!CallManager.getInstance().isMinimized) {
                            startClient()
                        }
                    }
                    override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
                        super.onError(websocket, cause)
                        Log.d(LOG_TAG, "WebSocket Error is: " + cause.toString())

                        val intent = Intent()
                        intent.action = "suspendApp"
                        sendBroadcast(intent)
                        if (isInternetConnection){
                            startClient()
                        }
                    }
                })
                .connectAsynchronously()
    }

    internal fun onSuccessful() {

        Log.d(LOG_TAG,"onSuccessful authorize")

        val intent = Intent()
        intent.action = "successfulLogin"
        sendBroadcast(intent)

        stopGetPeerListTask()
        startGetPeerListTask()

        sendDeviceInfo()
    }

    fun continueRaCall(raCall: ra_call_rcv) {

        if (callManager.isMinimized) {
            notifyUser(callManager._contactName)
        } else {
            if (!callManager.isStarted) {
                val intent = Intent()
                intent.action = "incomingCall"
                sendBroadcast(intent)
            }

            Log.d(LOG_TAG,callManager._contactName+" calls you ")

            var racallresultObj = ra_callresult("ra_callresult", user.id, callManager._contactId.toString(),
                    callManager._conversationId.toString(),"ok","","","","",user.name)
            var message = gson.toJson(racallresultObj)
            mWebSocketClient!!.sendText(message)
        }
    }

    fun hangUp() {

        callManager._isIncommingStarted = false
        user._isCallingShowed = false

        val hangup = hangup("hangup", user.device, user.id, callManager._contactId.toString(), callManager._conversationId.toString())
        var message = gson.toJson(hangup)
        mWebSocketClient!!.sendText(message)
        roomState = ConnectionState.CLOSED

        Log.d(LOG_TAG,"Send HangUp message")
    }

    fun notifyUser(name: String) {

        val title = "VisiTel"
        val message = "Incoming call from $name"

        val notif: Notification.Builder = Notification.Builder(applicationContext)
        if (notifManager == null) {
            notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }

        var builder:NotificationCompat.Builder? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            var mChannel = notifManager!!.getNotificationChannel("10")
            if (mChannel == null) {
                mChannel = NotificationChannel("10", title, importance)
                mChannel.enableVibration(true)
                mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                notifManager!!.createNotificationChannel(mChannel)
            }

            builder = NotificationCompat.Builder(applicationContext, "10")

            builder.setChannelId("1")

            val openReceive = Intent(this, Main::class.java)
            openReceive.action = AppConstant.YES_ACTION
            openReceive.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            val stackBuilder = TaskStackBuilder.create(this)
            stackBuilder.addParentStack(Main::class.java)
            stackBuilder.addNextIntent(openReceive)

            val cancelReceive = Intent()
            cancelReceive.action = AppConstant.CANCEL_ACTION
            val pendingIntentOpen = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT)
            val pendingIntentCancel = PendingIntent.getBroadcast(this, 12345, cancelReceive, PendingIntent.FLAG_UPDATE_CURRENT)

            builder!!.addAction(NotificationCompat.Action(android.R.drawable.sym_call_missed,"Open",pendingIntentOpen))
            builder!!.addAction(NotificationCompat.Action(android.R.drawable.sym_call_outgoing,"Cancel",pendingIntentCancel))
            builder.setContentTitle(title)                            // required
                    .setSmallIcon(android.R.drawable.sym_call_incoming)   // required ic_popup_reminder
                    .setContentText(message) // required
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntentOpen)
                    .setTicker(message)
                    .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
        } else {
            builder = NotificationCompat.Builder(applicationContext, "10")

            val openReceive = Intent(this, Main::class.java)
            openReceive.action = AppConstant.YES_ACTION
            openReceive.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            val stackBuilder = TaskStackBuilder.create(this)
            stackBuilder.addParentStack(Main::class.java)
            stackBuilder.addNextIntent(openReceive)

            val cancelReceive = Intent()
            cancelReceive.action = AppConstant.CANCEL_ACTION

            val pendingIntentCancel = PendingIntent.getBroadcast(this, 12345, cancelReceive, PendingIntent.FLAG_UPDATE_CURRENT)
            val pendingIntentOpen = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT)

            builder!!.addAction(NotificationCompat.Action(android.R.drawable.sym_call_missed,"Open",pendingIntentOpen))
            builder!!.addAction(NotificationCompat.Action(android.R.drawable.sym_call_outgoing,"Cancel",pendingIntentCancel))
            builder.setContentTitle(title)                            // required
                    .setSmallIcon(android.R.drawable.sym_call_incoming)   // required
                    .setContentText(message) // required
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntentOpen)
                    .setTicker(message)
                    .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)).priority = Notification.PRIORITY_HIGH
        }


        val notification = builder.build()
        notifManager!!.notify(10, notification)

    }

    fun sendRaAnswer () {

        val intent = Intent()
        intent.action = "startCall"
        sendBroadcast(intent)

        Log.d(LOG_TAG,"Initiate ra_answer message to send")
        val answerMsg = ra_answer_snd("ra_answer",user.id.toLong(),
                callManager._contactId,
                callManager._conversationId, "ok")
        val message = gson.toJson(answerMsg)
        mWebSocketClient!!.sendText(message)
        Log.d(LOG_TAG,"Send ra_answer and start call")

    }


    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.

        user.token = token

        sendDeviceInfo()
    }

    fun sendDeviceInfo() {
        if (User.getInstance().id.isEmpty()) {
            Log.d(LOG_TAG, "Can't send deviceinfo user 'id' is null")
            return
        }

        val deviceInfoMsg = deviceinfo("deviceinfo", User.getInstance().device,
                User.getInstance().token, User.getInstance().pushToken, User.getInstance().id)
        val message = gson.toJson(deviceInfoMsg)
        mWebSocketClient!!.sendText(message)

        Log.d(LOG_TAG,"Token is ${User.getInstance().token}")
    }

    fun startGetPeerListTask() {

        if (getPeerListTimerNew != null) {
            getPeerListTimerNew!!.cancel()
        }
        getPeerListTimerNew = Timer()
        getPeerListTimerTask = GetPeerList()

        getPeerListTimerNew?.schedule(getPeerListTimerTask, 100, 30000)
    }

    fun stopGetPeerListTask() {
        if (getPeerListTimerNew != null) {
            getPeerListTimerNew!!.cancel()
        }
    }

    fun startCheckInternetConnectionTask() {

        if (checkInternetConnectionTimerNew != null) {
            checkInternetConnectionTimerNew!!.cancel()
        }
        checkInternetConnectionTimerNew = Timer()
        checkInternetConnectionTask = CheckInternetConnection()

        checkInternetConnectionTimerNew?.schedule(checkInternetConnectionTask, 150, 2000)
    }

    fun stopCheckInternetConnectionTask() {
        if (checkInternetConnectionTimerNew != null) {
            checkInternetConnectionTimerNew!!.cancel()
        }
    }

    inner class CheckInternetConnection : TimerTask() {
        internal  val LOG_TAG = "TimerLogs"

        override fun run() {
            if (!isInternetAvailable()) {
                Log.d(LOG_TAG, "Internet Connection is Unavailable")
                isInternetConnection = false

                suspendApp()

                // TODO: STOP ALL CONNECTION REQUESTS AND ALL ACTIVITY
                if (mWebSocketClient != null){
                    mWebSocketClient!!.disconnect()
                }
            } else {

                if (mWebSocketClient != null && mWebSocketClient!!.isOpen) {
                    unSuspendApp()
                } else {
                    startClient()
                }

                isInternetConnection = true
            }
        }
    }


    inner class GetPeerList : TimerTask() {

        internal val LOG_TAG = "TimerLogs"

        override fun run() {
            Log.d(LOG_TAG,"GetPeerList Method Execute")
            val getPeerListObject = getpeerlist("getpeerlist",user.device,user.userName,user.id)
            var message = gson.toJson(getPeerListObject)
            mWebSocketClient!!.sendText(message)
        }
    }

    fun startKeepAliveTask() {

        if (keepAliveTimer != null) {
            keepAliveTimer!!.cancel()
        }
        keepAliveTimer = Timer()
        keepAliveTimerTask = KeepAliveTask()

        keepAliveTimer?.schedule(keepAliveTimerTask, 100, 30000)
    }

    fun stopKeepAliveTask() {
        if (keepAliveTimer != null) {
            keepAliveTimer!!.cancel()
        }
    }

    inner class KeepAliveTask : TimerTask() {

        internal val LOG_TAG = "TimerLogs"

        override fun run() {
            Log.d(LOG_TAG,"KeepAliveTask Method Execute")
            val keepaliveObject = keepalive("keepalive",user.device,user.id.toLong(),callManager._conversationId)
            var message = gson.toJson(keepaliveObject)
            mWebSocketClient!!.sendText(message)
        }
    }

    fun startActiveListenerTask() {
        Log.d(LOG_TAG, "startActiveListenerTask")
        if (activeListenerTimer != null) {
            activeListenerTimer!!.cancel()
        }
        activeListenerTimer = Timer()
        activeListenerTimerTask = ActiveListenerTask()

        activeListenerTimer?.schedule(activeListenerTimerTask, 60000)
    }

    fun stopActiveListenerTask() {
        Log.d(LOG_TAG, "stopActiveListenerTask")
        if (activeListenerTimer != null) {
            activeListenerTimer!!.cancel()
            callManager.isMinimized = false
            if (mWebSocketClient!!.state == WebSocketState.CLOSED
                    || mWebSocketClient!!.state == WebSocketState.CLOSING) {
                startClient()
            }
        }
    }

    inner class ActiveListenerTask : TimerTask() {

        internal val LOG_TAG = "TimerLogs"

        override fun run() {
            Log.d(LOG_TAG," ActiveListenerTask Method Execute")
            if (mWebSocketClient != null && !CallManager.getInstance().isStarted
                    && mWebSocketClient!!.state != WebSocketState.CLOSING
                    && mWebSocketClient!!.state != WebSocketState.CLOSED
                    && callManager.isMinimized){
                mWebSocketClient!!.disconnect()
                callManager._isIncommingStarted = false
            }
        }
    }

    object AppConstant {
        val YES_ACTION = "YES_ACTION"
        val CANCEL_ACTION = "CANCEL_ACTION"
    }

}
