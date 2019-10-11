package us.visitel.mobileclient;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

//import us.visitel.mobileclient.R;


/**
 * Handles the initial setup where the user selects which room to join.
 */
public class LoginActivity extends Activity
        implements NclWebSocketChannelClient.WebSocketChannelEvents  {

    private static final String TAG = "LoginActivity";

    private DisabledButton loginButton;
    private TextView textRegister;
    private TextView textForgetPassword;
    private EditText userNameEditText;
    private EditText passwordEditText;
    private CheckBox rememberMe;
    private Toast logToast;

    private TextView textErrorMessage;

    private NclApplication application;
    private NclWebSocketChannelClient client;
    private DictionaryOpenHelper doh = null;


    private boolean showedCloseMessage = false;
    private boolean sentRegisterMessage = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        userNameEditText = (EditText) findViewById(R.id.userName);
        passwordEditText = (EditText) findViewById(R.id.password);
        passwordEditText.setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(
                            TextView textView, int i, KeyEvent keyEvent) {
                        if (i == EditorInfo.IME_ACTION_DONE) {
                            loginButton.performClick();
                            return true;
                        }
                        return false;
                    }
                });
        userNameEditText.requestFocus();

        loginButton = (DisabledButton) findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(loginListener);


        textRegister = (TextView) findViewById(R.id.textRegister);
        textRegister.setOnClickListener(registerListener);

        textForgetPassword = (TextView) findViewById(R.id.textForgetPassword);
        textForgetPassword.setOnClickListener(forgetPasswordListener);

        rememberMe = (CheckBox) findViewById(R.id.rememberMe);

        textErrorMessage = (TextView) findViewById(R.id.textErrorMessage);
        textErrorMessage.setVisibility(View.INVISIBLE);

        application = (NclApplication)getApplication();
        application.addListener(this);
        client = application.getWebSocketClient();
        loginButton.setEnabled(client.getState() == NclWebSocketChannelClient.WebSocketConnectionState.CONNECTED);
        if (client.getState() == NclWebSocketChannelClient.WebSocketConnectionState.ERROR) {
            // show connection error message
            loginButton.setEnabled(false);
            textErrorMessage.setText(R.string.error_not_connected);
            textErrorMessage.setVisibility(View.VISIBLE);
        } else if (client.getState() == NclWebSocketChannelClient.WebSocketConnectionState.CLOSED) {
            // show connection close message
            loginButton.setEnabled(false);
            textErrorMessage.setText(R.string.error_connection_closed);
            textErrorMessage.setVisibility(View.VISIBLE);
        }

        doh = new DictionaryOpenHelper(this);

        String userName = doh.get("username");
        if (userName != null) {
            userNameEditText.setText(userName);
        }

        String password = doh.get("password");
        if (password != null) {
            passwordEditText.setText(password);
        }

        showedCloseMessage = false;
        sentRegisterMessage = false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "LoginActivity destroyed");
        application.removeListener(this);
        application.setHasResetToLogin(false);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.login_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items.
//        if (item.getItemId() == R.id.action_website) {
//            Intent intent = new Intent();
//            intent.setAction(Intent.ACTION_VIEW);
//            intent.setData(Uri.parse("https://secure.visitel.us"));
//            startActivity(intent);
//            return true;
//        } else if (item.getItemId() == R.id.action_settings) {
//            Intent intent = new Intent(this, SettingsActivity.class);
//            startActivity(intent);
//            return true;
//        } else if (item.getItemId() == R.id.action_about) {
//            Intent intent = new Intent(this, AboutActivity.class);
//            startActivity(intent);
//            return true;
//        } else {
            return super.onOptionsItemSelected(item);
//        }
    }

    private final OnClickListener loginListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if(!sentRegisterMessage) {
                String userName = userNameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                if (userName.length() > 0 && password.length() > 0) {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("type", "register_name");
                        json.put("id", "");
                        json.put("userid", userName);
                        json.put("param0", password);
                        Log.d(TAG, "Send: " + json.toString());
                        //client.send(json.toString());
                        application.sendMessage(json.toString());
                        sentRegisterMessage = true;
                    } catch (JSONException e) {
                        //reportError("WebSocket register JSON error: " + e.getMessage());
                    }
                }
            }
        }
    };

    private final OnClickListener registerListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://secure.visitel.us"));
            startActivity(intent);
        }
    };

    private final OnClickListener forgetPasswordListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://secure.visitel.us"));
            startActivity(intent);
        }
    };

    @Override
    public void onWebSocketOpen() {
        // TODO Auto-generated method stub
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("Connection opened");
                sentRegisterMessage = false;
                showedCloseMessage = false;
                loginButton.setEnabled(true);
                textErrorMessage.setVisibility(View.INVISIBLE);
            }
        });
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
                    if (rememberMe.isChecked()) {
                        String userName = userNameEditText.getText().toString();
                        String password = passwordEditText.getText().toString();
                        doh.delete("username");
                        doh.delete("password");
                        doh.insert("username", userName);
                        doh.insert("password", password);
                    }
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
                    sentRegisterMessage = false;
                } else {
                    sentRegisterMessage = false;
                    String statusDesc = json.getString("statusdesc");
                    logAndToast(statusDesc);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!showedCloseMessage) {
                    showedCloseMessage = true;
                    logAndToast("Connection closed");
                }
                loginButton.setEnabled(false);
                sentRegisterMessage = false;
            }
        });
    }

    @Override
    public void onWebSocketError(String description) {
        // TODO Auto-generated method stub
        sentRegisterMessage = false;
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