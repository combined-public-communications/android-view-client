package us.visitel.mobileclient;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.StatsReport;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import us.visitel.mobileclient.R;

/**
 * Fragment for call control.
 */
public class CallFragment extends Fragment {
    private static final String TAG = "CallFragment";

    public static final String EXTRA_CONTACTNAME=
            "us.visitel.mobileclient.CONTACTNAME";
    public static final String EXTRA_DISPLAY_HUD = "us.visitel.mobileclient.DISPLAY_HUD";

    private View controlView;
    private TextView encoderStatView;
    private TextView contactNameView;
//    private TextView hudView;
//    private Button restartICEButton;
    private ImageButton disconnectButton;
    private ImageButton cameraSwitchButton;
    private ImageButton videoScalingButton;
    private OnCallEvents callEvents;
    private ScalingType scalingType;
    private boolean displayHud;
    private volatile boolean isRunning;

    private NclApplication application;
    private NclUser user;

    private Date startTime;

    /**
     * Call control interface for container activity.
     */
    public interface OnCallEvents {
        void onRestartICE();
        void onCallHangUp();
        void onCameraSwitch();
        void onVideoScalingSwitch(ScalingType scalingType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        controlView =
                inflater.inflate(R.layout.fragment_call_visitel, container, false);

        // Create UI controls.
        encoderStatView =
                (TextView) controlView.findViewById(R.id.encoder_stat_call);
        contactNameView =
                (TextView) controlView.findViewById(R.id.contact_name_call);
//        hudView =
//                (TextView) controlView.findViewById(R.id.hud_stat_call);
//        restartICEButton =
//                (Button) controlView.findViewById(R.id.restart_ice);
        disconnectButton =
                (ImageButton) controlView.findViewById(R.id.button_call_disconnect);
        cameraSwitchButton =
                (ImageButton) controlView.findViewById(R.id.button_call_switch_camera);
        videoScalingButton =
                (ImageButton) controlView.findViewById(R.id.button_call_scaling_mode);

        // Add buttons click events.
//        restartICEButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                callEvents.onRestartICE();
//            }
//        });
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callEvents.onCallHangUp();
            }
        });

        cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callEvents.onCameraSwitch();
            }
        });

        videoScalingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scalingType == ScalingType.SCALE_ASPECT_FILL) {
                    videoScalingButton.setBackgroundResource(
                            R.drawable.ic_action_full_screen);
                    scalingType = ScalingType.SCALE_ASPECT_FIT;
                } else {
                    videoScalingButton.setBackgroundResource(
                            R.drawable.ic_action_return_from_full_screen);
                    scalingType = ScalingType.SCALE_ASPECT_FILL;
                }
                callEvents.onVideoScalingSwitch(scalingType);
            }
        });
        scalingType = ScalingType.SCALE_ASPECT_FIT;



        return controlView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if (args != null) {
            String contactName = args.getString(EXTRA_CONTACTNAME);
            contactNameView.setText(contactName);
            displayHud = args.getBoolean(EXTRA_DISPLAY_HUD, false);
        }
        int visibility = displayHud ? View.VISIBLE : View.INVISIBLE;
        encoderStatView.setVisibility(visibility);
//        hudView.setVisibility(View.INVISIBLE);
//        hudView.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
        isRunning = true;

    }

    @Override
    public void onStop() {
        isRunning = false;
        super.onStop();
    }

    // TODO: Replace with onAttach(Context) once we only support API level 23+.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callEvents = (OnCallEvents) activity;
        application = (NclApplication)activity.getApplication();
        user = application.getUser();
    }

    public void startTimer() {
        startTime = new Date(System.currentTimeMillis());
    }

    private Map<String, String> getReportMap(StatsReport report) {
        Map<String, String> reportMap = new HashMap<String, String>();
        for (StatsReport.Value value : report.values) {
            reportMap.put(value.name, value.value);
        }
        return reportMap;
    }

    public void updateEncoderStatistics() {
        Log.d(TAG, "isRunning: " + isRunning + " displayHud: " + displayHud);
        if (!isRunning || !displayHud) {
            return;
        }
//    String fps = null;
//    String targetBitrate = null;
//    String actualBitrate = null;
//    StringBuilder bweBuilder = new StringBuilder();
//    for (StatsReport report : reports) {
//      if (report.type.equals("ssrc") && report.id.contains("ssrc")
//          && report.id.contains("send")) {
//        Map<String, String> reportMap = getReportMap(report);
//        String trackId = reportMap.get("googTrackId");
//        if (trackId != null
//            && trackId.contains(PeerConnectionClient.VIDEO_TRACK_ID)) {
//          fps = reportMap.get("googFrameRateSent");
//        }
//      } else if (report.id.equals("bweforvideo")) {
//        Map<String, String> reportMap = getReportMap(report);
//        targetBitrate = reportMap.get("googTargetEncBitrate");
//        actualBitrate = reportMap.get("googActualEncBitrate");
//
//        for (StatsReport.Value value : report.values) {
//          String name = value.name.replace("goog", "")
//              .replace("Available", "").replace("Bandwidth", "")
//              .replace("Bitrate", "").replace("Enc", "");
//          bweBuilder.append(name).append("=").append(value.value)
//              .append(" ");
//        }
//        bweBuilder.append("\n");
//      }
//    }
//
//    StringBuilder stat = new StringBuilder(128);
//    if (fps != null) {
//      stat.append("Fps:  ")
//          .append(fps)
//          .append("\n");
//    }
//    if (targetBitrate != null) {
//      stat.append("Target BR: ")
//          .append(targetBitrate)
//          .append("\n");
//    }
//    if (actualBitrate != null) {
//      stat.append("Actual BR: ")
//          .append(actualBitrate)
//          .append("\n");
//    }
//
//    if (cpuMonitor.sampleCpuUtilization()) {
//      stat.append("CPU%: ")
//          .append(cpuMonitor.getCpuCurrent())
//          .append("/")
//          .append(cpuMonitor.getCpuAvg3())
//          .append("/")
//          .append(cpuMonitor.getCpuAvgAll());
//    }


        String stat = "";
        Date timeNow = new Date(System.currentTimeMillis());
        long t = timeNow.getTime() - startTime.getTime();
        String timeString = String.format(Locale.US, "%d:%02d:%02d", t/3600000, (t%3600000)/60000, (t%60000)/1000);

        stat = "Time: " + timeString + "\n";
        stat += user.balanceLabel + ": " + user.balance;

        //Log.d(TAG, stat);
        encoderStatView.setText(stat);
        //hudView.setText(bweBuilder.toString() + hudView.getText());
    }
}
