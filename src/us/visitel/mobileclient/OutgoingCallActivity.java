package us.visitel.mobileclient;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import org.webrtc.Camera2Enumerator;


/**
 * Handles the initial setup where the user selects which room to join.
 */
public class OutgoingCallActivity extends Activity
        implements NclWebSocketChannelClient.WebSocketChannelEvents  {
    private static final String TAG = "OutgoingCallActivity";
    private static final int CONNECTION_REQUEST = 1;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_outgoing_call);

        rejectButton = (ImageButton) findViewById(R.id.reject_button);
        rejectButton.setOnClickListener(rejectListener);

        messageView = (TextView) findViewById(R.id.outgoing_call_message);

        final Intent intent = getIntent();
        contactId = intent.getStringExtra(MainActivity.EXTRA_CONTACTID);
        contactName = intent.getStringExtra(MainActivity.EXTRA_CONTACTNAME);

        final String messageText = "Calling " + contactName + "...";
        messageView.setText(messageText);

        application = (NclApplication)getApplication();
        application.addListener(this);
        client = application.getWebSocketClient();
        user = application.getUser();

        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.dial);
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to play audio file", e);
        }

        JSONObject json = new JSONObject();
        try {
            json.put("type", "ra_call");
            json.put("id", user.id);
            json.put("dst", contactId);

            client.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error to call contact", e);
        }

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "OutgoingCallActivity destroyed");
        application.removeListener(this);
        if (player != null) {
            player.stop();
        }
        super.onDestroy();
    }



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
            if(msgType.length() > 0 && msgType.equals("hangup")) {
                logAndToast("The call was canceled");
                setResult(RESULT_CANCELED);
                finish();
            } else if (msgType.equals("ra_callresult")) {
                final String status = json.getString("status");
                Log.d(TAG, "ra_callresult: " + status);
                if (status.equals("ok")) {
                    conversationId = json.getString("conversationid");
                } else {
                    final String statusDesc = json.getString("statusdesc");
                    logAndToast(statusDesc);
                    setResult(RESULT_CANCELED);
                    finish();
                }
            } else if (msgType.equals("ra_answer")) {
                JSONObject jsonAnswer = new JSONObject();
                try {
                    jsonAnswer.put("type", "call");
                    jsonAnswer.put("id", user.id);
                    jsonAnswer.put("dst", contactId);
                    jsonAnswer.put("conversationid", conversationId);
                    client.send(jsonAnswer.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Error to add contact", e);
                }
            } else if (msgType.equals("ra_reject")) {
                logAndToast("The call was rejected");
                setResult(RESULT_CANCELED);
                finish();
            } else if (msgType.equals("callresult")) {
                final String status = json.getString("status");
                Log.d(TAG, "call result: " + status);
                if (status.equals("ok")) {
                    Log.d(TAG, "support camera2: " + canUseCamera2());
                    if (canUseCamera2()) {
                        Intent intent = new Intent(this,
                                VideoCallCamera2Activity.class);
                        intent.putExtra(MainActivity.EXTRA_CONTACTID, contactId);
                        intent.putExtra(MainActivity.EXTRA_CONTACTNAME, contactName);
                        intent.putExtra(MainActivity.EXTRA_CONVERSATIONID, conversationId);
                        intent.putExtra(MainActivity.EXTRA_INITIATOR, true);
                        intent.putExtra(VideoCallCamera2Activity.EXTRA_DISPLAY_HUD, true);
                        startActivityForResult(intent, CONNECTION_REQUEST);
                    } else {
                        Intent intent = new Intent(this,
                                VideoCallCamera1Activity.class);
                        intent.putExtra(MainActivity.EXTRA_CONTACTID, contactId);
                        intent.putExtra(MainActivity.EXTRA_CONTACTNAME, contactName);
                        intent.putExtra(MainActivity.EXTRA_CONVERSATIONID, conversationId);
                        intent.putExtra(MainActivity.EXTRA_INITIATOR, true);
                        intent.putExtra(VideoCallCamera1Activity.EXTRA_DISPLAY_HUD, true);
                        startActivityForResult(intent, CONNECTION_REQUEST);
                    }
                    finish();
                } else {
                    logAndToast("The call was not established");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }  else if (msgType.equals("logout")) {
                String desc = json.getString("statusdesc");
                logAndToast(desc);
                setResult(RESULT_CANCELED);
                finish();
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void onWebSocketClose() {
        // TODO Auto-generated method stub
        finish();
    }

    @Override
    public void onWebSocketError(String description) {
        // TODO Auto-generated method stub

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