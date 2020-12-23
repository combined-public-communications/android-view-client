package com.combinedpublic.mobileclient.Classes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;

import java.util.Date;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "development@combinedpublic.com")

public class CpcApplication extends MultiDexApplication implements LifecycleObserver {

    public static boolean IS_APP_IN_FOREGROUND = true;

    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        if (!BuildConfig.DEBUG && !Configuration.CombinedPublic.isEmulator()) {
            ACRA.init(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void appInForeground(){
        IS_APP_IN_FOREGROUND = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void appInBackground(){
        IS_APP_IN_FOREGROUND = false;

        if (User.getInstance().isLoggedIn != null && User.getInstance().isLoggedIn) {
            Date date = new Date();

            SharedPreferences settings = getSharedPreferences("Settings", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("time" , date.getTime());
            editor.apply();
            Log.d("Application", "Time is saved");
        }
    }
}
