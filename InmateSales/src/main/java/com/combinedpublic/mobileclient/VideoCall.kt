package com.combinedpublic.mobileclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.beust.klaxon.Klaxon
import com.combinedpublic.mobileclient.Classes.CallManager
import com.combinedpublic.mobileclient.Classes.Configuration
import com.combinedpublic.mobileclient.WebRTC.AppRTCAudioManager
import com.combinedpublic.mobileclient.WebRTC.AppRTCClient
import com.combinedpublic.mobileclient.WebRTC.CallFragment.OnCallEvents
import com.combinedpublic.mobileclient.WebRTC.PeerConnectionClient
import com.combinedpublic.mobileclient.services.newCandidate
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*

class VideoCall : AppCompatActivity(), PeerConnectionClient.PeerConnectionEvents, OnCallEvents {


    var LOG_TAG = "VideoCallLogs"

    var hangUpBtn:ImageButton? = null
    var speakerBtn:ImageButton? = null
    var fullScreenBtn:ImageButton? = null
    var switchCamBtn:ImageButton? = null
    var isDebugging = true

    lateinit var loadingPanelVideoCall: RelativeLayout

    lateinit var localVideoView: SurfaceViewRenderer
    lateinit var remoteVideoView: SurfaceViewRenderer

    var rootEglBase: EglBase? = null
    private var audioManager: AppRTCAudioManager? = null

    var peerIceServers: MutableList<PeerConnection.IceServer> = ArrayList()
    var iceConnected: Boolean = false

    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    // True if local view is in the fullscreen renderer.
    private var isSwappedFeeds: Boolean = false
    private var activityRunning: Boolean = true
    private var isFullScreen: Boolean = true
    // Controls
    private var isAnswerSended: Boolean = false

    private var remoteProxyRenderer = ProxyVideoSink()
    private var localProxyVideoSink = ProxyVideoSink()

    private var peerConnectionClient: PeerConnectionClient? = null

    private var signalingParameters: AppRTCClient.SignalingParameters? = null

    private var remoteSinks = mutableListOf<VideoSink>()

    private var  peerConnectionParameters: PeerConnectionClient.PeerConnectionParameters? = null

    val EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL"
    val EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS"
    val EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE"
    val EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC"
    val EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC"
    val EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC"
    val EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE"
    val EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC"
    val EXTRA_NOAUDIOPROCESSING_ENABLED = "org.appspot.apprtc.NOAUDIOPROCESSING"
    val EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP"
    val EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED = "org.appspot.apprtc.SAVE_INPUT_AUDIO_TO_FILE"
    val EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES"
    val EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC"
    val EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC"
    val EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS"
    val EXTRA_DISABLE_WEBRTC_AGC_AND_HPF = "org.appspot.apprtc.DISABLE_WEBRTC_GAIN_CONTROL"
    val EXTRA_ENABLE_RTCEVENTLOG = "org.appspot.apprtc.ENABLE_RTCEVENTLOG"
    val EXTRA_USE_LEGACY_AUDIO_DEVICE = "org.appspot.apprtc.USE_LEGACY_AUDIO_DEVICE"

    private class ProxyVideoSink : VideoSink {
        private var target: VideoSink? = null

        @Synchronized
        override fun onFrame(frame: VideoFrame) {
            if (target == null) {
                Log.d("VideoCall", "Dropping frame in proxy because target is null.")
                return
            }

            target!!.onFrame(frame)
        }

        @Synchronized
        fun setTarget(target: VideoSink) {
            this.target = target
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "confCompleted") {
                confCompleted()
            }
            else if (action == "suspendApp") {
                //some func
            }
            else if (action == "unSuspendApp") {
                //some func
            }
            else if (action == "receivedOffer") {
                val offerMsg = JSONObject(intent.getStringExtra("offer"))
                Log.d(LOG_TAG,"Received Offer type is :"+offerMsg["type"])
                if (offerMsg["type"] == "offer") {

                    if (!CallManager.getInstance()._isInitiator) {
                        val sessionD = SessionDescription(SessionDescription.Type.OFFER, offerMsg["sdp"].toString())
                        onRemoteDescription(sessionD)
                      } else {
                        Log.d(LOG_TAG,"Err: Received offer for call receiver")
                      }

                } else {

                    if (CallManager.getInstance()._isInitiator) {
                        val sessionD = SessionDescription(SessionDescription.Type.ANSWER, offerMsg["sdp"].toString())
                        onRemoteDescription(sessionD)
                    } else {
                        Log.d(LOG_TAG,"Err: Received answer for call initiator")
                    }
                }
            }
            else if (action == "receivedCandidate") {

                val candidateJson = intent.getStringExtra("candidate")

                //Log.d(LOG_TAG, "candidate is : $candidateJson")
                var candidateMsg: newCandidate? = null
                try{
                    candidateMsg = Klaxon()
                            .parse<newCandidate>(candidateJson)

                    if (candidateMsg!!.payload != null) {

                        val candidate = IceCandidate(candidateMsg.payload!!.sdpMid.toString(),
                                candidateMsg.payload!!.sdpMLineIndex!!.toInt(),
                                candidateMsg.payload!!.candidate)
                        onRemoteIceCandidate(candidate)

                    } else {
                        Log.d(LOG_TAG,"Error parsing ICE candidate message.")
                    }

                } catch (e: JSONException) {
                    Log.d(LOG_TAG,"error is "+e.toString())
                }
            }
            else if (action == "reject") {
                didHangUp()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val filter = IntentFilter()
        val filters = listOf<String>("suspendApp", "unSuspendApp", "confCompleted", "receivedOffer", "receivedCandidate", "reject")
        for (item in filters) {
            filter.addAction(item)
        }
        registerReceiver(receiver, filter)

        super.onCreate(savedInstanceState)
        //Thread.setDefaultUncaughtExceptionHandler( UnhandledExceptionHandler(this))

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility
        setContentView(R.layout.activity_video_call)

        iceConnected = false
        signalingParameters = null

        initUi()
        //Block all ui
        confStarted()

        initVideos()
        getIceServers()
        CallManager.getInstance().isStarted = true

        peerConnectionParameters =
                PeerConnectionClient.PeerConnectionParameters(intent.getBooleanExtra(EXTRA_VIDEO_CALL, true), false,//loopback or true
                        false, videoWidth, videoHeight, intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                        intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0), intent.getStringExtra(EXTRA_VIDEOCODEC),
                        intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                        intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false),
                        intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0), intent.getStringExtra(EXTRA_AUDIOCODEC),
                        intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
                        intent.getBooleanExtra(EXTRA_ENABLE_RTCEVENTLOG, false),
                        intent.getBooleanExtra(EXTRA_USE_LEGACY_AUDIO_DEVICE, false), null)

        // Create peer connection client.
        peerConnectionClient =  PeerConnectionClient(
                applicationContext, rootEglBase, peerConnectionParameters!!, this)
        val options = PeerConnectionFactory.Options()

        peerConnectionClient!!.createPeerConnectionFactory(options)

        startCall()
    }

    override fun onLocalDescription(sdp: SessionDescription?) {
        Log.d(LOG_TAG,"onLocalDescription : implemented")
        runOnUiThread {
            showToast("Sending " + sdp!!.type)
            if (CallManager.getInstance()._isInitiator) {
                sendOfferSdp(sdp!!)
            } else {
                sendAnswerSdp(sdp!!)
            }
            if (peerConnectionParameters!!.videoMaxBitrate > 0) {
                Log.d(LOG_TAG, "Set video maximum bitrate: " + peerConnectionParameters!!.videoMaxBitrate)
                peerConnectionClient!!.setVideoMaxBitrate(peerConnectionParameters!!.videoMaxBitrate)
            }
        }
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d(LOG_TAG,"onIceCandidate :  implemented")
        runOnUiThread {
            //onRemoteIceCandidate(candidate!!)
            sendLocalIceCandidate(candidate!!)
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        Log.d(LOG_TAG,"onIceCandidatesRemoved : not implemented")
    }

    override fun onIceConnected() {
        Log.d(LOG_TAG,"onIceConnected :  implemented")
        runOnUiThread {
            showToast("ICE connected")
            iceConnected = true
            callConnected()
        }
    }

    override fun onIceFailed() {
        Log.d(LOG_TAG,"onIceFailed :  implemented")
        runOnUiThread{
            startCall()
        }
    }

    override fun onIceDisconnected() {
        Log.d(LOG_TAG,"onIceDisconnected :  implemented")
        runOnUiThread {
            showToast("ICE disconnected")
            iceConnected = false
            didHangUp()
        }
    }

    override fun onPeerConnectionClosed() {
        Log.d(LOG_TAG,"onPeerConnectionClosed : not implemented")
        runOnUiThread {
            iceConnected = false
            didHangUp()
        }
    }

    override fun onPeerConnectionStatsReady(reports: Array<out StatsReport>?) {
        Log.d(LOG_TAG,"onPeerConnectionStatsReady : not implemented")
    }

    override fun onPeerConnectionError(description: String?) {
        Log.d(LOG_TAG,"onPeerConnectionError : not implemented")
    }

    private fun initUi() {

        loadingPanelVideoCall = findViewById(R.id.loadingPanelVideoCall)
        localVideoView = findViewById(R.id.local_gl_surface_view)
        remoteVideoView = findViewById(R.id.remote_gl_surface_view)

        hangUpBtn = findViewById(R.id.hangUpBtn)
        hangUpBtn!!.setOnClickListener {
            didHangUp()
        }

        switchCamBtn = findViewById(R.id.switchCamBtn)
        switchCamBtn!!.setOnClickListener {
            switchCamera()
        }

        speakerBtn = findViewById(R.id.speakerBtn)
        speakerBtn!!.setOnClickListener {
            switchSpeaker()
        }

        fullScreenBtn = findViewById(R.id.fullScreenBtn)
        fullScreenBtn!!.setOnClickListener {
            switchFullScreen()
        }

        // Swap feeds on local view click.
        localVideoView.setOnClickListener { setSwappedFeeds(!isSwappedFeeds) }

        remoteSinks.add(remoteProxyRenderer)

    }

    private fun initVideos() {
         var  intent = intent
        rootEglBase = EglBase.create()

        localVideoView.init(rootEglBase!!.eglBaseContext, null)
        localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

        remoteVideoView.init(rootEglBase!!.eglBaseContext, null)
        remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        remoteVideoView.setEnableHardwareScaler(true)

        localVideoView.setZOrderMediaOverlay(true)
        localVideoView.setEnableHardwareScaler(true)
        remoteVideoView.setEnableHardwareScaler(false /* enabled */)

        // Start with local feed in remoteVideo and swap it to the localVideo when the call is connected.
        setSwappedFeeds(true)


    }

    private fun startCall() {

        if (CallManager.getInstance()._isInitiator == null) {
            didHangUp()
        }

        var iceCandidates = listOf<IceCandidate>()

        var signalingParameters = AppRTCClient.SignalingParameters(peerIceServers,
                CallManager.getInstance()._isInitiator,
                "",
                "",
                "",
                CallManager.getInstance().offerSdp,
                iceCandidates)

        var videoCapturer:VideoCapturer? = null
    if (peerConnectionParameters!!.videoCallEnabled) {
      videoCapturer = createVideoCapturer()
    }
         peerConnectionClient!!.createPeerConnection(
        localProxyVideoSink, remoteSinks, videoCapturer, signalingParameters)
    if (CallManager.getInstance()._isInitiator) {
      showToast("Creating OFFER...")
      // Create offer. Offer SDP will be sent to answering client in
      // PeerConnectionEvents.onLocalDescription event.
      peerConnectionClient!!.createOffer()
    } else {
      if (CallManager.getInstance().offerSdp != null) {
          Log.d(LOG_TAG,"callManager.offerSdp is not null , set remoteDescription")
        peerConnectionClient!!.setRemoteDescription(CallManager.getInstance().offerSdp)
          showToast("Creating ANSWER...")
        // Create answer. Answer SDP will be sent to offering client in
        // PeerConnectionEvents.onLocalDescription event.
        peerConnectionClient!!.createAnswer()
      }
      if (signalingParameters.iceCandidates != null) {
        // Add remote ICE candidates from room.
        for ( iceCandidate in signalingParameters.iceCandidates) {
          peerConnectionClient!!.addRemoteIceCandidate(iceCandidate)
        }
      }
    }

        // Create and audio manager that will take care of audio routing,
    // audio modes, audio device enumeration etc.
    audioManager = AppRTCAudioManager.create(applicationContext)
    // Store existing audio settings and change audio mode to
    // MODE_IN_COMMUNICATION for best possible VoIP performance.
    Log.d(LOG_TAG, "Starting the audio manager...")
    // This method will be called each time the number of available audio
        // devices has changed.
    audioManager!!.start { selectedAudioDevice, availableAudioDevices -> onAudioManagerDevicesChanged(selectedAudioDevice,availableAudioDevices) }

    }


    private fun getIceServers() {
        peerIceServers = CallManager.getInstance().makeTurnServerRequestToURL()
        if (peerIceServers.isEmpty()) {
            Log.d(LOG_TAG,"Ice-servers var is empty")
            //TODO: reCALL TURN REQUEST OR CLOSE CALL AND hangUP
            didHangUp()
        } else {
            for (iceServer in peerIceServers){
                Log.d(LOG_TAG,"IceSerer is:"+iceServer.hostname+
                        "\nPassword is:"+iceServer.password+
                        "\nUrls is:"+iceServer.urls)
            }
        }
        /* else {
            for (iceServer in iceServers) {
                if (iceServer.password == null) {
                    val peerIceServer = PeerConnection.IceServer.builder(iceServer.urls).createIceServer()
                    peerIceServers.add(peerIceServer)
                } else {
                    /*val peerIceServer = PeerConnection.IceServer.builder(iceServer.urls)
                            .setUsername(iceServer.username)
                            .setPassword(iceServer.password)
                            .createIceServer()*/
                    val peerIceServer = PeerConnection.IceServer(iceServer.uri,iceServer.username,iceServer.password)


                    /*val peerIceServer = PeerConnection.IceServer.builder(iceServer.urls)

                            .setUsername(iceServer.username)
                            .setPassword(iceServer.password)
                            .createIceServer()*/

                    peerIceServers.add(peerIceServer)
                }
            }
        }*/
    }

    private fun setSwappedFeeds(isSwappedFeeds:Boolean) {
        Log.d(LOG_TAG, "setSwappedFeeds: " + isSwappedFeeds)
        Logging.d(LOG_TAG, "setSwappedFeeds: $isSwappedFeeds")
        this.isSwappedFeeds = isSwappedFeeds
        localProxyVideoSink.setTarget(if (isSwappedFeeds) remoteVideoView else localVideoView)
        remoteProxyRenderer.setTarget(if (isSwappedFeeds) localVideoView else remoteVideoView)
        remoteVideoView.setMirror(isSwappedFeeds)
        localVideoView.setMirror(!isSwappedFeeds)
  }

    private fun createVideoCapturer(): VideoCapturer? {
        val videoCapturer: VideoCapturer?

        if (useCamera2()) {
            if (!captureToTexture()) {
                reportError(getString(R.string.camera2_texture_only_error))
                return null
            }
            Logging.d(LOG_TAG, "Creating capturer using camera2 API.")
            val enumerator1 = Camera2Enumerator(this)
            videoCapturer = createCameraCapturer(enumerator1)
        } else {
            Logging.d(LOG_TAG, "Creating capturer using camera1 API.")
            val enumerator2 =Camera1Enumerator(captureToTexture())
            videoCapturer = createCameraCapturer(enumerator2)
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera")
            return null
        }
        //videoCapturer = createCameraCapturer()
        return videoCapturer
    }

    /**
     * Closing up - normal hangup and app destroye
     */

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e:IllegalArgumentException) {
            Log.d(LOG_TAG,"IllegalArgumentException : "+e.message)
        }
        super.onDestroy()
    }

    private fun didHangUp() {
        if (CallManager.getInstance()._conversationId != null) {

            sendMsg("hangUpMessage")

            isAnswerSended = false

            activityRunning = false
            remoteProxyRenderer.setTarget(VideoSink { null })
            localProxyVideoSink.setTarget(VideoSink { null })

            //closeCameraDevice()

            if (localVideoView != null) {
                localVideoView.release()
            }
            if (remoteVideoView != null) {
                remoteVideoView.release()
            }
            if (peerConnectionClient != null) {
                peerConnectionClient!!.close()
                peerConnectionClient = null
            }

            if (audioManager != null) {
                audioManager!!.stop()
                audioManager = null
            }

            if (iceConnected) {
                setResult(RESULT_OK)
            } else {
                setResult(RESULT_CANCELED)
            }


            CallManager.getInstance().isStarted = false
            if (CallManager.getInstance().isMinimized){
             sendMsg("appPause")
            }
            finish()

            intent.action = "unSuspendApp"
            sendBroadcast(intent)

        }
    }

    // CallFragment.OnCallEvents interface implementation.
    override fun onCallHangUp() {
        showToast("onCallHangUp")
        didHangUp()
    }

    override fun onCameraSwitch() {
        showToast("onCameraSwitch")
        if (peerConnectionClient != null) {
            peerConnectionClient!!.switchCamera()
        }
    }

    override fun onVideoScalingSwitch(scalingType: RendererCommon.ScalingType?) {
        showToast("onVideoScalingSwitch")
        remoteVideoView.setScalingType(scalingType)
    }

    override fun onCaptureFormatChange(width: Int, height: Int, framerate: Int) {
        showToast("onCaptureFormatChange")
        if (peerConnectionClient != null) {
            peerConnectionClient!!.changeCaptureFormat(width, height, framerate)
        }
    }

    override fun onToggleMic(): Boolean {
        showToast("onToggleMic")
        return true
    }

    private fun confCompleted() {
        loadingPanelVideoCall.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun confStarted() {
        loadingPanelVideoCall.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun switchCamera() {

        if (peerConnectionClient!!.videoCapturer != null) {
            try {
                if (peerConnectionClient!!.videoCapturer is CameraVideoCapturer) {
                    Log.d(LOG_TAG, "Switch camera")
                    var cameraVideoCapturer = peerConnectionClient!!.videoCapturer as CameraVideoCapturer
                    cameraVideoCapturer.switchCamera(null)
                } else {
                    Log.d(LOG_TAG, "Will not switch camera, video caputurer is not a camera")
                }
            } catch (err:Exception) {
                Log.d(LOG_TAG,"Camera Switch Error is : "+err.message)
            }

        }
    }

    private fun switchSpeaker() {

        if (audioManager!= null ) {
            Log.d("AudioDeviceSwitcher",audioManager!!.audioDevices.count().toString()+"audio devices is available")
            if (audioManager!!.audioDevices.count() > 1) {

                val devices = audioManager!!.audioDevices.toTypedArray()
                val devicesArray = mutableListOf<String>()
                for (i in 0..(devices.count()-1)) {
                    devicesArray.add(devices[i].name)
                }
                val simpleDevicesArray = devicesArray.toTypedArray()
                Log.d("AudioDeviceSwitcher","simpleDevicesArray is : "+simpleDevicesArray.toString())

                var builder = AlertDialog.Builder(this)
                builder.setTitle("Pick output")
                builder.setItems(simpleDevicesArray) {
                    dialog, which ->  Log.d("AudioDeviceSwitcher","Selected device is : "+simpleDevicesArray[which])
                    selectAudioDevice(simpleDevicesArray[which])

                    dialog.cancel()
                }
                builder.setNegativeButton("Cancel") {
                    dialog, whichButton ->  dialog.cancel()
                }
                var alert = builder.create()
                alert.show()
            }
            else if (audioManager!!.audioDevices.count() == 1) {
                showToast("Only one audio device is available")
            }
            else {
                showToast("We can't find available audio devices")
            }
        } else {
            showToast("We can't find available audio devices")
        }


    }

    private fun selectAudioDevice(device:String) {
        when (device) {
            "SPEAKER_PHONE" -> {audioManager!!.selectAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
            audioManager!!.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)}

            "EARPIECE" -> {audioManager!!.selectAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE)
                audioManager!!.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE)}

            "WIRED_HEADSET" -> {audioManager!!.selectAudioDevice(AppRTCAudioManager.AudioDevice.WIRED_HEADSET)
                audioManager!!.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.WIRED_HEADSET)}

            "BLUETOOTH" -> {audioManager!!.selectAudioDevice(AppRTCAudioManager.AudioDevice.BLUETOOTH)
                audioManager!!.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.BLUETOOTH)}

            "NONE" -> {audioManager!!.selectAudioDevice(AppRTCAudioManager.AudioDevice.NONE)
                audioManager!!.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.NONE)}
        }
    }

    private fun switchFullScreen() {
       isFullScreen = !isFullScreen
        if (isFullScreen) {
                fullScreenBtn!!.background = null
                fullScreenBtn!!.setBackgroundResource(R.drawable.full_screen_return)
                remoteVideoView.setEnableHardwareScaler(false)
                remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT,
                        RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                remoteVideoView.refreshDrawableState()

        } else {
                fullScreenBtn!!.background = null
                fullScreenBtn!!.setBackgroundResource(R.drawable.full_screen)
                remoteVideoView.setEnableHardwareScaler(true)
                remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                        RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                remoteVideoView.refreshDrawableState()
        }

    }

    // Should be called from ui thread
    private fun callConnected() {
        Log.i(LOG_TAG,"Call connected")

        val intent = Intent()
        intent.action = "sendCallconnected"
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
        setSwappedFeeds(false /* isSwappedFeeds */)
        confCompleted()
    }

    // This method is called when the audio manager reports audio device change,
  // e.g. from wired headset to speakerphone.
    private fun onAudioManagerDevicesChanged(device: AppRTCAudioManager.AudioDevice, availableDevices: Set<AppRTCAudioManager.AudioDevice> ) {
    Log.d(LOG_TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
            + "selected: " + device)
  }

    private fun useCamera2(): Boolean {
        return Camera2Enumerator.isSupported(this)
    }

    private fun captureToTexture(): Boolean {
        return true
    }

    @MainThread private fun closeCameraDevice() {
    if (peerConnectionClient!!.videoCapturer != null) {
        try {
            if (peerConnectionClient!!.videoCapturer is CameraVideoCapturer) {
                Log.d(LOG_TAG, "Close camera")
                var cameraVideoCapturer = peerConnectionClient!!.videoCapturer as CameraVideoCapturer
                cameraVideoCapturer.stopCapture()
            } else {
                Log.d(LOG_TAG, "Will not close camera, video caputurer is not a camera")
            }
        } catch (err:Exception) {
            Log.d(LOG_TAG,"Camera Closing Error is : "+err.message)
        }
    }
}

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        // First, try to find front facing camera
        Logging.d(LOG_TAG, "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(LOG_TAG, "Creating front facing camera capturer.")
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        // Front facing camera not found, try something else
        Logging.d(LOG_TAG, "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(LOG_TAG, "Creating other camera capturer.")
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null

        /*var videoCapturer:VideoCapturer? = null
        Log.d(LOG_TAG, "Creating capturer using camera2 API.")

        // Creating camera capturer
        var enumerator = Camera2Enumerator(this)
        val deviceNames = enumerator.deviceNames
        Log.d(LOG_TAG, "Looking for back facing cameras.")


        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                Log.d(LOG_TAG, "Creating back facing camera capturer.")
                videoCapturer = enumerator.createCapturer(deviceName, null)
                break
            }
        }

        if (videoCapturer == null) {
            Log.e(LOG_TAG, "Failed to open camera.")
            return null
        }
        return videoCapturer*/
    }

    // Activity interfaces
    override fun onStop() {
        super.onStop()
        activityRunning = false
        Configuration.CombinedPublic.activityVideoCallPaused()
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null) {
            peerConnectionClient!!.stopVideoSource()
        }
    }

    override fun onStart() {
        super.onStart()
        activityRunning = true
        Configuration.CombinedPublic.activityVideoCallResumed()
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient!!.startVideoSource()
        }
    }

    override fun onPause() {
        sendMsg("stopSound")
        sendMsg("appPause")
        CallManager.getInstance().isMinimized = true
        super.onPause()
        Configuration.CombinedPublic.activityVideoCallPaused()
    }

    override fun onResume() {
        sendMsg("stopSound")
        sendMsg("appResume")
        CallManager.getInstance().isMinimized = false
        super.onResume()
        Configuration.CombinedPublic.activityVideoCallResumed()
    }

    fun sendMsg(str: String){
        val intent = Intent()
        intent.action = str
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
    }

    private fun reportError(errorMessage: String) {
    Log.e(LOG_TAG, "Peerconnection error: $errorMessage")
  }

    private fun sendOfferSdp(sdp:SessionDescription) {
        val intent = Intent()
        intent.action = "sendOffer"
        intent.putExtra("type", "offer")
        intent.putExtra("sdp", sdp.description)
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
    }

    private fun sendAnswerSdp(sdp:SessionDescription) {
        val intent = Intent()
        intent.action = "sendOffer"
        intent.putExtra("type", "answer")
        intent.putExtra("sdp", sdp.description)
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
    }

    private fun sendLocalIceCandidate(candidate: IceCandidate) {
            Log.d(LOG_TAG,"onIceCandidate : sendCandidate")
            val intent = Intent()
            intent.action = "sendCandidate"
            intent.putExtra("candidate", candidate.sdp)
            intent.putExtra("sdpMid", candidate.sdpMid)
            intent.putExtra("sdpMLineIndex", candidate.sdpMLineIndex.toLong())
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            sendBroadcast(intent)
    }

    fun onRemoteIceCandidate(candidate: IceCandidate) {
        runOnUiThread {
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received ICE candidate for a non-initialized peer connection.");
            } else {
                peerConnectionClient!!.addRemoteIceCandidate(candidate)
            }
        }
    }

    fun onRemoteDescription(sdp: SessionDescription) {
        runOnUiThread{
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received remote SDP for non-initilized peer connection.")
            } else {
                Log.d(LOG_TAG,"Received remote " + sdp.type)
                peerConnectionClient!!.setRemoteDescription(sdp)
                if (!CallManager.getInstance()._isInitiator) {
                    showToast("Creating ANSWER...")
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient!!.createAnswer()
                }
            }
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        //didHangUp()
    }

    /**
     * Util Methods
     */

    private fun showToast(msg: String) {
        Log.d(LOG_TAG,msg)
        try {
            runOnUiThread { Toast.makeText(this@VideoCall, msg, Toast.LENGTH_SHORT).show() }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }


    /*override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
    }*/

}


