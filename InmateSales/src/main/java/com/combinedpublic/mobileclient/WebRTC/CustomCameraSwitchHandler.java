package com.combinedpublic.mobileclient.WebRTC;

import org.webrtc.CameraVideoCapturer;

/**
 * Webrtc_Step2
 * Created by vivek-3102 on 11/03/17.
 */

public class CustomCameraSwitchHandler implements CameraVideoCapturer.CameraSwitchHandler {

    private String logTag = this.getClass().getCanonicalName();

    public CustomCameraSwitchHandler(String logTag) {
        this.logTag = this.getClass().getCanonicalName();
        this.logTag = this.logTag+" "+logTag;
    }

    @Override
    public void onCameraSwitchDone(boolean b) {

    }

    @Override
    public void onCameraSwitchError(String s) {

    }
}