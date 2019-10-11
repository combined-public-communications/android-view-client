package us.visitel.mobileclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import us.visitel.mobileclient.NclUser.Contact;


/**
 * Handles the initial setup where the user selects which room to join.
 */
public class MainActivity extends Activity implements
        NclWebSocketChannelClient.WebSocketChannelEvents,
        MenuFragment.OnMenuEvents,
        ProfileFragment.OnButtonEvents,
        ContactsFragment.OnContactEvents,
        ButtonEvent {

    public static final String EXTRA_CONTACTID = "us.visitel.mobileclient.CONTACTID";
    public static final String EXTRA_CONTACTNAME = "us.visitel.mobileclient.CONTACTNAME";
    public static final String EXTRA_CONVERSATIONID = "us.visitel.mobileclient.CONVERSATIONID";
    public static final String EXTRA_INITIATOR = "us.visitel.mobileclient.INITIATOR";
    public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
    public static final String EXTRA_BITRATE = "org.appspot.apprtc.BITRATE";
    public static final String EXTRA_HWCODEC = "org.appspot.apprtc.HWCODEC";
    private static final String TAG = "MainActivity";
    private static final int CONNECTION_REQUEST = 1;
    private static boolean commandLineRun = false;

    //private ListView contactListView;
    MenuFragment menuFragment;
    ContactsFragment contactsFragment;
    ProfileFragment profileFragment;
    private MenuFragment.ActiveMenuItem activeMenuItem;
    private SharedPreferences sharedPref;
//	  private String keyprefVideoCallEnabled;
//	  private String keyprefResolution;
//	  private String keyprefFps;
//	  private String keyprefVideoBitrateType;
//	  private String keyprefVideoBitrateValue;
//	  private String keyprefVideoCodec;
//	  private String keyprefAudioBitrateType;
//	  private String keyprefAudioBitrateValue;
//	  private String keyprefAudioCodec;
//	  private String keyprefHwCodecAcceleration;
//	  private String keyprefCpuUsageDetection;
//	  private String keyprefDisplayHud;
//	  private String keyprefRoomServerUrl;
//	  private String keyprefRoom;
//	  private String keyprefRoomList;

    private Toast logToast;

    private NclApplication application;
    private NclWebSocketChannelClient client;
    private NclUser user;

    private DictionaryOpenHelper doh;

    private Timer timer;
    private int pressBackCount;

    private boolean isInitiator;
    //private AsyncHttpURLConnection httpConnection;
    //private NclUser.Contact contact;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate");
        // Get setting keys.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//	    sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//	    keyprefVideoCallEnabled = getString(R.string.pref_videocall_key);
//	    keyprefResolution = getString(R.string.pref_resolution_key);
//	    keyprefFps = getString(R.string.pref_fps_key);
//	    keyprefVideoBitrateType = getString(R.string.pref_startvideobitrate_key);
//	    keyprefVideoBitrateValue = getString(R.string.pref_startvideobitratevalue_key);
//	    keyprefVideoCodec = getString(R.string.pref_videocodec_key);
//	    keyprefHwCodecAcceleration = getString(R.string.pref_hwcodec_key);
//	    keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
//	    keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
//	    keyprefAudioCodec = getString(R.string.pref_audiocodec_key);
//	    keyprefCpuUsageDetection = getString(R.string.pref_cpu_usage_detection_key);
//	    keyprefDisplayHud = getString(R.string.pref_displayhud_key);
//	    keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);
//	    keyprefRoom = getString(R.string.pref_room_key);
//	    keyprefRoomList = getString(R.string.pref_room_list_key);

        setContentView(R.layout.activity_main);

        menuFragment = new MenuFragment();

        // Activate call fragment and start the call.
        getFragmentManager().beginTransaction()
                .add(R.id.menu_fragment_container, menuFragment).commit();

        contactsFragment = new ContactsFragment();

        final Intent intent = getIntent();
        contactsFragment.setArguments(intent.getExtras());
        // Activate call fragment and start the call.
        getFragmentManager().beginTransaction()
                .add(R.id.contacts_fragment_container, contactsFragment).show(contactsFragment).commit();


        profileFragment = new ProfileFragment();

        profileFragment.setArguments(intent.getExtras());
        // Activate call fragment and start the call.
        getFragmentManager().beginTransaction()
                .add(R.id.profile_fragment_container, profileFragment).hide(profileFragment).commit();

        doh = new DictionaryOpenHelper(this);

        application = (NclApplication) getApplication();
        application.addListener(this);
        client = application.getWebSocketClient();
        user = application.getUser();

        activeMenuItem = MenuFragment.ActiveMenuItem.MENU_ITEM_CONTACT;

        pressBackCount = 0;
        isInitiator = false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MainActivity destroyed");
        application.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        logOut();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.contact_menu, menu);
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
//        } else if (item.getItemId() == R.id.action_profile) {
//            Intent intent = new Intent(this, ProfileActivity.class);
//            startActivity(intent);
//            return true;
//        } else if (item.getItemId() == R.id.action_settings) {
//            Intent intent = new Intent(this, SettingsActivity.class);
//            startActivity(intent);
//            return true;
//        } else if (item.getItemId() == R.id.action_logout) {
//            logOut();
//            return true;
//        } else if (item.getItemId() == R.id.action_about) {
//            Intent intent = new Intent(this, AboutActivity.class);
//            startActivity(intent);
//            return true;
//        } else {
            return super.onOptionsItemSelected(item);
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // String room = roomEditText.getText().toString();
//		String roomListJson = new JSONArray(contactList).toString();
//		SharedPreferences.Editor editor = sharedPref.edit();
//		// editor.putString(keyprefRoom, room);
//		editor.putString(keyprefRoomList, roomListJson);
//		editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
//		contactList = new ArrayList<String>();
//
//		String roomListJson = sharedPref.getString(keyprefRoomList, null);
//		if (roomListJson != null) {
//			try {
//				JSONArray jsonArray = new JSONArray(roomListJson);
//				for (int i = 0; i < jsonArray.length(); i++) {
//					contactList.add(jsonArray.get(i).toString());
//				}
//			} catch (JSONException e) {
//				Log.e(TAG, "Failed to load room list: " + e.toString());
//			}
//		}

        menuFragment.setActive(activeMenuItem);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONNECTION_REQUEST && commandLineRun) {
            Log.d(TAG, "Return: " + resultCode);
            setResult(resultCode);
            finish();
        }
    }

    private void logOut() {
        if (pressBackCount == 0) {
            TimerTask task = new TimerTask(){
                public void run() {
                    pressBackCount = 0;
                    timer.cancel();
                }
            };
            timer = new Timer(true);
            timer.schedule(task, 3000, 30000);
            logAndToast("Push again to quit");
            pressBackCount++;
        } else {
            pressBackCount = 0;
            JSONObject json = new JSONObject();
            try {
                json.put("type", "bye");
                json.put("id", user.id);

                client.send(json.toString());
                application.stopGetPeerListTask();
                Log.d(TAG, "Logout");
                finish();
            } catch (JSONException e) {
                Log.e(TAG, "Error to add contact", e);
            }
        }
    }


    @Override
    public void onWebSocketOpen() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onWebSocketMessage(String message) {
        try {
            JSONObject json;
            json = new JSONObject(message);
            String msgType = json.getString("type");
            if (msgType.length() > 0) {
                if (msgType.equals("getpeerlistresult")) {
                    if (!json.isNull("status")) {
                        String status = json.getString("status");
                        if (status.equals("ok")) {
                            final JSONArray contactsJson = new JSONArray(
                                    json.getString("payload"));
                            Log.d(TAG, "contacts: " + contactsJson.length());
                            final String balance = json.isNull("balance") ? "0" : json.getString("balance");
                            final String balanceLabel = json.isNull("param2") ? "Minutes" : json.getString("param2");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    user.balance = balance;
                                    user.balanceLabel = balanceLabel;
                                    user.updateContacts(contactsJson);
                                    //adapter.notifyDataSetChanged();
                                    contactsFragment.updateContacts();
                                }
                            });
                        }
                    }
                } else if (msgType.equals("ra_call")) {

                    Contact contact = user.getContact(json.getString("id"));
                    String conversationId = json.getString("conversationid");
                    if (contact != null) {
                        Intent intent = new Intent(MainActivity.this,
                                IncomingCallActivity.class);
                        intent.putExtra(EXTRA_CONTACTID, contact.getId());
                        intent.putExtra(EXTRA_CONTACTNAME, contact.getName());
                        intent.putExtra(EXTRA_CONVERSATIONID, conversationId);
                        startActivityForResult(intent, CONNECTION_REQUEST);
                    }
                } else if (msgType.equals("logout")) {
                    String desc = json.getString("statusdesc");
                    logAndToast(desc);
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
            // e.printStackTrace();
            Log.e(TAG, "Error to parse message", e);
        }
    }

    @Override
    public void onWebSocketClose() {
        // TODO Auto-generated method stub
        Log.e(TAG, "WebSocket closed");
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

    @Override
    public void onProfile() {
        // TODO Auto-generated method stub
        FragmentManager fManager = getFragmentManager();
        fManager.beginTransaction()
//                .setCustomAnimations(R.animator.fragment_slide_right_enter,
//                        R.animator.fragment_slide_right_exit)
                .hide(contactsFragment)
                .show(profileFragment).commit();
    }

    @Override
    public void onContacts() {
        // TODO Auto-generated method stub
        FragmentManager fManager = getFragmentManager();
//        fManager.beginTransaction()
//                .setCustomAnimations(R.animator.fragment_slide_left_enter,
//                        R.animator.fragment_slide_left_exit)
//                        .hide(profileFragment)
//                .show(contactsFragment).commit();
        fManager.beginTransaction()
                .hide(profileFragment)
                .show(contactsFragment).commit();
    }

    @Override
    public void onSettings() {
        // TODO Auto-generated method stub
        //Intent intent = new Intent(this, SettingsActivity.class);
        //startActivity(intent);
    }

    @Override
    public void onButtonClicked(MenuFragment.ActiveMenuItem item) {
        switch (item) {
            case MENU_ITEM_PROFILE:
                activeMenuItem = MenuFragment.ActiveMenuItem.MENU_ITEM_PROFILE;
                onProfile();
                menuFragment.setActive(activeMenuItem);
                break;
            case MENU_ITEM_CONTACT:
                activeMenuItem = MenuFragment.ActiveMenuItem.MENU_ITEM_CONTACT;
                onContacts();
                menuFragment.setActive(activeMenuItem);
                break;
            default:
                break;
        }
    }

    @Override
    public void onUnregister() {
        // TODO Auto-generated method stub
        AlertDialog.Builder builder = new Builder(this);
        builder.setMessage("Are you sure to logout?");
        builder.setTitle("VisiTel");
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doh.delete("username");
                doh.delete("password");
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "bye");
                    json.put("id", user.id);

                    client.send(json.toString());
                    application.stopGetPeerListTask();
                    Log.d(TAG, "Unregister");
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    setResult(RESULT_OK);
                    finish();
                } catch (JSONException e) {
                    Log.e(TAG, "Error to log out", e);
                }
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });
        builder.create().show();
    }

    @Override
    public void onAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public void onToast(String msg) {
        // TODO Auto-generated method stub
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    @Override
    public void onCallout(String id, String name, boolean isInitiator) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(this,
                OutgoingCallActivity.class);
        intent.putExtra(EXTRA_CONTACTID, id);
        intent.putExtra(EXTRA_CONTACTNAME, name);
        intent.putExtra(EXTRA_INITIATOR, isInitiator);
        startActivityForResult(intent, CONNECTION_REQUEST);
    }

}
