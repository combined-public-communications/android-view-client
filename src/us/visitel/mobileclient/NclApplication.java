package us.visitel.mobileclient;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import android.app.Application;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.content.Intent;


public class NclApplication extends Application implements NclWebSocketChannelClient.WebSocketChannelEvents, Application.ActivityLifecycleCallbacks {

    private static final String TAG = "NclApplication";
//    private final URI uri = URI.create("ws://192.168.1.2:8181");
    private final URI uri = URI.create("wss://secure.visitel.us:8185");


    private Handler handler;
    private NclWebSocketChannelClient client;

    private Timer timerGetPeerList;
    private Timer timerWSConnection;

    private List<NclWebSocketChannelClient.WebSocketChannelEvents> listeners;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
    private final Object listenersLock = new Object();

    private NclUser user;

    private boolean inBackground;
    private boolean hasResetToLogin;
//    private boolean hasGotIceServers;

    private String lastActivityInBackground;

//    private Activity currentActivity;
    private int activityCount;


    public NclWebSocketChannelClient getWebSocketClient() {
        return client;
    }

    public NclUser getUser() {
        return user;
    }

    public LinkedList<PeerConnection.IceServer> getIceServers() {
        return iceServers;
    }

    public boolean canResetToLogin() {
        return !hasResetToLogin && !inBackground && activityCount > 0;
    }
    public void resetToLogin() {
        hasResetToLogin = true;
        stopGetPeerListTask();
    }
    public void setHasResetToLogin(boolean r) {
        hasResetToLogin = r;
    }
    public void sendMessage(String message) {
        client.send(message);
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();

        inBackground = false;
        hasResetToLogin = false;
//        hasGotIceServers = false;

        activityCount = 0;

        registerActivityLifecycleCallbacks(this);

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        listeners = new LinkedList<NclWebSocketChannelClient.WebSocketChannelEvents>();
        user = new NclUser();


//        TimerTask task = new TimerTask(){
//            public void run() {
//                JSONObject json = new JSONObject();
//                try {
//                    json.put("type", "getpeerlist");
//                    json.put("id", user.id);
//                    json.put("userid", user.userId);
//
//                    client.send(json.toString());
//                } catch (JSONException e) {
//                    //reportError("WebSocket register JSON error: " + e.getMessage());
//                }
//            }
//        };
//        timerGetPeerList = new Timer(true);
//        timerGetPeerList.schedule(task, 1000, 30000);

//        URI uri = URI.create("wss://192.168.0.105:8080");
//		URI uri = URI.create("ws://192.168.0.106:8181");
        //URI uri = URI.create("wss://dev.admin.visitel.us:8080");
//        URI uri = URI.create("wss://admin.visitel.us:8185");
        client = new NclWebSocketChannelClient(handler, this);
        handler.post(new Runnable() {
            @Override
            public void run() {
                client.connect(uri.toString());
            }
        });
        Log.d(TAG, "Application create!");
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "Application terminate!");
        hasResetToLogin = false;
        super.onTerminate();
    }

    @Override
    public void onTrimMemory (int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "Application trim memory " + level);
    }


    @Override
    public void onWebSocketOpen() {
        Log.d(TAG, "onWebSocketOpen");
        synchronized(listenersLock) {
            for(NclWebSocketChannelClient.WebSocketChannelEvents l : listeners) {
                l.onWebSocketOpen();
            }
        }
        fetchIceServers();
    }


    @Override
    public void onWebSocketMessage(String message) {
        Log.d(TAG, "Got message: " + message);
//        startWSConnectionTimer();
        synchronized(listenersLock) {
            for(NclWebSocketChannelClient.WebSocketChannelEvents l : listeners) {
                l.onWebSocketMessage(message);
            }
        }
        try {
            JSONObject json;
            json = new JSONObject(message);
            String msgType = json.getString("type");
            if (msgType.length() > 0) {
                if (msgType.equals("getstunresult")) {
                    onStunResult(json);
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
        stopGetPeerListTask();
        synchronized(listenersLock) {
            for(NclWebSocketChannelClient.WebSocketChannelEvents l : listeners) {
                l.onWebSocketClose();
            }
        }
//        if (currentActivity != null) {
//            ((NclWebSocketChannelClient.WebSocketChannelEvents) currentActivity).onWebSocketClose();
//        }
    }


    @Override
    public void onWebSocketError(String description) {
        stopGetPeerListTask();
        synchronized(listenersLock) {
            for(NclWebSocketChannelClient.WebSocketChannelEvents l : listeners) {
                l.onWebSocketError(description);
            }
        }
//        if (currentActivity != null) {
//            ((NclWebSocketChannelClient.WebSocketChannelEvents) currentActivity).onWebSocketError(description);
//        }
    }

    @Override
    public void onActivityResumed(Activity activity){
        Log.d(TAG, "onActivityResumed " + activity.getLocalClassName());
//        currentActivity = activity;
        if (inBackground) {
            inBackground = false;
            // If current connection is disconnected, then re-login.
            // If current activity is the login activity, then continue
            // If current connection is connected, then continue
            Log.d(TAG, "ConnectionState: " + client.getState());
            Log.d(TAG, "lastActivityInBackground: " + lastActivityInBackground);
            if (client.getState() != NclWebSocketChannelClient.WebSocketConnectionState.CONNECTED) {

                if (handler.getLooper().getThread().isAlive()) {
                    Log.d(TAG, "thread alive");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "reconnect");
                            client.disconnect(true);
                            client.connect(uri.toString());
                        }
                    });
                } else {
                    Log.d(TAG, "thread not alive");
//                    HandlerThread handlerThread = new HandlerThread(TAG);
//                    handlerThread.start();
//                    handler = new Handler(handlerThread.getLooper());
//
//                    client = new NclWebSocketChannelClient(handler, this);
//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            client.connect(uri.toString());
//                        }
//                    });
                }

                Log.d(TAG, "after handle post " + handler.getLooper().getThread().isAlive());
                if (!lastActivityInBackground.equals("LoginActivity") &&
                    !lastActivityInBackground.equals("SplashScreenActivity") &&
                        !activity.getLocalClassName().equals("SplashScreenActivity") &&
                        !activity.getLocalClassName().equals("LoginActivity")) {

                    Intent intent = new Intent(activity, SplashScreenActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                }
            }
            lastActivityInBackground = "";
        }
    }

    @Override
    public void onActivityPaused(Activity activity){
        Log.d(TAG, "onActivityPaused " + activity.getLocalClassName());
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated " + activity.getLocalClassName());
        activityCount++;
        Log.d(TAG, "activityCount: " + activityCount);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d(TAG, "onActivityStarted " + activity.getLocalClassName());
//        currentActivity = activity;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.d(TAG, "onActivityStopped " + activity.getLocalClassName());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.d(TAG, "onActivitySaveInstanceState " + activity.getLocalClassName());
        inBackground = true;
        lastActivityInBackground = activity.getLocalClassName();
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "onActivityDestroyed " + activity.getLocalClassName());
        activityCount--;
        Log.d(TAG, "activityCount: " + activityCount);
    }

    public void setUserInfo(String id, String userId, String name, String balance, String balanceLabel) {
        user.setInformation(id, userId, name, balance, balanceLabel);
    }

    public void addListener(NclWebSocketChannelClient.WebSocketChannelEvents listener) {
        synchronized(listenersLock) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(NclWebSocketChannelClient.WebSocketChannelEvents listener) {
        synchronized(listenersLock) {
            if (listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }

    public void startGetPeerListTask() {
        if (timerGetPeerList != null) {
            Log.w(TAG, "Get peer list task has started");
            return;
        }
        TimerTask task = new TimerTask(){
            public void run() {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "getpeerlist");
                    json.put("id", user.id);
                    json.put("userid", user.userId);

                    client.send(json.toString());
                } catch (JSONException e) {
                    //reportError("WebSocket register JSON error: " + e.getMessage());
                }
            }
        };
        timerGetPeerList = new Timer(true);
        timerGetPeerList.schedule(task, 1000, 30000);
    }

    public void stopGetPeerListTask() {
        if (timerGetPeerList != null) {
            timerGetPeerList.cancel();
            timerGetPeerList = null;
        }
    }

    public void sendDeviceInfo() {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "deviceinfo");
            json.put("id", user.id);
            json.put("device", getOSVersion() + " : " + getDeviceName());

            client.send(json.toString());
        } catch (JSONException e) {
            //reportError("WebSocket register JSON error: " + e.getMessage());
        }
    }

    private String getOSVersion() {
        return "Android " + Build.VERSION.RELEASE;
    }

    /** Returns the consumer friendly device name */
    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    private void fetchIceServers() {
        //if (!hasGotIceServers) {
            JSONObject json = new JSONObject();
            try {
                json.put("type", "getstun");

                client.send(json.toString());
            } catch (JSONException e) {
                //reportError("WebSocket register JSON error: " + e.getMessage());
            }
        //}
    }

    private void onStunResult(JSONObject stunresult) {
        try {
            iceServers.clear();
            final JSONArray iceServersJson = new JSONArray(
                    stunresult.getString("payload"));
            for (int i = 0; i < iceServersJson.length(); ++i) {
                JSONObject ice = iceServersJson
                        .getJSONObject(i);
                String userName = "";
                if (ice.has("username")) {
                    userName = ice.getString("username");
                }

                String iceUrl = "";
                if (ice.has("url")) {
                    iceUrl = ice.getString("url");
                }

                String credential = "";
                if (ice.has("credential")) {
                    credential = ice.getString("credential");
                }
                if (!credential.equals("")) {
                    PeerConnection.IceServer iceServer = new PeerConnection.IceServer(
                            iceUrl, userName, credential);
                    iceServers.add(iceServer);
                }
            }
//            hasGotIceServers = true;
            Log.d(TAG, "ice servers: " + iceServers.size());
        } catch (JSONException e) {
            //reportError("WebSocket register JSON error: " + e.getMessage());
        }
    }

//    private void initIceServers() {
////		PeerConnection.IceServer iceServer = new PeerConnection.IceServer(
////				"turn:global.turn.twilio.com:3478?transport=udp",
////				"d0d4f1df0fe52aaa1c26428147b226e2e794ee4a12f7b9eb3126ecc40ec64e5e",
////				"W55cQ2i4plU00zIgfqHhw7aNPjS8tTY04B5OPV3kaFY=");
////		iceServers.add(iceServer);
//
//        httpConnection = new AsyncHttpURLConnection(
//                "GET",
//                "https://secure.visitel.us/visitation/twilio.aspx",
//                "", new AsyncHttpEvents() {
//            @Override
//            public void onHttpError(String errorMessage) {
//                Log.e(TAG, "ICE server connection error: "
//                        + errorMessage);
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        new Handler().postDelayed(new Runnable(){
//                            public void run() {
//                                initIceServers();
//                            }
//                        }, 30000);
//                    }
//                });
//            }
//
//            @Override
//            public void onHttpComplete(String response) {
//                Log.d(TAG, response);
//                try {
//                    JSONArray iceServersJson = new JSONArray(response);
//
//                    for (int i = 0; i < iceServersJson.length(); ++i) {
//                        JSONObject ice = iceServersJson
//                                .getJSONObject(i);
//                        String userName = "";
//                        if (ice.has("username")) {
//                            userName = ice.getString("username");
//                        }
//
//                        String iceUrl = "";
//                        if (ice.has("url")) {
//                            iceUrl = ice.getString("url");
//                        }
//
//                        String credential = "";
//                        if (ice.has("credential")) {
//                            credential = ice.getString("credential");
//                        }
//                        if (!credential.equals("")) {
//                            PeerConnection.IceServer iceServer = new PeerConnection.IceServer(
//                                    iceUrl, userName, credential);
//                            iceServers.add(iceServer);
//                        }
//                    }
//                } catch (JSONException e) {
//                    // TODO Auto-generated catch block
//                    Log.e(TAG, "Error to parse ice message", e);
//                }
//            }
//        });
//        httpConnection.send();
//    }

    private void startWSConnectionTimer() {
        final NclApplication app = this;
        TimerTask task = new TimerTask(){
            public void run() {
                Log.d(TAG, "WS Connection time out");
                stopGetPeerListTask();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        client.disconnect(true);
                        client = null;
                        client = new NclWebSocketChannelClient(handler, app);
                        if (!inBackground) {
                            client.connect(uri.toString());
                        } else {
                            Log.d(TAG, "In background, no need to reconnect");
                        }
                    }
                });

            }
        };
        if (timerWSConnection != null) {
            timerWSConnection.cancel();
            timerWSConnection = null;
        }
        timerWSConnection = new Timer();
        timerWSConnection.schedule(task, 60000);
    }

}
