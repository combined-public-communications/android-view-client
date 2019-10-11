package us.visitel.mobileclient;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class SplashScreenActivity extends Activity
        implements NclWebSocketChannelClient.WebSocketChannelEvents  {
    private static final String TAG = "SplashScreenActivity";

    private Toast logToast;
    private NclApplication application;
    private NclWebSocketChannelClient client;
    private DictionaryOpenHelper doh = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        application = (NclApplication)getApplication();
        client = application.getWebSocketClient();
        if (client.getState() == NclWebSocketChannelClient.WebSocketConnectionState.CLOSED ||
                client.getState() == NclWebSocketChannelClient.WebSocketConnectionState.ERROR) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            setResult(RESULT_OK);
            finish();
        } else {
            application.addListener(this);
        }


        if (client.getState() == NclWebSocketChannelClient.WebSocketConnectionState.CONNECTED) {
            sendRegisterMessage();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "SplashScreenActivity destroyed");
        application.removeListener(this);
        super.onDestroy();
    }

    public class FetchDataTask extends AsyncTask<String,Void,Bitmap>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bitmap=null;
            try {
                HttpURLConnection connection= (HttpURLConnection) (new URL(strings[0])).openConnection();
                InputStream is=connection.getInputStream();
                bitmap= BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        //Called when task is finished
        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);

            //Send the data to ContactsActivity by Intent
            Intent intent=new Intent(SplashScreenActivity.this,MainActivity.class);
            intent.putExtra("Image",result);
            startActivity(intent);

            //Destroy self after starting ContactsActivity
            finish();
        }
    }

    @Override
    public void onWebSocketOpen() {
        // TODO Auto-generated method stub
        sendRegisterMessage();
    }

    @Override
    public void onWebSocketMessage(String message) {
        // TODO Auto-generated method stub
        JSONObject json;
        try {
            json = new JSONObject(message);
            String msgType = json.getString("type");
            if(msgType.length() > 0 && msgType.equals("registerresult")) {
                String status = json.getString("status");
                if(status.equals("ok")) {
                    String id = json.getString("id");
                    String userId = json.getString("userid");
                    String name = json.getString("displayname");
                    String balance = json.getString("balance");
                    String balanceLabel = json.getString("param2");
                    application.setUserInfo(id, userId, name, balance, balanceLabel);
                    application.setHasResetToLogin(false);
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    application.startGetPeerListTask();
                    application.sendDeviceInfo();
                } else {
                    if (application.canResetToLogin()) {
                        application.resetToLogin();
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                    }
                    setResult(RESULT_OK);
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
        if (application.canResetToLogin()) {
            application.resetToLogin();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onWebSocketError(String description) {
        if (application.canResetToLogin()) {
            application.resetToLogin();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        setResult(RESULT_OK);
        finish();
    }

    private void sendRegisterMessage() {
        doh = new DictionaryOpenHelper(this);

        String userName = doh.get("username");
        String password = doh.get("password");

        if (userName != null && userName.length() > 0 && password != null && password.length() > 0) {
            JSONObject json = new JSONObject();
            try {
                json.put("type", "register_name");
                json.put("id", "");
                json.put("userid", userName);
                json.put("param0", password);
                Log.d(TAG, "Send: " + json.toString());
//                client.send(json.toString());
                application.sendMessage(json.toString());
            } catch (JSONException e) {
                //reportError("WebSocket register JSON error: " + e.getMessage());
            }
        } else {
            if (application.canResetToLogin()) {
                application.resetToLogin();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }
            finish();
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
}