package us.visitel.mobileclient;

import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import us.visitel.mobileclient.PeerConnectionClientCamera1.PeerConnectionParameters;
import org.appspot.apprtc.UnhandledExceptionHandler;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import us.visitel.mobileclient.R;


/**
 * Activity of the VisiTel Android app demonstrating interoperability
 * between the Android/Java implementation of PeerConnection and the
 * secure.visitel.us webapp.
 */
public class VideoCallCamera1Activity extends Activity
        implements NclWebSocketChannelClient.WebSocketChannelEvents,
        PeerConnectionClientCamera1.PeerConnectionEvents,
        CallFragment.OnCallEvents {
    private static final String TAG = "VideoCallCamera1";

    // Fix for devices running old Android versions not finding the libraries.
    // https://bugs.chromium.org/p/webrtc/issues/detail?id=6751
//    static {
//        try {
//            System.loadLibrary("c++_shared");
//            System.loadLibrary("boringssl.cr");
//            System.loadLibrary("protobuf_lite.cr");
//        } catch (UnsatisfiedLinkError e) {
//            Logging.w(TAG, "Failed to load native dependencies: ", e);
//        }
//    }

    public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
    public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
    public static final String EXTRA_SCREENCAPTURE = "org.appspot.apprtc.SCREENCAPTURE";
    public static final String EXTRA_CAMERA2 = "us.visitel.mobileclient.CAMERA2";
    public static final String EXTRA_VIDEO_WIDTH = "us.visitel.mobileclient.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "us.visitel.mobileclient.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
            "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "us.visitel.mobileclient.CAPTURETOTEXTURE";
    public static final String EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC";
    public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED =
            "org.appspot.apprtc.NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP";
    public static final String EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES";
    public static final String EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC";
    public static final String EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC";
    public static final String EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS";
    public static final String EXTRA_ENABLE_LEVEL_CONTROL = "org.appspot.apprtc.ENABLE_LEVEL_CONTROL";
    public static final String EXTRA_DISPLAY_HUD = "us.visitel.mobileclient.DISPLAY_HUD";
    public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
    public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
    public static final String EXTRA_VIDEO_FILE_AS_CAMERA = "org.appspot.apprtc.VIDEO_FILE_AS_CAMERA";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT";
    public static final String EXTRA_USE_VALUES_FROM_INTENT =
            "org.appspot.apprtc.USE_VALUES_FROM_INTENT";
    public static final String EXTRA_DATA_CHANNEL_ENABLED = "org.appspot.apprtc.DATA_CHANNEL_ENABLED";
    public static final String EXTRA_ORDERED = "org.appspot.apprtc.ORDERED";
    public static final String EXTRA_MAX_RETRANSMITS_MS = "org.appspot.apprtc.MAX_RETRANSMITS_MS";
    public static final String EXTRA_MAX_RETRANSMITS = "org.appspot.apprtc.MAX_RETRANSMITS";
    public static final String EXTRA_PROTOCOL = "org.appspot.apprtc.PROTOCOL";
    public static final String EXTRA_NEGOTIATED = "org.appspot.apprtc.NEGOTIATED";
    public static final String EXTRA_ID = "org.appspot.apprtc.ID";

    public static final String EXTRA_CONTACTNAME=
            "us.visitel.mobileclient.CONTACTNAME";

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    //Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;

//    private class ProxyRenderer implements VideoRenderer.Callbacks {
//        private VideoRenderer.Callbacks target;
//
//        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
//            if (target == null) {
//                Logging.d(TAG, "Dropping frame in proxy because target is null.");
//                VideoRenderer.renderFrameDone(frame);
//                return;
//            }
//
//            target.renderFrame(frame);
//        }
//
//        synchronized public void setTarget(VideoRenderer.Callbacks target) {
//            this.target = target;
//        }
//    }

    //Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;


    //private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer();
    //private final ProxyRenderer localProxyRenderer = new ProxyRenderer();
    private PeerConnectionClientCamera1 peerConnectionClient = null;
    private SessionDescription offerSdp = null;
    private ArrayList<IceCandidate> candidates = new ArrayList<IceCandidate>();

    private SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager = null;
    //private EglBase rootEglBase;
//    private GLSurfaceView pipRenderer;
//    private GLSurfaceView fullscreenRenderer;
    private final List<VideoRenderer.Callbacks> remoteRenderers =
            new ArrayList<VideoRenderer.Callbacks>();
    //private View menuBar;
    private GLSurfaceView videoView;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private ScalingType scalingType;
    private Toast logToast;
    //private final LayoutParams hudLayout =
    //    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    //private TextView hudView;
    //private TextView encoderStatView;
    //private TextView contactNameView;
    private ImageButton videoScalingButton;

    CallFragment callFragment;

    private String contactId;
    private String contactName;
    private boolean isInitiator;
    private String conversationId;

    private boolean commandLineRun;
    private boolean activityRunning;
    private int runTimeMs;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;
    private boolean streamAdded;
    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private final Timer statsTimer = new Timer();
    // True if local view is in the fullscreen renderer.
    private boolean isSwappedFeeds;

    private NclApplication application;
    private NclWebSocketChannelClient client;
    private NclUser user;

    private Timer timer;

    private SharedPreferences sharedPref;
    private String keyprefVideoCallEnabled;
    private String keyprefScreencapture;
    private String keyprefCamera2;
    private String keyprefResolution;
    private String keyprefFps;
    private String keyprefCaptureQualitySlider;
    private String keyprefVideoBitrateType;
    private String keyprefVideoBitrateValue;
    private String keyprefVideoCodec;
    private String keyprefAudioBitrateType;
    private String keyprefAudioBitrateValue;
    private String keyprefAudioCodec;
    private String keyprefHwCodecAcceleration;
    private String keyprefCaptureToTexture;
    private String keyprefFlexfec;
    private String keyprefNoAudioProcessingPipeline;
    private String keyprefAecDump;
    private String keyprefOpenSLES;
    private String keyprefDisableBuiltInAec;
    private String keyprefDisableBuiltInAgc;
    private String keyprefDisableBuiltInNs;
    private String keyprefEnableLevelControl;
    private String keyprefDisplayHud;
    private String keyprefTracing;
    private String keyprefRoomServerUrl;
    private String keyprefRoom;
    private String keyprefRoomList;
    private String keyprefEnableDataChannel;
    private String keyprefOrdered;
    private String keyprefMaxRetransmitTimeMs;
    private String keyprefMaxRetransmits;
    private String keyprefDataProtocol;
    private String keyprefNegotiated;
    private String keyprefDataId;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get setting keys.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        keyprefVideoCallEnabled = getString(R.string.pref_videocall_key);
        keyprefScreencapture = getString(R.string.pref_screencapture_key);
        keyprefCamera2 = getString(R.string.pref_camera2_key);
        keyprefResolution = getString(R.string.pref_resolution_key);
        keyprefFps = getString(R.string.pref_fps_key);
        keyprefCaptureQualitySlider = getString(R.string.pref_capturequalityslider_key);
        keyprefVideoBitrateType = getString(R.string.pref_maxvideobitrate_key);
        keyprefVideoBitrateValue = getString(R.string.pref_maxvideobitratevalue_key);
        keyprefVideoCodec = getString(R.string.pref_videocodec_key);
        keyprefHwCodecAcceleration = getString(R.string.pref_hwcodec_key);
        keyprefCaptureToTexture = getString(R.string.pref_capturetotexture_key);
        keyprefFlexfec = getString(R.string.pref_flexfec_key);
        keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
        keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
        keyprefAudioCodec = getString(R.string.pref_audiocodec_key);
        keyprefNoAudioProcessingPipeline = getString(R.string.pref_noaudioprocessing_key);
        keyprefAecDump = getString(R.string.pref_aecdump_key);
        keyprefOpenSLES = getString(R.string.pref_opensles_key);
        keyprefDisableBuiltInAec = getString(R.string.pref_disable_built_in_aec_key);
        keyprefDisableBuiltInAgc = getString(R.string.pref_disable_built_in_agc_key);
        keyprefDisableBuiltInNs = getString(R.string.pref_disable_built_in_ns_key);
        keyprefEnableLevelControl = getString(R.string.pref_enable_level_control_key);
        keyprefDisplayHud = getString(R.string.pref_displayhud_key);
        keyprefTracing = getString(R.string.pref_tracing_key);
        keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);
        keyprefRoom = getString(R.string.pref_room_key);
        keyprefRoomList = getString(R.string.pref_room_list_key);
        keyprefEnableDataChannel = getString(R.string.pref_enable_datachannel_key);
        keyprefOrdered = getString(R.string.pref_ordered_key);
        keyprefMaxRetransmitTimeMs = getString(R.string.pref_max_retransmit_time_ms_key);
        keyprefMaxRetransmits = getString(R.string.pref_max_retransmits_key);
        keyprefDataProtocol = getString(R.string.pref_data_protocol_key);
        keyprefNegotiated = getString(R.string.pref_negotiated_key);
        keyprefDataId = getString(R.string.pref_data_id_key);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        setContentView(R.layout.activity_call_camera1);

        Thread.setDefaultUncaughtExceptionHandler(
                new UnhandledExceptionHandler(this));
        iceConnected = false;
        streamAdded = false;
        signalingParameters = null;

        // Create UI controls.
//        pipRenderer = (GLSurfaceView) findViewById(R.id.pip_video_view);
//        fullscreenRenderer = (GLSurfaceView) findViewById(R.id.fullscreen_video_view);

        // Show/hide call control fragment on view click.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        };

        // Swap feeds on pip view click.
//        pipRenderer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setSwappedFeeds(!isSwappedFeeds);
//            }
//        });
//
//        fullscreenRenderer.setOnClickListener(listener);
//        remoteRenderers.add(remoteRender);

        final Intent intent = getIntent();

        // Create video renderers.
        //rootEglBase = EglBase.create();
//        pipRenderer.init(rootEglBase.getEglBaseContext(), null);
//        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//
//        fullscreenRenderer.init(rootEglBase.getEglBaseContext(), null);
//        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
//
//        pipRenderer.setZOrderMediaOverlay(true);
//        pipRenderer.setEnableHardwareScaler(true /* enabled */);
//        fullscreenRenderer.setEnableHardwareScaler(true /* enabled */);

        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        setSwappedFeeds(true /* isSwappedFeeds */);

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        Uri url = intent.getData();
        contactId = intent.getStringExtra(MainActivity.EXTRA_CONTACTID);
        contactName = intent.getStringExtra(MainActivity.EXTRA_CONTACTNAME);
        conversationId = intent.getStringExtra(MainActivity.EXTRA_CONVERSATIONID);
        isInitiator = intent.getBooleanExtra(MainActivity.EXTRA_INITIATOR, true);

        application = (NclApplication)getApplication();
        application.addListener(this);
        client = application.getWebSocketClient();
        user = application.getUser();
        LinkedList<PeerConnection.IceServer> iceServers = application.getIceServers();

        if (user.id.equals("")) {
            logAndToast("You need to log in!");
            setResult(RESULT_CANCELED);
            finish();
        }

        if (iceServers.size() == 0) {
            logAndToast("Can not get ICE servers!");
            setResult(RESULT_CANCELED);
            finish();
        }

        Log.d(TAG, "userId: " + contactId + " userName: " + contactName);

//        MediaConstraints pcConstraints = new MediaConstraints();
//        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
//        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
//
//        MediaConstraints videoConstraints = new MediaConstraints();
//        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "640"));
//        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "480"));
//        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "320"));
//        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", "240"));
//
//        MediaConstraints audioConstraints = new MediaConstraints();

        signalingParameters = new SignalingParameters(
                iceServers, isInitiator,
                "", "", "",
                null, null);

        //rootView = findViewById(android.R.id.content);
        //encoderStatView = (TextView) findViewById(R.id.encoder_stat);
        //menuBar = findViewById(R.id.menubar_fragment);
        //contactNameView = (TextView) findViewById(R.id.contact_name);



        videoView = (GLSurfaceView) findViewById(R.id.glview);
        callFragment = new CallFragment();

        VideoRendererGui.setView(videoView, new Runnable() {
            @Override
            public void run() {
                createPeerConnectionFactory();
            }
        });

        scalingType = ScalingType.SCALE_ASPECT_FIT;
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT,
                scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, true);

        remoteRenderers.add(remoteRender);

//         Show/hide call control fragment on view click.
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        });

//    videoView.setOnClickListener(
//        new View.OnClickListener() {
//          @Override
//          public void onClick(View view) {
//            int visibility = menuBar.getVisibility() == View.VISIBLE
//                    ? View.INVISIBLE : View.VISIBLE;
//            encoderStatView.setVisibility(visibility);
//            menuBar.setVisibility(visibility);
//            contactNameView.setVisibility(visibility);
//            if (visibility == View.VISIBLE) {
//              encoderStatView.bringToFront();
//              menuBar.bringToFront();
//              contactNameView.bringToFront();
//              rootView.invalidate();
//            }
//          }
//        });

//    videoScalingButton = (ImageButton) findViewById(R.id.button_scaling_mode);
//    videoScalingButton.setOnClickListener(
//        new View.OnClickListener() {
//          @Override
//          public void onClick(View view) {
//            if (scalingType == ScalingType.SCALE_ASPECT_FILL) {
//              videoScalingButton.setBackgroundResource(
//                  R.drawable.ic_action_full_screen);
//              scalingType = ScalingType.SCALE_ASPECT_FIT;
//            } else {
//              videoScalingButton.setBackgroundResource(
//                  R.drawable.ic_action_return_from_full_screen);
//              scalingType = ScalingType.SCALE_ASPECT_FILL;
//            }
//            updateVideoView();
//          }
//        });

//    hudView = new TextView(this);
//    hudView.setTextColor(Color.BLACK);
//    hudView.setBackgroundColor(Color.WHITE);
//    hudView.setAlpha(0.4f);
//    hudView.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
//    hudView.setVisibility(View.INVISIBLE);
//    addContentView(hudView, hudLayout);


        boolean loopback = intent.getBooleanExtra(
                EXTRA_LOOPBACK, false);
        commandLineRun = intent.getBooleanExtra(
                EXTRA_CMDLINE, false);
        runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);
        //hwCodec = intent.getBooleanExtra(EXTRA_HWCODEC, true);

//        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        final boolean useValuesFromIntent = false;
        // Video call enabled flag.
        boolean videoCallEnabled = sharedPrefGetBoolean(R.string.pref_videocall_key,
                EXTRA_VIDEO_CALL, R.string.pref_videocall_default, useValuesFromIntent);

        // Use screencapture option.
        boolean useScreencapture = sharedPrefGetBoolean(R.string.pref_screencapture_key,
                EXTRA_SCREENCAPTURE, R.string.pref_screencapture_default, useValuesFromIntent);

        // Use Camera2 option.
        boolean useCamera2 = sharedPrefGetBoolean(R.string.pref_camera2_key, EXTRA_CAMERA2,
                R.string.pref_camera2_default, useValuesFromIntent);

        // Get default codecs.
        String videoCodec = sharedPrefGetString(R.string.pref_videocodec_key,
                EXTRA_VIDEOCODEC, R.string.pref_videocodec_default, useValuesFromIntent);
        String audioCodec = sharedPrefGetString(R.string.pref_audiocodec_key,
                EXTRA_AUDIOCODEC, R.string.pref_audiocodec_default, useValuesFromIntent);

        // Check HW codec flag.
        boolean hwCodec = sharedPrefGetBoolean(R.string.pref_hwcodec_key,
                EXTRA_HWCODEC_ENABLED, R.string.pref_hwcodec_default, useValuesFromIntent);

        // Check Capture to texture.
        boolean captureToTexture = sharedPrefGetBoolean(R.string.pref_capturetotexture_key,
                EXTRA_CAPTURETOTEXTURE_ENABLED, R.string.pref_capturetotexture_default,
                useValuesFromIntent);

        // Check FlexFEC.
        boolean flexfecEnabled = sharedPrefGetBoolean(R.string.pref_flexfec_key,
                EXTRA_FLEXFEC_ENABLED, R.string.pref_flexfec_default, useValuesFromIntent);

        // Check Disable Audio Processing flag.
        boolean noAudioProcessing = sharedPrefGetBoolean(R.string.pref_noaudioprocessing_key,
                EXTRA_NOAUDIOPROCESSING_ENABLED, R.string.pref_noaudioprocessing_default,
                useValuesFromIntent);

        // Check Disable Audio Processing flag.
        boolean aecDump = sharedPrefGetBoolean(R.string.pref_aecdump_key,
                EXTRA_AECDUMP_ENABLED, R.string.pref_aecdump_default, useValuesFromIntent);

        // Check OpenSL ES enabled flag.
        boolean useOpenSLES = sharedPrefGetBoolean(R.string.pref_opensles_key,
                EXTRA_OPENSLES_ENABLED, R.string.pref_opensles_default, useValuesFromIntent);

        // Check Disable built-in AEC flag.
        boolean disableBuiltInAEC = sharedPrefGetBoolean(R.string.pref_disable_built_in_aec_key,
                EXTRA_DISABLE_BUILT_IN_AEC, R.string.pref_disable_built_in_aec_default,
                useValuesFromIntent);

        // Check Disable built-in AGC flag.
        boolean disableBuiltInAGC = sharedPrefGetBoolean(R.string.pref_disable_built_in_agc_key,
                EXTRA_DISABLE_BUILT_IN_AGC, R.string.pref_disable_built_in_agc_default,
                useValuesFromIntent);

        // Check Disable built-in NS flag.
        boolean disableBuiltInNS = sharedPrefGetBoolean(R.string.pref_disable_built_in_ns_key,
                EXTRA_DISABLE_BUILT_IN_NS, R.string.pref_disable_built_in_ns_default,
                useValuesFromIntent);

        // Check Enable level control.
        boolean enableLevelControl = sharedPrefGetBoolean(R.string.pref_enable_level_control_key,
                EXTRA_ENABLE_LEVEL_CONTROL, R.string.pref_enable_level_control_key,
                useValuesFromIntent);

        // Get video resolution from settings.
        int videoWidth = 0;
        int videoHeight = 0;
        if (useValuesFromIntent) {
            videoWidth = getIntent().getIntExtra(EXTRA_VIDEO_WIDTH, 0);
            videoHeight = getIntent().getIntExtra(EXTRA_VIDEO_HEIGHT, 0);
        }
        if (videoWidth == 0 && videoHeight == 0) {
            String resolution =
                    sharedPref.getString(keyprefResolution, getString(R.string.pref_resolution_default));
            String[] dimensions = resolution.split("[ x]+");
            if (dimensions.length == 2) {
                try {
                    videoWidth = Integer.parseInt(dimensions[0]);
                    videoHeight = Integer.parseInt(dimensions[1]);
                } catch (NumberFormatException e) {
                    videoWidth = 0;
                    videoHeight = 0;
                    Log.e(TAG, "Wrong video resolution setting: " + resolution);
                }
            }
        }

        // Get camera fps from settings.
        int cameraFps = 0;
        if (useValuesFromIntent) {
            cameraFps = getIntent().getIntExtra(EXTRA_VIDEO_FPS, 0);
        }
        if (cameraFps == 0) {
            String fps = sharedPref.getString(keyprefFps, getString(R.string.pref_fps_default));
            String[] fpsValues = fps.split("[ x]+");
            if (fpsValues.length == 2) {
                try {
                    cameraFps = Integer.parseInt(fpsValues[0]);
                } catch (NumberFormatException e) {
                    cameraFps = 0;
                    Log.e(TAG, "Wrong camera fps setting: " + fps);
                }
            }
        }

        // Check capture quality slider flag.
        boolean captureQualitySlider = sharedPrefGetBoolean(R.string.pref_capturequalityslider_key,
                EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED,
                R.string.pref_capturequalityslider_default, useValuesFromIntent);

        // Get video and audio start bitrate.
        int videoStartBitrate = 0;
        if (useValuesFromIntent) {
            videoStartBitrate = getIntent().getIntExtra(EXTRA_VIDEO_BITRATE, 0);
        }
        if (videoStartBitrate == 0) {
            String bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default);
            String bitrateType = sharedPref.getString(keyprefVideoBitrateType, bitrateTypeDefault);
            if (!bitrateType.equals(bitrateTypeDefault)) {
                String bitrateValue = sharedPref.getString(
                        keyprefVideoBitrateValue, getString(R.string.pref_maxvideobitratevalue_default));
                videoStartBitrate = Integer.parseInt(bitrateValue);
            }
        }

        int audioStartBitrate = 0;
        if (useValuesFromIntent) {
            audioStartBitrate = getIntent().getIntExtra(EXTRA_AUDIO_BITRATE, 0);
        }
        if (audioStartBitrate == 0) {
            String bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default);
            String bitrateType = sharedPref.getString(keyprefAudioBitrateType, bitrateTypeDefault);
            if (!bitrateType.equals(bitrateTypeDefault)) {
                String bitrateValue = sharedPref.getString(
                        keyprefAudioBitrateValue, getString(R.string.pref_startaudiobitratevalue_default));
                audioStartBitrate = Integer.parseInt(bitrateValue);
            }
        }

        // Check statistics display option.
        boolean displayHud = sharedPrefGetBoolean(R.string.pref_displayhud_key,
                EXTRA_DISPLAY_HUD, R.string.pref_displayhud_default, useValuesFromIntent);

        boolean tracing = sharedPrefGetBoolean(R.string.pref_tracing_key, EXTRA_TRACING,
                R.string.pref_tracing_default, useValuesFromIntent);

        // Get datachannel options
        boolean dataChannelEnabled = sharedPrefGetBoolean(R.string.pref_enable_datachannel_key,
                EXTRA_DATA_CHANNEL_ENABLED, R.string.pref_enable_datachannel_default,
                useValuesFromIntent);
        boolean ordered = sharedPrefGetBoolean(R.string.pref_ordered_key, EXTRA_ORDERED,
                R.string.pref_ordered_default, useValuesFromIntent);
        boolean negotiated = sharedPrefGetBoolean(R.string.pref_negotiated_key,
                EXTRA_NEGOTIATED, R.string.pref_negotiated_default, useValuesFromIntent);
        int maxRetrMs = sharedPrefGetInteger(R.string.pref_max_retransmit_time_ms_key,
                EXTRA_MAX_RETRANSMITS_MS, R.string.pref_max_retransmit_time_ms_default,
                useValuesFromIntent);
        int maxRetr =
                sharedPrefGetInteger(R.string.pref_max_retransmits_key, EXTRA_MAX_RETRANSMITS,
                        R.string.pref_max_retransmits_default, useValuesFromIntent);
        int id = sharedPrefGetInteger(R.string.pref_data_id_key, EXTRA_ID,
                R.string.pref_data_id_default, useValuesFromIntent);
        String protocol = sharedPrefGetString(R.string.pref_data_protocol_key,
                EXTRA_PROTOCOL, R.string.pref_data_protocol_default, useValuesFromIntent);

//        keyprefVideoCallEnabled = getString(R.string.pref_videocall_key);
//        keyprefResolution = getString(R.string.pref_resolution_key);
//        keyprefFps = getString(R.string.pref_fps_key);
//        keyprefVideoBitrateType = getString(R.string.pref_startvideobitrate_key);
//        keyprefVideoBitrateValue = getString(R.string.pref_startvideobitratevalue_key);
//        keyprefVideoCodec = getString(R.string.pref_videocodec_key);
//        keyprefHwCodecAcceleration = getString(R.string.pref_hwcodec_key);
//        keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
//        keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
//        keyprefAudioCodec = getString(R.string.pref_audiocodec_key);
//        keyprefCpuUsageDetection = getString(R.string.pref_cpu_usage_detection_key);
//        keyprefDisplayHud = getString(R.string.pref_displayhud_key);
//
//
//        boolean videoCallEnabled = sharedPref.getBoolean(keyprefVideoCallEnabled,
//                Boolean.valueOf(getString(R.string.pref_videocall_default)));
//
//        // Get default codecs.
//        String videoCodec = sharedPref.getString(keyprefVideoCodec,
//                getString(R.string.pref_videocodec_default));
//        String audioCodec = sharedPref.getString(keyprefAudioCodec,
//                getString(R.string.pref_audiocodec_default));
//
//        // Check HW codec flag.
//        boolean hwCodec = sharedPref.getBoolean(keyprefHwCodecAcceleration,
//                Boolean.valueOf(getString(R.string.pref_hwcodec_default)));
//
//        // Get video resolution from settings.
//        int videoWidth = 0;
//        int videoHeight = 0;
//        String resolution = sharedPref.getString(keyprefResolution,
//                getString(R.string.pref_resolution_default));
//        String[] dimensions = resolution.split("[ x]+");
//        if (dimensions.length == 2) {
//            try {
//                videoWidth = Integer.parseInt(dimensions[0]);
//                videoHeight = Integer.parseInt(dimensions[1]);
//            } catch (NumberFormatException e) {
//                videoWidth = 0;
//                videoHeight = 0;
//                Log.e(TAG, "Wrong video resolution setting: " + resolution);
//            }
//        }
//
//        // Get camera fps from settings.
//        int cameraFps = 0;
//        String fps = sharedPref.getString(keyprefFps,
//                getString(R.string.pref_fps_default));
//        String[] fpsValues = fps.split("[ x]+");
//        if (fpsValues.length == 2) {
//            try {
//                cameraFps = Integer.parseInt(fpsValues[0]);
//            } catch (NumberFormatException e) {
//                Log.e(TAG, "Wrong camera fps setting: " + fps);
//            }
//        }
//
//        // Get video and audio start bitrate.
//        int videoStartBitrate = 0;
//        String bitrateTypeDefault = getString(
//                R.string.pref_startvideobitrate_default);
//        String bitrateType = sharedPref.getString(
//                keyprefVideoBitrateType, bitrateTypeDefault);
//        if (!bitrateType.equals(bitrateTypeDefault)) {
//            String bitrateValue = sharedPref.getString(keyprefVideoBitrateValue,
//                    getString(R.string.pref_startvideobitratevalue_default));
//            videoStartBitrate = Integer.parseInt(bitrateValue);
//        }
//        int audioStartBitrate = 0;
//        bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default);
//        bitrateType = sharedPref.getString(
//                keyprefAudioBitrateType, bitrateTypeDefault);
//        if (!bitrateType.equals(bitrateTypeDefault)) {
//            String bitrateValue = sharedPref.getString(keyprefAudioBitrateValue,
//                    getString(R.string.pref_startaudiobitratevalue_default));
//            audioStartBitrate = Integer.parseInt(bitrateValue);
//        }
//
//        // Test if CpuOveruseDetection should be disabled. By default is on.
//        boolean cpuOveruseDetection = sharedPref.getBoolean(
//                keyprefCpuUsageDetection,
//                Boolean.valueOf(
//                        getString(R.string.pref_cpu_usage_detection_default)));
//
//        // Check statistics display option.
//        boolean displayHud = sharedPref.getBoolean(keyprefDisplayHud,
//                Boolean.valueOf(getString(R.string.pref_displayhud_default)));

//        peerConnectionParameters = new PeerConnectionParameters(
//                videoCallEnabled,
//                loopback,
//                videoWidth,
//                videoHeight,
//                cameraFps,
//                videoStartBitrate,
//                videoCodec,
//                hwCodec,
//                audioStartBitrate,
//                audioCodec,
//                cpuOveruseDetection);

        PeerConnectionClientCamera1.DataChannelParameters dataChannelParameters = null;
        peerConnectionParameters =
                new PeerConnectionParameters(videoCallEnabled, loopback,
                        tracing, videoWidth, videoHeight, cameraFps,
                        videoStartBitrate, videoCodec,
                        hwCodec,
                        flexfecEnabled,
                        audioStartBitrate, audioCodec,
                        noAudioProcessing,
                        aecDump,
                        useOpenSLES,
                        disableBuiltInAEC,
                        disableBuiltInAGC,
                        disableBuiltInNS,
                        enableLevelControl, dataChannelParameters);

        callFragment = new CallFragment();
        // Send intent arguments to fragment.
        callFragment.setArguments(intent.getExtras());
        // Activate call fragment and start the call.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.commit();

        if (contactId != null && !contactId.equals("")) {
            // Start room connection.
//            logAndToast(getString(R.string.connecting_to, contactName));



            timer = null;

            startGetBalanceTask();

            //contactNameView.setText(contactName);

            // Create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
            audioManager = AppRTCAudioManager.create(getApplicationContext());
            // Store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.d(TAG, "Starting the audio manager...");
            audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
                // This method will be called each time the number of available audio
                // devices has changed.
                @Override
                public void onAudioDeviceChanged(
                        AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                    onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
                }
            });
        } else {
            logAndToast("Empty or missing user name!");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @TargetApi(17)
    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    @TargetApi(21)
    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && getIntent().getBooleanExtra(EXTRA_CAMERA2, true);
    }

    private boolean captureToTexture() {
        return false;
        // sharedPrefGetBoolean(R.string.pref_capturetotexture_key,
        //         EXTRA_CAPTURETOTEXTURE_ENABLED, R.string.pref_capturetotexture_default,
        //         false); 
                //getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

//    // Create peer connection factory when EGL context is ready.
    private void createPeerConnectionFactory() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "createPeerConnectionFactory, peerConnectionClient: " + peerConnectionClient);
                if (peerConnectionClient == null) {
                    peerConnectionClient = PeerConnectionClientCamera1.getInstance();
                    peerConnectionClient.createPeerConnectionFactory(
                            getApplicationContext(), peerConnectionParameters, VideoCallCamera1Activity.this);
                }
                if (signalingParameters != null) {
                    onConnectedToRoomInternal(signalingParameters);
                }
            }
        });
    }


    // Activity interfaces
    @Override
    public void onStop() {
        super.onStop();
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        Log.d(TAG, "VideoCallCamera1Activity destroyed");
        disconnect();
        application.removeListener(this);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onDestroy();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        //rootEglBase.release();
    }

    public void startGetBalanceTask() {
        if (timer != null) {
            Log.w(TAG, "Get balance list task has started");
            return;
        }

        TimerTask task = new TimerTask() {
            public void run() {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "keepalive");
                    json.put("id", user.id);
                    json.put("conversationid", conversationId);

                    client.send(json.toString());
                } catch (JSONException e) {
                    // reportError("WebSocket register JSON error: " +
                    // e.getMessage());
                }
            }
        };
        timer = new Timer(true);
        timer.schedule(task, 1000, 30000);
    }

    private void stopGetBalanceTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!iceConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
        } else {
            ft.hide(callFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void updateVideoView() {
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType);
        if (streamAdded) {
            VideoRendererGui.update(localRender,
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                    ScalingType.SCALE_ASPECT_FIT);
//                    scalingType);
        } else {
            VideoRendererGui.update(localRender,
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                    LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                    scalingType);
        }
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        Log.d(TAG, "disconnect");

        activityRunning = false;
//        remoteProxyRenderer.setTarget(null);
//        localProxyRenderer.setTarget(null);

        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
//        if (pipRenderer != null) {
//            //pipRenderer.release();
//            pipRenderer = null;
//        }
//
//        if (fullscreenRenderer != null) {
//            //fullscreenRenderer.release();
//            fullscreenRenderer = null;
//        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        statsTimer.cancel();

        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            disconnect();
                        }
                    }).create().show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    // Return the active connection stats,
    // or null if active connection is not found.
    private String getActiveConnectionStats(StatsReport report) {
        StringBuilder activeConnectionbuilder = new StringBuilder();
        // googCandidatePair to show information about the active
        // connection.
        for (StatsReport.Value value : report.values) {
            if (value.name.equals("googActiveConnection")
                    && value.value.equals("false")) {
                return null;
            }
            String name = value.name.replace("goog", "");
            activeConnectionbuilder.append(name).append("=")
                    .append(value.value).append("\n");
        }
        return activeConnectionbuilder.toString();
    }

    public void enableStatsEvents(boolean enable, int periodMs) {
        if (enable) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callFragment.startTimer();
                }
            });
            statsTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callFragment.updateEncoderStatistics();
                        }
                    });
                }
            }, 0, periodMs);
        } else {
            statsTimer.cancel();
        }
    }

    //Should be called from UI thread
    private void callConnected() {
        // Update video view.
        updateVideoView();
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        enableStatsEvents(true, STAT_CALLBACK_PERIOD);
        setSwappedFeeds(false /* isSwappedFeeds */);
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer = null;
        // if (useCamera2()) {
        //     if (!captureToTexture()) {
        //         reportError(getString(R.string.camera2_texture_only_error));
        //         return null;
        //     }

        //     Logging.d(TAG, "Creating capturer using camera2 API.");
        //     videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        // } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        // }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
//        localProxyRenderer.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
//        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
//        fullscreenRenderer.setMirror(isSwappedFeeds);
//        pipRenderer.setMirror(!isSwappedFeeds);
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final SignalingParameters params) {
        signalingParameters = params;
        if (peerConnectionClient == null) {
            Log.w(TAG, "Room is connected, but EGL context is not ready yet.");
            return;
        }
        //logAndToast("Creating peer connection...");
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        peerConnectionClient.createPeerConnection(VideoRendererGui.getEGLContext(), localRender,
                remoteRenderers, videoCapturer, signalingParameters);
//    if (pc.isHDVideo()) {
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//    } else {
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//    }

        // Schedule statistics display.
//    final Runnable repeatedStatsLogger = new Runnable() {
//      public void run() {
//        final Runnable runnableThis = this;
//        if (callFragment.getVisibility() == View.INVISIBLE) {
//          videoView.postDelayed(runnableThis, 1000);
//          return;
//        }
//
//		  runOnUiThread(new Runnable() {
//		      public void run() {
//		       if (encoderStatView.getVisibility() == View.VISIBLE) {
//		          updateEncoderStatistics();
//		        }
//		      }
//		    });
//		  videoView.postDelayed(runnableThis, 1000);
//
//
//      }
//    };
//    videoView.postDelayed(repeatedStatsLogger, 1000);

        if (signalingParameters.initiator) {
//            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (offerSdp != null) {
                peerConnectionClient.setRemoteDescription(offerSdp);
                peerConnectionClient.createAnswer();
                offerSdp = null;
            }
//            for (IceCandidate candidate : candidates) {
//                peerConnectionClient.addRemoteIceCandidate(candidate);
//            }
//            candidates.clear();
        }
    }



    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        Log.d(TAG, "Local description type: " + sdp.type.name());
        Log.d(TAG, "Local description: " + sdp.description);

        JSONObject json = new JSONObject();
        try {
            json.put("type", sdp.type.name().toLowerCase(Locale.US));
            json.put("id", user.id);
            json.put("dst", contactId);
            json.put("conversationid", conversationId);
            JSONObject jsonSdp = new JSONObject();
            jsonSdp.put("type", sdp.type.name().toLowerCase(Locale.US));
            jsonSdp.put("sdp", sdp.description);
            json.put("payload", jsonSdp);
            client.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error to send local description", e);
        }
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {

        JSONObject json = new JSONObject();
        try {
            json.put("type", "candidate");
            json.put("id", user.id);
            json.put("dst", contactId);
            json.put("conversationid", conversationId);
            JSONObject jsonCandi = new JSONObject();
            jsonCandi.put("sdpMLineIndex", candidate.sdpMLineIndex);
            jsonCandi.put("candidate", candidate.sdp);
            jsonCandi.put("sdpMid", candidate.sdpMid);
            json.put("payload", jsonCandi);
            client.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error to send local description", e);
        }
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Candidates removed");
            }
        });
    }

    @Override
    public void onIceConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //logAndToast("ICE connected");
                iceConnected = true;
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iceConnected = false;
            }
        });
    }


    @Override
    public void onPeerConnectionClosed() {
//	  JSONObject json = new JSONObject();
//	  try {
//	  	  json.put("type", "hangup");
//	  	  json.put("id", user.id);
//	  	  json.put("dst", contactId);
//	  	  json.put("conversationid", conversationId);
//	  	  client.send(json.toString());
//	  } catch (JSONException e) {
//	  	  Log.e(TAG, "Error to send hangup message", e);
//	  }
    }

    @Override
    public void onPeerConnectionError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    @Override
    public void onWebSocketOpen() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onWebSocketMessage(String message) {
        // TODO Auto-generated method stub
        try {
            JSONObject json;
            json = new JSONObject(message);
            String msgType = json.getString("type");
            if (msgType.length() > 0) {
                if (msgType.equals("offer")) {
                    String payload = json.getString("payload");
                    Log.d(TAG, payload);
                    JSONObject offer = new JSONObject(payload);
                    final SessionDescription sdp =
                            new SessionDescription(Type.OFFER,
                                    offer.getString("sdp"));
                    if (peerConnectionClient != null) {
                        peerConnectionClient.setRemoteDescription(sdp);
                        if (!signalingParameters.initiator) {
                        // Create answer. Answer SDP will be sent to offering client in
                        // PeerConnectionEvents.onLocalDescription event.
                            peerConnectionClient.createAnswer();
                        }
                    } else {
                        Log.d(TAG, "offer pc is null");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                offerSdp = sdp;
                            }
                        });
                    }
                } else if (msgType.equals("answer")) {
                    String payload = json.getString("payload");
                    Log.d(TAG, payload);
                    JSONObject answer = new JSONObject(payload);
                    final SessionDescription sdp =
                            new SessionDescription(Type.ANSWER,
                                    answer.getString("sdp"));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (peerConnectionClient != null) {
                                peerConnectionClient.setRemoteDescription(sdp);
                            }
                        }
                    });
                } else if (msgType.equals("candidate")) {
                    String payload = json.getString("payload");
                    Log.d(TAG, payload);
                    JSONObject candi = new JSONObject(payload);
                    final IceCandidate candidate = new IceCandidate(candi.getString("sdpMid"),
                            candi.getInt("sdpMLineIndex"), candi.getString("candidate"));
                    Log.d(TAG, "Got candidate " + candidate);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "add candidate");
                            if (peerConnectionClient == null) {
                                Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                                return;
                            }
                            peerConnectionClient.addRemoteIceCandidate(candidate);
                        }
                    });
                } else if (msgType.equals("keepaliveresult")) {
                    //String status = json.getString("status");
                    //if (status.equals("ok")) {
                        String balance = json.getString("balance");
                        String balanceLabel = json.getString("param2");
                        user.balance = balance;
                        user.balanceLabel = balanceLabel;
                    //}
                } else if (msgType.equals("hangup")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logAndToast("Inmate hanged up.");
                            iceConnected = false;
                            streamAdded = false;
                            disconnect();
                        }
                    });
                } else if (msgType.equals("ra_reject")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logAndToast("Inmate hanged up.");
                            iceConnected = false;
                            streamAdded = false;
                            disconnect();
                        }
                    });
                } else if (msgType.equals("logout")) {
                    final Activity thisAct = this;
                    final String desc = json.getString("statusdesc");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopGetBalanceTask();
                            logAndToast(desc);
                            if (application.canResetToLogin()) {
                                application.resetToLogin();
                                Intent intent = new Intent(thisAct, LoginActivity.class);
                                startActivity(intent);
                            }
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            Log.e(TAG, "Error to parse message", e);
        }
    }

    @Override
    public void onWebSocketClose() {
        // TODO Auto-generated method stub
//        if (application.canResetToLogin()) {
//            application.resetToLogin();
//            Intent intent = new Intent(this, LoginActivity.class);
//            startActivity(intent);
//        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onWebSocketError(String description) {
        // TODO Auto-generated method stub
//        if (application.canResetToLogin()) {
//            application.resetToLogin();
//            Intent intent = new Intent(this, LoginActivity.class);
//            startActivity(intent);
//        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onRestartICE() {
        if (peerConnectionClient != null) {
            peerConnectionClient.restartICE();
        }
    }

    @Override
    public void onCallHangUp() {
//        logAndToast("Disconnecting call.");
        JSONObject json = new JSONObject();
        try {
            json.put("type", "hangup");
            json.put("id", user.id);
            json.put("dst", contactId);
            json.put("conversationid", conversationId);
            client.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error to send hangup message", e);
        }
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
//        fullscreenRenderer.setScalingType(scalingType);
        this.scalingType = scalingType;
        updateVideoView();
    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStreamAdded() {
        // TODO Auto-generated method stub
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                logAndToast("Stream added");
                streamAdded = true;
                callConnected();
                updateVideoView();
            }
        });

        try {
            JSONObject json = new JSONObject();
            json.put("type", "callconnected");
            json.put("id", user.id);
            json.put("dst", contactId);
            json.put("conversationid", conversationId);
            Log.d(TAG, "ICE connected: " + json.toString());
            client.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error to send callconnected message", e);
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private String sharedPrefGetString(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        String defaultValue = getString(defaultId);
        if (useFromIntent) {
            String value = getIntent().getStringExtra(intentName);
            if (value != null) {
                return value;
            }
            return defaultValue;
        } else {
            String attributeName = getString(attributeId);
            return sharedPref.getString(attributeName, defaultValue);
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private boolean sharedPrefGetBoolean(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        boolean defaultValue = Boolean.valueOf(getString(defaultId));
        if (useFromIntent) {
            return getIntent().getBooleanExtra(intentName, defaultValue);
        } else {
            String attributeName = getString(attributeId);
            return sharedPref.getBoolean(attributeName, defaultValue);
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private int sharedPrefGetInteger(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        String defaultString = getString(defaultId);
        int defaultValue = Integer.parseInt(defaultString);
        if (useFromIntent) {
            return getIntent().getIntExtra(intentName, defaultValue);
        } else {
            String attributeName = getString(attributeId);
            String value = sharedPref.getString(attributeName, defaultString);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Wrong setting for: " + attributeName + ":" + value);
                return defaultValue;
            }
        }
    }
}
