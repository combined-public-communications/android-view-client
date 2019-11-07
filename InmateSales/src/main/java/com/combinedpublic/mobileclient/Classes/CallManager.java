package com.combinedpublic.mobileclient.Classes;

import android.os.StrictMode;
import android.util.Log;

import com.combinedpublic.mobileclient.services.ra_call_rcv;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class CallManager {

    public static String LOG_TAG = "CallManagerLogs";
    private static CallManager sInstance;
    public Long _contactId;
    public Long _conversationId;
    public String _contactName;
    public Boolean _isInitiator;
    public Boolean _isIncommingStarted = false;
    public Boolean isStarted = false;
    public Boolean isMinimized = false;
    public SessionDescription offerSdp;
    public ra_call_rcv raCallMsg;

    private static final int TURN_HTTP_TIMEOUT_MS = 5000;

    public static CallManager getInstance() {
        if (sInstance == null) {
            sInstance = new CallManager();
        }

        return sInstance;
    }

    // Prevent duplicate objects
    private CallManager() {
    }

    // Requests & returns a TURN ICE Server based on a request URL.  Must be run
    // off the main thread!

    public List<PeerConnection.IceServer> requestTurnServers(String url)
            throws IOException, JSONException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        List<PeerConnection.IceServer> turnServers = new ArrayList<>();
        Log.d(LOG_TAG, "Request TURN from: " + url);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("REFERER", Configuration.CombinedPublic.turnRequestBaseUrl());
        connection.setConnectTimeout(TURN_HTTP_TIMEOUT_MS);
        connection.setReadTimeout(TURN_HTTP_TIMEOUT_MS);
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Non-200 response when requesting TURN server from " + url + " : "
                    + connection.getHeaderField(null));
        }
        InputStream responseStream = connection.getInputStream();
        String response = drainStream(responseStream);
        connection.disconnect();
        Log.d(LOG_TAG, "TURN response: " + response);

        JSONArray iceServers = new JSONArray(response);

        for (int i = 0; i < iceServers.length(); ++i) {
            JSONObject server = iceServers.getJSONObject(i);
            String turnUrl = server.get("url").toString();
            ArrayList<String> arr = new ArrayList<>();
            arr.add(turnUrl);
            JSONArray turnUrls = new JSONArray(arr);
            String username = server.has("username") ? server.getString("username") : "";
            String credential = server.has("credential") ? server.getString("credential") : "";
            for (int j = 0; j < turnUrls.length(); j++) {
                String turnUrla = turnUrls.getString(j);
                PeerConnection.IceServer turnServer =
                        PeerConnection.IceServer.builder(turnUrla)
                                .setUsername(username)
                                .setPassword(credential)
                                .createIceServer();
                turnServers.add(turnServer);
            }
        }
        return turnServers;
    }

    // Return the list of ICE servers described by a WebRTCPeerConnection
    // configuration string.
    public List<PeerConnection.IceServer> iceServersFromPCConfigJSON(String pcConfig)
            throws JSONException {
        JSONObject json = new JSONObject(pcConfig);
        JSONArray servers = json.getJSONArray("iceServers");
        List<PeerConnection.IceServer> ret = new ArrayList<>();
        for (int i = 0; i < servers.length(); ++i) {
            JSONObject server = servers.getJSONObject(i);
            String url = server.getString("urls");
            String credential = server.has("credential") ? server.getString("credential") : "";
            PeerConnection.IceServer turnServer =
                    PeerConnection.IceServer.builder(url)
                            .setPassword(credential)
                            .createIceServer();
            ret.add(turnServer);
        }
        return ret;
    }

    public List<PeerConnection.IceServer> makeTurnServerRequestToURL() {

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        try {
            List<PeerConnection.IceServer> turnServers = requestTurnServers(Configuration.CombinedPublic.turnRequestUrl());
            for (PeerConnection.IceServer turnServer : turnServers) {
                Log.d(LOG_TAG, "TurnServer: " + turnServer);
                iceServers.add(turnServer);
            }
        } catch (IOException ex) {
            Log.d(LOG_TAG,"Error requestTurnServers is:"+ex.getLocalizedMessage());
        } catch (JSONException jex) {
            Log.d(LOG_TAG,"Error requestTurnServers is:"+jex.getLocalizedMessage());
        }
        return iceServers;
    }


    // Return the contents of an InputStream as a String.
    private static String drainStream(InputStream in) {
        Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
