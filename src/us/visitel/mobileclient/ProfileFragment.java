package us.visitel.mobileclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Handles the initial setup where the user selects which room to join.
 */
public class ProfileFragment extends Fragment {
    public interface OnButtonEvents {
        public void onUnregister();
        public void onSettings();
        public void onAbout();
    }

    private static final String TAG = "ProfileFragment";

    private View controlView;
    private Button addFundButton;
    private Button forgetMeButton;
    private TextView userNameText;
    private TextView balanceLabel;
    private TextView balanceText;
    private ImageButton aboutMoreInfoButton;
    //    private RelativeLayout layoutSettings;
    private LinearLayout layoutAbout;
    private Toast logToast;

    private OnButtonEvents buttonEvents;

//    private DictionaryOpenHelper doh = null;

    private NclApplication application;
    private NclUser user;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        controlView =
                inflater.inflate(R.layout.fragment_profile, container, false);

        userNameText = (TextView) controlView.findViewById(R.id.userName);
        balanceLabel = (TextView) controlView.findViewById(R.id.balanceLabel);
        balanceText = (TextView) controlView.findViewById(R.id.balance);
        
//        layoutSettings = (RelativeLayout)controlView.findViewById(R.id.settings);
//        layoutSettings.setOnClickListener(settingsListener);
        layoutAbout = (LinearLayout)controlView.findViewById(R.id.about);
        layoutAbout.setOnClickListener(aboutListener);
        addFundButton = (Button) controlView.findViewById(R.id.add_fund_button);
        addFundButton.setOnClickListener(addFundListener);

        forgetMeButton = (Button) controlView.findViewById(R.id.forgetMe);
        forgetMeButton.setOnClickListener(forgetMeListener);

        aboutMoreInfoButton = (ImageButton) controlView.findViewById(R.id.about_more_info);
        aboutMoreInfoButton.setOnClickListener(aboutListener);

        return controlView;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ProfileFragment destroyed");
        super.onDestroy();
    }

    // TODO: Replace with onAttach(Context) once we only support API level 23+.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        buttonEvents = (OnButtonEvents) activity;
        application = (NclApplication)activity.getApplication();
        user = application.getUser();
    }

    @Override
    public void onResume() {
        super.onResume();

        userNameText.setText(user.name);
        balanceLabel.setText(user.balanceLabel);
        balanceText.setText(user.balance);
    }

    private final OnClickListener settingsListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            buttonEvents.onSettings();
        }
    };

    private final OnClickListener aboutListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            buttonEvents.onAbout();
        }
    };

    private final OnClickListener addFundListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://secure.visitel.us"));
            startActivity(intent);
        }
    };

    private final OnClickListener forgetMeListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            buttonEvents.onUnregister();
        }
    };


    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        //logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }
}