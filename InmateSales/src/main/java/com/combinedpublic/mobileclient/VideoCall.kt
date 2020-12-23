package com.combinedpublic.mobileclient

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beust.klaxon.Klaxon
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import com.twilio.video.*
import com.twilio.video.VideoTrack
import kotlinx.android.synthetic.main.activity_video_call.*
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.StatsReport
import org.webrtc.VideoCapturer
import com.combinedpublic.mobileclient.Classes.*
import com.combinedpublic.mobileclient.WebRTC.AppRTCAudioManager
import com.combinedpublic.mobileclient.WebRTC.AppRTCClient
import com.combinedpublic.mobileclient.WebRTC.CallFragment.OnCallEvents
import com.combinedpublic.mobileclient.WebRTC.PeerConnectionClient
import com.combinedpublic.mobileclient.services.newCandidate
import kotlin.properties.Delegates

class VideoCall : AppCompatActivity(), PeerConnectionClient.PeerConnectionEvents, OnCallEvents {


    var LOG_TAG = "VideoCallLogs"

    private val CAMERA_MIC_PERMISSION_REQUEST_CODE = 1

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
    private var _isTwilioStarted: Boolean = false
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

    // Twilio region
    /*
     * Twilio - A Room represents communication between a local participant and one or more participants.
     */
    private var room: Room? = null
    private var localParticipant: LocalParticipant? = null

    /*
     * Twilio - AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
     * video.
     */
    private val audioCodec: AudioCodec
        get() {
            val audioCodecName = sharedPreferences.getString(Settings.PREF_AUDIO_CODEC,
                    Settings.PREF_AUDIO_CODEC_DEFAULT)

            return when (audioCodecName) {
                IsacCodec.NAME -> IsacCodec()
                OpusCodec.NAME -> OpusCodec()
                PcmaCodec.NAME -> PcmaCodec()
                PcmuCodec.NAME -> PcmuCodec()
                G722Codec.NAME -> G722Codec()
                else -> OpusCodec()
            }
        }
    private val videoCodec: VideoCodec
        get() {
            val videoCodecName = sharedPreferences.getString(Settings.PREF_VIDEO_CODEC,
                    Settings.PREF_VIDEO_CODEC_DEFAULT)

            return when (videoCodecName) {
                Vp8Codec.NAME -> {
                    val simulcast = sharedPreferences.getBoolean(
                            Settings.PREF_VP8_SIMULCAST,
                            Settings.PREF_VP8_SIMULCAST_DEFAULT)
                    Vp8Codec(simulcast)
                }
                H264Codec.NAME -> H264Codec()
                Vp9Codec.NAME -> Vp9Codec()
                else -> Vp8Codec()
            }
        }

    private val enableAutomaticSubscription: Boolean
        get() {
            return sharedPreferences.getBoolean(Settings.PREF_ENABLE_AUTOMATIC_SUBSCRIPTION, Settings.PREF_ENABLE_AUTOMATIC_SUBCRIPTION_DEFAULT)
        }

    /*
     * Twilio - Encoding parameters represent the sender side bandwidth constraints.
     */
    private val encodingParameters: EncodingParameters
        get() {
            val defaultMaxAudioBitrate = Settings.PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT
            val defaultMaxVideoBitrate = Settings.PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT
            val maxAudioBitrate = Integer.parseInt(
                    sharedPreferences.getString(Settings.PREF_SENDER_MAX_AUDIO_BITRATE,
                            defaultMaxAudioBitrate) ?: defaultMaxAudioBitrate
            )
            val maxVideoBitrate = Integer.parseInt(
                    sharedPreferences.getString(Settings.PREF_SENDER_MAX_VIDEO_BITRATE,
                            defaultMaxVideoBitrate) ?: defaultMaxVideoBitrate
            )

            return EncodingParameters(maxAudioBitrate, maxVideoBitrate)
        }

    /*
     * Twilio - Room events listener
     */
    private val roomListener = object : Room.Listener {
        override fun onConnected(room: Room) {
            localParticipant = room.localParticipant
            Log.d(LOG_TAG, "(TWILIO) Connected to room :  ${room.name}")
            title = room.name

            // Only one participant is supported
            room.remoteParticipants?.firstOrNull()?.let { addRemoteParticipant(it) }
        }

        override fun onReconnected(room: Room) {
            Log.d(LOG_TAG, "(TWILIO) Connected to ${room.name}")
            //reconnectingProgressBar.visibility = View.GONE;
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            //videoStatusTextView.text = "Reconnecting to ${room.name}"
            Log.d(LOG_TAG, "(TWILIO) Reconnecting to ${room.name}")
            //reconnectingProgressBar.visibility = View.VISIBLE;
        }

        override fun onConnectFailure(room: Room, e: TwilioException) {
            //videoStatusTextView.text = "Failed to connect"
            Log.d(LOG_TAG, "(TWILIO) Failed to connect")
            audioSwitch!!.deactivate()
        }

        override fun onDisconnected(room: Room, e: TwilioException?) {
            localParticipant = null
            //videoStatusTextView.text = "Disconnected from ${room.name}"
            Log.d(LOG_TAG, "(TWILIO) Disconnected from ${room.name}")
            //reconnectingProgressBar.visibility = View.GONE;
            this@VideoCall.room = null
            // Only reinitialize the UI if disconnect was not called from onDestroy()
            if (!disconnectedFromOnDestroy) {
                audioSwitch!!.deactivate()
                moveLocalVideoToPrimaryView()
            }
        }

        override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
            addRemoteParticipant(participant)
            Log.d(LOG_TAG, "(TWILIO) onParticipantConnected")

        }

        override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
            removeRemoteParticipant(participant)
            Log.d(LOG_TAG, "(TWILIO) onParticipantDisconnected")
        }

        override fun onRecordingStarted(room: Room) {
            /*
             * Indicates when media shared to a Room is being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            Log.d(LOG_TAG, "(TWILIO) onRecordingStarted")
        }

        override fun onRecordingStopped(room: Room) {
            /*
             * Indicates when media shared to a Room is no longer being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            Log.d(LOG_TAG, "(TWILIO) onRecordingStopped")
        }
    }

    /*
     * Twilio - RemoteParticipant events listener
     */
    private val participantListener = object : RemoteParticipant.Listener {
        override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            Log.i(LOG_TAG, "(TWILIO) onAudioTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteAudioTrackPublication.trackName}]")
            //videoStatusTextView.text = "onAudioTrackAdded"
            Log.d(LOG_TAG, "(TWILIO) onAudioTrackAdded")
        }

        override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            Log.i(LOG_TAG, "(TWILIO) onAudioTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteAudioTrackPublication.trackName}]")
            //videoStatusTextView.text = "onAudioTrackRemoved"
            Log.d(LOG_TAG, "(TWILIO) onAudioTrackRemoved")
        }

        override fun onDataTrackPublished(remoteParticipant: RemoteParticipant,
                                          remoteDataTrackPublication: RemoteDataTrackPublication) {
            Log.i(LOG_TAG, "(TWILIO) onDataTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteDataTrackPublication.trackName}]")
           //videoStatusTextView.text = "onDataTrackPublished"
            Log.d(LOG_TAG, "(TWILIO) onDataTrackPublished")
        }

        override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant,
                                            remoteDataTrackPublication: RemoteDataTrackPublication) {
            Log.i(LOG_TAG, "(TWILIO) onDataTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteDataTrackPublication.trackName}]")
            //videoStatusTextView.text = "onDataTrackUnpublished"
            Log.d(LOG_TAG, "(TWILIO) onDataTrackUnpublished")
        }

        override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            Log.i(LOG_TAG, "(TWILIO) onVideoTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteVideoTrackPublication.trackName}]")
            //videoStatusTextView.text = "onVideoTrackPublished"
            Log.d(LOG_TAG, "(TWILIO) onVideoTrackPublished")

        }

        override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            Log.i(LOG_TAG, "(TWILIO) onVideoTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteVideoTrackPublication.trackName}]")
            //videoStatusTextView.text = "onVideoTrackUnpublished"
            Log.d(LOG_TAG, "(TWILIO) onVideoTrackUnpublished")
        }

        override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                            remoteAudioTrack: RemoteAudioTrack) {
            Log.i(LOG_TAG, "(TWILIO) onAudioTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                    "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                    "name=${remoteAudioTrack.name}]")
            //videoStatusTextView.text = "onAudioTrackSubscribed"
            Log.d(LOG_TAG, "(TWILIO) onAudioTrackSubscribed")
        }

        override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                              remoteAudioTrack: RemoteAudioTrack) {
            Log.i(LOG_TAG, "(TWILIO) onAudioTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                    "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                    "name=${remoteAudioTrack.name}]")
            //videoStatusTextView.text = "onAudioTrackUnsubscribed"
            Log.d(LOG_TAG, "(TWILIO) onAudioTrackUnsubscribed")
        }

        override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                    twilioException: TwilioException) {
            Log.i(LOG_TAG, "(TWILIO) onAudioTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "name=${remoteAudioTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")
            //videoStatusTextView.text = "onAudioTrackSubscriptionFailed"
            Log.d(LOG_TAG, "(TWILIO) onAudioTrackSubscriptionFailed")
        }

        override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant,
                                           remoteDataTrackPublication: RemoteDataTrackPublication,
                                           remoteDataTrack: RemoteDataTrack) {
            Log.i(LOG_TAG, "(TWILIO) onDataTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                    "name=${remoteDataTrack.name}]")
            //videoStatusTextView.text = "onDataTrackSubscribed"
            Log.d(LOG_TAG, "(TWILIO) onDataTrackSubscribed")
        }

        override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                             remoteDataTrackPublication: RemoteDataTrackPublication,
                                             remoteDataTrack: RemoteDataTrack) {
            Log.i(LOG_TAG, "(TWILIO) onDataTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                    "name=${remoteDataTrack.name}]")
            //videoStatusTextView.text = "onDataTrackUnsubscribed"
            Log.d(LOG_TAG, "(TWILIO) onDataTrackUnsubscribed")
        }

        override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                   remoteDataTrackPublication: RemoteDataTrackPublication,
                                                   twilioException: TwilioException) {
            Log.i(LOG_TAG, "(TWILIO) onDataTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "name=${remoteDataTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")
            //videoStatusTextView.text = "onDataTrackSubscriptionFailed"
            Log.d(LOG_TAG, "(TWILIO) onDataTrackSubscriptionFailed")
        }

        override fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                            remoteVideoTrack: RemoteVideoTrack) {
            Log.i(LOG_TAG, "(TWILIO) onVideoTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                    "name=${remoteVideoTrack.name}]")
            //videoStatusTextView.text = "onVideoTrackSubscribed"
            Log.d(LOG_TAG, "(TWILIO) onVideoTrackSubscribed")
            addRemoteParticipantVideo(remoteVideoTrack)

        }

        override fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                              remoteVideoTrack: RemoteVideoTrack) {
            Log.i(LOG_TAG, "(TWILIO) onVideoTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                    "name=${remoteVideoTrack.name}]")
            //videoStatusTextView.text = "onVideoTrackUnsubscribed"
            Log.d(LOG_TAG, "(TWILIO) onVideoTrackUnsubscribed")
            removeParticipantVideo(remoteVideoTrack)
        }

        override fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                                    twilioException: TwilioException) {
            Log.i(LOG_TAG, "(TWILIO) onVideoTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "name=${remoteVideoTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")
            //videoStatusTextView.text = "onVideoTrackSubscriptionFailed"
            Log.d(LOG_TAG, "(TWILIO) onVideoTrackSubscriptionFailed")
        }

        override fun onAudioTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            Log.d(LOG_TAG, "(TWILIO) onAudioTrackEnabled")
        }

        override fun onVideoTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            Log.d(LOG_TAG, "(TWILIO) onVideoTrackEnabled")
        }

        override fun onVideoTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            Log.d(LOG_TAG, "(TWILIO) onVideoTrackDisabled")
        }

        override fun onAudioTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            Log.d(LOG_TAG, "(TWILIO) onAudioTrackDisabled")
        }
    }

    private var localAudioTrack: LocalAudioTrack? = null
    private var localVideoTrack: LocalVideoTrack? = null
    private var alertDialog: AlertDialog? = null
    private val cameraCapturerCompat by lazy {
        CameraCapturerCompat(this, CameraCapturerCompat.Source.FRONT_CAMERA)
    }
    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this@VideoCall)
    }

    /*
     * Twilio - Audio management
     */
    var audioSwitch: AudioSwitch? = null

    private var savedVolumeControlStream by Delegates.notNull<Int>()
    private lateinit var audioDeviceMenuItem: ImageButton

    private var participantIdentity: String? = null
    private lateinit var TIlocalVideoView: tvi.webrtc.VideoSink
    private var disconnectedFromOnDestroy = false
    private var isSpeakerPhoneEnabled = true

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            var cameraAndMicPermissionGranted = true

            for (grantResult in grantResults) {
                cameraAndMicPermissionGranted = cameraAndMicPermissionGranted and
                        (grantResult == PackageManager.PERMISSION_GRANTED)
            }

            if (cameraAndMicPermissionGranted) {
                createAudioAndVideoTracks()
            } else {
                Toast.makeText(this,
                        R.string.permissions_needed,
                        Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun requestPermissionForCameraAndMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show()
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                    CAMERA_MIC_PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkPermissionForCameraAndMicrophone(): Boolean {
        val resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED
    }

    private fun createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true)

        // Share your camera
        localVideoTrack = LocalVideoTrack.create(this,
                true,
                cameraCapturerCompat)
    }

    private fun connectToRoom() {

        Log.d(LOG_TAG, "(TWILIO) connectToRoom called")

        val _token = User.getInstance().accessToken
        val _roomName = User.getInstance().roomName

        if (_token == "TWILIO_ACCESS_TOKEN" ) { // || _isTwilioStarted
            Log.d(LOG_TAG,"(TWILIO) TWILIO_ACCESS_TOKEN is bad - $_token - _isTwilioStarted = - $_isTwilioStarted -  | or already twilio started , stop connection to room")
            return
        }

        _isTwilioStarted = true

        audioSwitch!!.activate()

        val connectOptionsBuilder = ConnectOptions.Builder(_token)
                .roomName(_roomName)

        /*
         * Add local audio track to connect options to share with participants.
         */
        localAudioTrack?.let { connectOptionsBuilder.audioTracks(listOf(it)) }

        /*
         * Add local video track to connect options to share with participants.
         */
        localVideoTrack?.let { connectOptionsBuilder.videoTracks(listOf(it)) }

        /*
         * Set the preferred audio and video codec for media.
         */
        connectOptionsBuilder.preferAudioCodecs(listOf(audioCodec))
        connectOptionsBuilder.preferVideoCodecs(listOf(videoCodec))

        /*
         * Set the sender side encoding parameters.
         */
        connectOptionsBuilder.encodingParameters(encodingParameters)

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        connectOptionsBuilder.enableAutomaticSubscription(enableAutomaticSubscription)

        room = Video.connect(this, connectOptionsBuilder.build(), roomListener)
        //setDisconnectAction()
    }

    private fun setupAudioTwilio() {

        Log.d(LOG_TAG, "(TWILIO) setupAudioTwilio is called")

        audioSwitch!!.start { audioDevices, selectedDevice ->
            // TODO update UI with audio devices
        }

        val devices: List<AudioDevice> = audioSwitch!!.availableAudioDevices

        Log.d(LOG_TAG, "(TWILIO) setupAudioTwilio is called - devices count is - ${devices.toString()}")

        devices.find { it is AudioDevice.Speakerphone }?.let { audioSwitch!!.selectDevice(it) }
    }

    /*
     * Called when participant joins the room
     */
    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.visibility == View.VISIBLE) {
            Log.d(LOG_TAG,"(TWILIO) addRemoteParticipant called - Error - Multiple participants are not currently support in this UI")
            return
        }
        participantIdentity = remoteParticipant.identity
        //videoStatusTextView.text = "Participant $participantIdentity joined"

        /*
         * Add participant renderer
         */
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let { addRemoteParticipantVideo(it) }
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(participantListener)
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
        moveLocalVideoToThumbnailView()
        primaryVideoView.mirror = false
        videoTrack.addSink(primaryVideoView)

        runOnUiThread {
            callConnected()

            /*
             * If the local video track was released when the app was put in the background, recreate.
             */
            localVideoTrack = if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
                LocalVideoTrack.create(this,
                        true,
                        cameraCapturerCompat)
            } else {
                localVideoTrack
            }
            localVideoTrack?.addSink(TIlocalVideoView)

            /*
             * If connected to a Room then share the local video track.
             */
            localVideoTrack?.let { localParticipant?.publishTrack(it) }

            /*
             * Update encoding parameters if they have changed.
             */
            localParticipant?.setEncodingParameters(encodingParameters)

        }
    }

    private fun moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.visibility == View.GONE) {
            thumbnailVideoView.visibility = View.VISIBLE
            with(localVideoTrack) {
                this?.removeSink(primaryVideoView)
                this?.addSink(thumbnailVideoView)
            }
            TIlocalVideoView = thumbnailVideoView
            thumbnailVideoView.mirror = cameraCapturerCompat.cameraSource ==
                    CameraCapturerCompat.Source.FRONT_CAMERA
        }
    }

    /*
     * Called when participant leaves the room
     */
    private fun removeRemoteParticipant(remoteParticipant: RemoteParticipant) {
        //videoStatusTextView.text = "Participant $remoteParticipant.identity left."
        if (remoteParticipant.identity != participantIdentity) {
            return
        }

        /*
         * Remove participant renderer
         */
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let { removeParticipantVideo(it) }
            }
        }
        moveLocalVideoToPrimaryView()
    }

    private fun removeParticipantVideo(videoTrack: VideoTrack) {
        videoTrack.removeSink(primaryVideoView)
    }

    private fun moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.visibility == View.VISIBLE) {
            thumbnailVideoView.visibility = View.GONE
            with(localVideoTrack) {
                this?.removeSink(thumbnailVideoView)
                this?.addSink(primaryVideoView)
            }
            TIlocalVideoView = primaryVideoView
            primaryVideoView.mirror = cameraCapturerCompat.cameraSource ==
                    CameraCapturerCompat.Source.FRONT_CAMERA
        }
    }

    // Twilio endregion

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
            else if (action == "gettokenresult") {
                connectToRoom()
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
        val filters = listOf<String>("suspendApp", "unSuspendApp", "confCompleted", "receivedOffer",
                "receivedCandidate", "reject", "gettokenresult")
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

        initUi()
        //Block all ui
        confStarted()

        if (User.getInstance()._isTwilioEnabled) {
            Log.d(LOG_TAG, "isTwilioEnabled - true - Start Twilio")
            startTwilio()
        } else {
            Log.d(LOG_TAG, "isTwilioEnabled - false - Start WebRTC")
            startWebRTC()
        }

    }

    fun startWebRTC() {

        iceConnected = false
        signalingParameters = null

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

    fun startTwilio() {

        CallManager.getInstance().isStarted = true

        initBaseUi()
        initTwilioUi()

        audioSwitch = AudioSwitch(applicationContext)

        /*
        * Set local video view to primary view
        */
        TIlocalVideoView = primaryVideoView

        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        savedVolumeControlStream = volumeControlStream
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        /*
         * Request permissions.
         */
        requestPermissionForCameraAndMicrophone()

        /*
         * Set the initial state of the UI
         */
        setupAudioTwilio()

        //connectToRoom()

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
            if (!User.getInstance()._isTwilioEnabled) {
                startCall()
            }
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

    private fun initTwilioUi() {
        /*
         * Enable/disable the local video track
         */
//        TIlocalVideoTrack?.let {
//            val enable = !it.isEnabled
//            it.enable(enable)
//        }!!
    }

    private fun initBaseUi() {

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
    }

    private fun initUi() {

        loadingPanelVideoCall = findViewById(R.id.loadingPanelVideoCall)
        localVideoView = findViewById(R.id.local_gl_surface_view)
        remoteVideoView = findViewById(R.id.remote_gl_surface_view)

        initBaseUi()

        // Swap feeds on local view click.
        localVideoView.setOnClickListener {
            if (!User.getInstance()._isTwilioEnabled) {
                setSwappedFeeds(!isSwappedFeeds)
            }

        }

        if (!User.getInstance()._isTwilioEnabled) {
            remoteSinks.add(remoteProxyRenderer)
        }

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

        Log.d(LOG_TAG,"onDestroy called")

        try {
            unregisterReceiver(receiver)
        } catch (e:IllegalArgumentException) {
            Log.d(LOG_TAG,"IllegalArgumentException : "+e.message)
        }

        if (User.getInstance()._isTwilioEnabled) {
            onDestroyTwilio()
        }

        super.onDestroy()
    }

    private fun onDestroyTwilio() {

        Log.d(LOG_TAG,"onDestroy called (TWILIO)")


        /*
         * Tear down audio management and restore previous volume stream
         */
        audioSwitch!!.stop()
        volumeControlStream = savedVolumeControlStream

        Log.d(LOG_TAG,"audioSwitch.stop called (TWILIO)")

        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        if (room != null && room!!.state != Room.State.DISCONNECTED) {
            room!!.disconnect()
            disconnectedFromOnDestroy = true
        }
        Log.d(LOG_TAG,"room.disconnect called (TWILIO)")

        /*
         * If this local video track is being shared in a Room, remove from local
         * participant before releasing the video track. Participants will be notified that
         * the track has been removed.
         */
        localVideoTrack?.let { localParticipant?.unpublishTrack(it) }
        Log.d(LOG_TAG,"localParticipant.unpublishTrack called (TWILIO)")

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        if (localAudioTrack != null) {
            localAudioTrack!!.release()
            localAudioTrack = null
            Log.d(LOG_TAG,"localAudioTrack.release called (TWILIO)")
        }

        if (localVideoTrack != null) {
            localVideoTrack!!.release()
            localVideoTrack = null
            Log.d(LOG_TAG,"localVideoTrack.release called (TWILIO)")
        }

        room = null
        Log.d(LOG_TAG,"the room is set to null (TWILIO)")
        audioSwitch = null
        Log.d(LOG_TAG,"the audioSwitch is set to null (TWILIO)")
        localAudioTrack = null
        Log.d(LOG_TAG,"the localAudioTrack is set to null (TWILIO)")
        localVideoTrack = null
        Log.d(LOG_TAG,"the localVideoTrack is set to null (TWILIO)")

        User.getInstance()._isTwilioEnabled = false
    }

    private fun didHangUp() {

        Log.d(LOG_TAG,"(TWILIO)(WebRTC) didHangUp is called.")

        if (CallManager.getInstance()._conversationId != null) {

            sendMsg("hangUpMessage")

            _isTwilioStarted = false

            isAnswerSended = false

            activityRunning = false

            if (!User.getInstance()._isTwilioEnabled) {

                if (remoteProxyRenderer != null) {

                    remoteProxyRenderer.setTarget(VideoSink { null })
                }

                if (localProxyVideoSink != null) {

                    localProxyVideoSink.setTarget(VideoSink { null })
                }

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
            }

            if (User.getInstance()._isTwilioEnabled) {
                /*
                 * Disconnect from room
                 */
                room?.disconnect()
                Log.d(LOG_TAG,"(TWILIO) room.disconnect is called.")
            }

            CallManager.getInstance().isStarted = false
            if (CallManager.getInstance().isMinimized){
             sendMsg("appPause")
            }

            if (User.getInstance()._isTwilioEnabled) {
                onDestroyTwilio()
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
        if (peerConnectionClient != null && !User.getInstance()._isTwilioEnabled) {
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
        if (!User.getInstance()._isTwilioEnabled) {
            loadingPanelVideoCall.visibility = View.GONE
        }

        if (User.getInstance()._isTwilioEnabled) {
            hangUpBtn?.isEnabled = true
            speakerBtn?.isEnabled = true
            fullScreenBtn?.isEnabled = true
            switchCamBtn?.isEnabled = true
            loadingPanelVideoCall.visibility = View.GONE
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun confStarted() {
        if (User.getInstance()._isTwilioEnabled) {
            loadingPanelVideoCall.visibility = View.VISIBLE
        }

        if (User.getInstance()._isTwilioEnabled) {
            hangUpBtn?.isEnabled = false
            speakerBtn?.isEnabled = false
            fullScreenBtn?.isEnabled = false
            switchCamBtn?.isEnabled = false
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun switchCamera() {

        if (User.getInstance()._isTwilioEnabled) {
            val cameraSource = cameraCapturerCompat.cameraSource
            cameraCapturerCompat.switchCamera()
            if (thumbnailVideoView.visibility == View.VISIBLE) {
                thumbnailVideoView.mirror = cameraSource == CameraCapturerCompat.Source.BACK_CAMERA
            } else {
                primaryVideoView.mirror = cameraSource == CameraCapturerCompat.Source.BACK_CAMERA
            }
        }

        if (!User.getInstance()._isTwilioEnabled && peerConnectionClient!!.videoCapturer != null) {
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

        if (!User.getInstance()._isTwilioEnabled && audioManager!= null ) {
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
        }
        else if (User.getInstance()._isTwilioEnabled) {
            // TODO: SWITCH SPEAKER TWILIO

            val devices: List<AudioDevice> = audioSwitch!!.availableAudioDevices

            var _earpiece: AudioDevice? = null
            var _speakerphone: AudioDevice? = null

            Log.d(LOG_TAG, "(TWILIO) audio devices : ")

            for ( device in devices ) {
                Log.d(LOG_TAG, "(TWILIO) audio device is : ${device.name}")
                if (device.name == "Earpiece") {
                    _earpiece = device
                } else if ( device.name == "Speakerphone") {
                    _speakerphone = device
                }
            }

            if (audioSwitch!!.selectedAudioDevice != null) {
                Log.d(LOG_TAG, "(TWILIO) current device is : ${audioSwitch!!.selectedAudioDevice!!.name}")
            }

            if (audioSwitch!!.selectedAudioDevice!!.name == "Speakerphone") {
                audioSwitch!!.selectDevice(_earpiece)
            } else {
                audioSwitch!!.selectDevice(_speakerphone)
            }

            Log.d(LOG_TAG, "(TWILIO) selected device is : ${audioSwitch!!.selectedAudioDevice!!.name}")

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

            if (User.getInstance()._isTwilioEnabled) {
                fullScreenBtn!!.background = null
                fullScreenBtn!!.setBackgroundResource(R.drawable.full_screen_return)
                primaryVideoView.videoScaleType = VideoScaleType.ASPECT_FIT

            } else {
                fullScreenBtn!!.background = null
                fullScreenBtn!!.setBackgroundResource(R.drawable.full_screen_return)
                remoteVideoView.setEnableHardwareScaler(false)
                remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT,
                        RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                remoteVideoView.refreshDrawableState()
            }

        } else {

            if (User.getInstance()._isTwilioEnabled) {
                fullScreenBtn!!.background = null
                fullScreenBtn!!.setBackgroundResource(R.drawable.full_screen)
                primaryVideoView.videoScaleType = VideoScaleType.ASPECT_FILL

            } else {
                fullScreenBtn!!.background = null
                fullScreenBtn!!.setBackgroundResource(R.drawable.full_screen)
                remoteVideoView.setEnableHardwareScaler(true)
                remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                        RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                remoteVideoView.refreshDrawableState()
            }
        }

    }

    // Should be called from ui thread
    private fun callConnected() {
        Log.i(LOG_TAG,"(TWILIO)(WebRTC) Call connected")

        val intent = Intent()
        intent.action = "sendCallconnected"
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
        if (!User.getInstance()._isTwilioEnabled) {
            setSwappedFeeds(false /* isSwappedFeeds */)
        }
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
        if (peerConnectionClient != null && !User.getInstance()._isTwilioEnabled) {
            peerConnectionClient!!.startVideoSource()
        }
    }

    override fun onPause() {
        if (User.getInstance()._isTwilioEnabled) {
            /*
             * If this local video track is being shared in a Room, remove from local
             * participant before releasing the video track. Participants will be notified that
             * the track has been removed.
             */
            localVideoTrack?.let { localParticipant?.unpublishTrack(it) }

            /*
             * Release the local video track before going in the background. This ensures that the
             * camera can be used by other applications while this app is in the background.
             */
            localVideoTrack?.release()
            localVideoTrack = null
            super.onPause()
        } else {
            sendMsg("stopSound")
            sendMsg("appPause")
            CallManager.getInstance().isMinimized = true
            super.onPause()
            Configuration.CombinedPublic.activityVideoCallPaused()
        }
    }

    override fun onResume() {
        if (User.getInstance()._isTwilioEnabled) {
            super.onResume()
            /*
             * If the local video track was released when the app was put in the background, recreate.
             */
            localVideoTrack = if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
                LocalVideoTrack.create(this,
                        true,
                        cameraCapturerCompat)
            } else {
                localVideoTrack
            }
            localVideoTrack?.addSink(TIlocalVideoView)

            /*
             * If connected to a Room then share the local video track.
             */
            localVideoTrack?.let { localParticipant?.publishTrack(it) }

            /*
             * Update encoding parameters if they have changed.
             */
            localParticipant?.setEncodingParameters(encodingParameters)

        } else {
            sendMsg("stopSound")
            sendMsg("appResume")
            CallManager.getInstance().isMinimized = false
            super.onResume()
            Configuration.CombinedPublic.activityVideoCallResumed()
        }
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
        if (!User.getInstance()._isTwilioEnabled) {
            val intent = Intent()
            intent.action = "sendOffer"
            intent.putExtra("type", "offer")
            intent.putExtra("sdp", sdp.description)
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            sendBroadcast(intent)
        }
    }

    private fun sendAnswerSdp(sdp:SessionDescription) {
        if (!User.getInstance()._isTwilioEnabled) {
            val intent = Intent()
            intent.action = "sendOffer"
            intent.putExtra("type", "answer")
            intent.putExtra("sdp", sdp.description)
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            sendBroadcast(intent)
        }
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

}


