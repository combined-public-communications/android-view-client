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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.appspot.apprtc.util.AsyncHttpURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

import us.visitel.mobileclient.R;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class ContactsFragment extends Fragment {

    private static final String TAG = "ContactsFragment";

    public interface OnContactEvents {
        public void onToast(String msg);
        public void onCallout(String id, String name, boolean isInitiator);
    }

    private View controlView;
    private ViewGroup container;
    private ListView contactListView;
    private SharedPreferences sharedPref;

    private ArrayList<String> contactList;
    private ContactAdapter adapter;
    private LayoutInflater inflater;
    private OnContactEvents contactEvents;
    private Activity parentActivity;


    private NclApplication application;
    private NclUser user;

    private boolean isInitiator;
    private NclUser.Contact contact;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        this.container = container;

        controlView =
                inflater.inflate(R.layout.fragment_contacts, container, false);

        contactListView = (ListView) controlView.findViewById(R.id.contacts_listview);
        contactListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        contactListView.setOnItemClickListener(contactClickListener);


        isInitiator = false;
        return controlView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    // TODO: Replace with onAttach(Context) once we only support API level 23+.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        contactEvents = (OnContactEvents) activity;
        parentActivity = activity;
        application = (NclApplication)parentActivity.getApplication();
        user = application.getUser();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ContactsActivity destroyed");
        super.onDestroy();
    }



    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

        adapter = new ContactAdapter(parentActivity);
        contactListView.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            contactListView.requestFocus();
            contactListView.setItemChecked(0, true);
        }
    }

    public void updateContacts() {
        adapter.notifyDataSetChanged();
    }


    private final OnItemClickListener contactClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            contact = user.getContact(position);
            if (contact != null) {
                if (contact.getId().equals("")) {
                    logAndToast("Contact " + contact.getName() + " is not online");
                    return;
                }
                isInitiator = true;

                Log.d(TAG, "Selected contact: " + contact.getId() + " "
                        + contact.getName());

                if (contactEvents != null) {
                    contactEvents.onCallout(contact.getId(), contact.getName(), isInitiator);
                }

            }

        }
    };



    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (contactEvents != null) {
            contactEvents.onToast(msg);
        }
    }

}
