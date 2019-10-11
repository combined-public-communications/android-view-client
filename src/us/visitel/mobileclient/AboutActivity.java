package us.visitel.mobileclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;


/**
 * Handles the initial setup where the user selects which room to join.
 */
public class AboutActivity extends Activity
        implements TitleBarFragment.OnButtonEvents {
    private static final String TAG = "AboutActivity";
    private static final int CONNECTION_REQUEST = 1;

    private TitleBarFragment titleBarFragment;
    private LinearLayout layoutWebsite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        titleBarFragment = new TitleBarFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TitleBarFragment.TITLE, "About");
        titleBarFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .add(R.id.menu_fragment_container, titleBarFragment)
                .commit();

        layoutWebsite = (LinearLayout)findViewById(R.id.website);
        layoutWebsite.setOnClickListener(websiteListener);
    }

    @Override
    public void onBack() {
        // TODO Auto-generated method stub
        setResult(RESULT_OK);
        finish();
    }

    private final OnClickListener websiteListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://secure.visitel.us"));
            startActivity(intent);
        }
    };
}