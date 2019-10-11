package us.visitel.mobileclient;

import android.os.Handler;
import android.util.Log;

import de.tavendo.autobahn.WebSocket.WebSocketConnectionObserver;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;


import java.util.Timer;
import java.util.TimerTask;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

/**
 * WebSocket client implementation.
 *
 * <p>All public methods should be called from a looper executor thread
 * passed in a constructor, otherwise exception will be thrown.
 * All events are dispatched on the same thread.
 */

public class NclWebSocketChannelClient {
    private static final String TAG = "WSChannelRTCClient";
    private static final int CLOSE_TIMEOUT = 1000;
    private final WebSocketChannelEvents events;
    private final Handler handler;
    private WebSocketConnection ws;
    private WebSocketObserver wsObserver;
    private String wsServerUrl;
    private WebSocketConnectionState state;
    private final Object closeEventLock = new Object();
    private boolean closeEvent;

    /**
     * Possible WebSocket connection states.
     */
    public enum WebSocketConnectionState { NEW, CONNECTED, CLOSED, ERROR }

    /**
     * Callback interface for messages delivered on WebSocket.
     * All events are dispatched from a looper executor thread.
     */
    public interface WebSocketChannelEvents {
        void onWebSocketOpen();
        void onWebSocketMessage(final String message);
        void onWebSocketClose();
        void onWebSocketError(final String description);
    }

    public NclWebSocketChannelClient(Handler handler, WebSocketChannelEvents events) {
        this.handler = handler;
        this.events = events;
        state = WebSocketConnectionState.NEW;
    }

    public WebSocketConnectionState getState() {
        return state;
    }

    public void connect(final String wsUrl) {
        checkIfCalledOnValidThread();
        if (state != WebSocketConnectionState.NEW && state != WebSocketConnectionState.CLOSED) {
            Log.e(TAG, "WebSocket is already connected. " + state);
            return;
        }
        wsServerUrl = wsUrl;

        closeEvent = false;

        Log.d(TAG, "Connecting WebSocket to: " + wsUrl);
        ws = new WebSocketConnection();
        wsObserver = new WebSocketObserver();
        try {
            WebSocketOptions options = new WebSocketOptions();
            options.setReconnectInterval(5000);
            ws.connect(new URI(wsServerUrl), wsObserver, options);
        } catch (URISyntaxException e) {
            reportError("URI error: " + e.getMessage());
        } catch (WebSocketException e) {
            // Can not connect to the server for the first time,
            // not reconnect
            reportError("WebSocket connection error: " + e.getMessage());
        }
    }

    public void send(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkIfCalledOnValidThread();
                switch (state) {
                    case NEW:
                    case CONNECTED:
                        // Store outgoing messages and send them after websocket client
                        // is registered.
                        Log.d(TAG, "WS ACC: " + message);
                        if (ws.isConnected()) {
                            ws.sendTextMessage(message);
                        } else {
                            Log.e(TAG, "WebSocket not connected");
                        }

                        return;
                    case ERROR:
                    case CLOSED:
                        Log.e(TAG, "WebSocket send() in error or closed state : " + message);
                }
            }
        });
    }



    public void disconnect(boolean waitForComplete) {
        checkIfCalledOnValidThread();
        Log.d(TAG, "Disonnect WebSocket. State: " + state);

        // Close WebSocket in CONNECTED or ERROR states only.
        if (state == WebSocketConnectionState.CONNECTED
                || state == WebSocketConnectionState.ERROR) {
            ws.disconnect();

            // Send DELETE to http WebSocket server.
            //sendWSSMessage("DELETE", "");

            state = WebSocketConnectionState.CLOSED;

            // Wait for websocket close event to prevent websocket library from
            // sending any pending messages to deleted looper thread.
            if (waitForComplete) {
                synchronized (closeEventLock) {
                    while (!closeEvent) {
                        try {
                            closeEventLock.wait(CLOSE_TIMEOUT);
                            break;
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Wait error: " + e.toString());
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Disonnecting WebSocket done.");
    }

    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state != WebSocketConnectionState.ERROR) {
                    state = WebSocketConnectionState.ERROR;
                    events.onWebSocketError(errorMessage);
                }
            }
        });
    }


    // Helper method for debugging purposes. Ensures that WebSocket method is
    // called on a looper thread.
    private void checkIfCalledOnValidThread() {
        if (Thread.currentThread() != handler.getLooper().getThread()) {
            throw new IllegalStateException("WebSocket method is not called on valid thread");
        }
    }

    private class WebSocketObserver implements WebSocketConnectionObserver {
        @Override
        public void onOpen() {
            Log.d(TAG, "WebSocket connection opened to: " + wsServerUrl);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    state = WebSocketConnectionState.CONNECTED;
                    events.onWebSocketOpen();
                }
            });
        }

        @Override
        public void onClose(WebSocketCloseNotification code, String reason) {
            Log.d(TAG, "WebSocket connection closed. Code: " + code
                    + ". Reason: " + reason + ". State: " + state);
            synchronized (closeEventLock) {
                closeEvent = true;
                closeEventLock.notify();
            }
            final WebSocketCloseNotification websocketCode = code;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (state != WebSocketConnectionState.CLOSED) {
                        state = WebSocketConnectionState.CLOSED;
                    }
                    Log.d(TAG, "onWebSocketClose");
                    events.onWebSocketClose();

//                    class NewTask extends TimerTask{
//                        public void run(){
//                            try {
//                                Log.d(TAG, "RECONNECT task, state: " + state);
//                                if (state == WebSocketConnectionState.CLOSED) {
//                                    WebSocketOptions options = new WebSocketOptions();
//                                    options.setReconnectInterval(5000);
//                                    ws.connect(new URI(wsServerUrl), wsObserver, options);
//                                }
//                            } catch (URISyntaxException e) {
//                                reportError("URI error: " + e.getMessage());
//                            } catch (WebSocketException e) {
//                                reportError("WebSocket connection error: " + e.getMessage());
//                            }
//                        }
//                    }

//                    if (websocketCode != WebSocketCloseNotification.RECONNECT) {
//                        Log.d(TAG, "START reconnect task");
//                        int delay=5000;
//                        Timer timer=new Timer();
//                        NewTask myTask=new NewTask();
//                        timer.schedule(myTask,delay);
//                    }
                }
            });
        }

        @Override
        public void onTextMessage(String payload) {
            Log.d(TAG, "WSS->C: " + payload);
            final String message = payload;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (state == WebSocketConnectionState.CONNECTED) {
                        events.onWebSocketMessage(message);
                    }
                }
            });
        }

        @Override
        public void onRawTextMessage(byte[] payload) {
        }

        @Override
        public void onBinaryMessage(byte[] payload) {
        }
    }

}
