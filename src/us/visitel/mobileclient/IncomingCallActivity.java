package us.visitel.mobileclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.Camera2Enumerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import us.visitel.mobileclient.R;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class IncomingCallActivity extends Activity
        implements NclWebSocketChannelClient.WebSocketChannelEvents  {
    private static final String TAG = "IncomingCallActivity";

    private ImageButton acceptButton;
    private ImageButton rejectButton;
    private Toast logToast;

    private NclApplication application;
    private NclWebSocketChannelClient client;
    private NclUser user;

    private String contactId;
    private String contactName;
    private String conversationId;

    private TextView messageView;

    private MediaPlayer player;
    private boolean sentAnswerMessage = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_incoming_call);

        rejectButton = (ImageButton) findViewById(R.id.reject_button);
        rejectButton.setOnClickListener(rejectListener);

        acceptButton = (ImageButton) findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(acceptListener);

        int screenWidth;
        int screenHeight;
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

//        rejectButton.setLeft(30); //dm.xdpi;
//        rejectButton.setWidth(dm.widthPixels/2 - 60);
//        acceptButton.setLeft(dm.widthPixels/2 + 30);
//        acceptButton.setWidth(dm.widthPixels/2 - 60);

        sentAnswerMessage = false;

        messageView = (TextView) findViewById(R.id.incoming_call_message);

        final Intent intent = getIntent();
        contactId = intent.getStringExtra(MainActivity.EXTRA_CONTACTID);
        contactName = intent.getStringExtra(MainActivity.EXTRA_CONTACTNAME);
        conversationId = intent.getStringExtra(MainActivity.EXTRA_CONVERSATIONID);

//        messageView.setText(contactName + " is calling you...");
        messageView.setText(contactName);

        Vibrator v = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(2000);
        }
        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.ring);
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to play audio file", e);
        }

        application = (NclApplication)getApplication();
        application.addListener(this);
        client = application.getWebSocketClient();
        user = application.getUser();

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "IncomingCallActivity destroyed");
        application.removeListener(this);
        if (player != null) {
            player.stop();
        }
        super.onDestroy();
    }

    private final OnClickListener acceptListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (sentAnswerMessage == false) {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "ra_answer");
                    json.put("id", user.id);
                    json.put("dst", contactId);
                    json.put("conversationid", conversationId);
                    Log.d(TAG, "C->WSS: " + json.toString());
                    client.send(json.toString());
                    sentAnswerMessage = true;

                    if (canUseCamera2()) {
                        Intent intent = new Intent(IncomingCallActivity.this, VideoCallCamera2Activity.class);
                        intent.putExtra(MainActivity.EXTRA_CONTACTID, contactId);
                        intent.putExtra(MainActivity.EXTRA_CONTACTNAME, contactName);
                        intent.putExtra(MainActivity.EXTRA_CONVERSATIONID, conversationId);
                        intent.putExtra(MainActivity.EXTRA_INITIATOR, false);
                        intent.putExtra(VideoCallCamera2Activity.EXTRA_DISPLAY_HUD, true);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(IncomingCallActivity.this, VideoCallCamera1Activity.class);
                        intent.putExtra(MainActivity.EXTRA_CONTACTID, contactId);
                        intent.putExtra(MainActivity.EXTRA_CONTACTNAME, contactName);
                        intent.putExtra(MainActivity.EXTRA_CONVERSATIONID, conversationId);
                        intent.putExtra(MainActivity.EXTRA_INITIATOR, false);
                        intent.putExtra(VideoCallCamera1Activity.EXTRA_DISPLAY_HUD, true);
                        startActivity(intent);
                    }
                    finish();
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to accept the call", e);
                }
            }
        }
    };

    private final OnClickListener rejectListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            JSONObject json = new JSONObject();
            try {
                json.put("type", "ra_reject");
                json.put("id", user.id);
                json.put("dst", contactId);
                json.put("conversationid", conversationId);
                Log.d(TAG, "C->WSS: " + json.toString());
                client.send(json.toString());

                setResult(RESULT_CANCELED);
                finish();
            } catch (JSONException e) {
                Log.e(TAG, "Failed to reject the call", e);
            }
        }
    };

    @Override
    public void onWebSocketOpen() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onWebSocketMessage(String message) {
        // TODO Auto-generated method stub
        JSONObject json;
        try {
            json = new JSONObject(message);
            String msgType = json.getString("type");
            if(msgType.length() > 0) {
                if (msgType.equals("hangup")) {
                    logAndToast("The call was canceled");
                    setResult(RESULT_CANCELED);
                    finish();
                } else if (msgType.equals("logout")) {
                    sentAnswerMessage = false;
                    String desc = json.getString("statusdesc");
                    logAndToast(desc);
                    finish();
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void onWebSocketClose() {
        // TODO Auto-generated method stub
        sentAnswerMessage = false;
        finish();
    }

    @Override
    public void onWebSocketError(String description) {
        // TODO Auto-generated method stub
        sentAnswerMessage = false;
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

    private boolean canUseCamera2() {
        return Camera2Enumerator.isSupported(this);
    }
}