package us.visitel.mobileclient;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;
import android.widget.TextView;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import us.visitel.mobileclient.R;

/**
 * Fragment for call control.
 */
public class MenuFragment extends Fragment {
    private static final String TAG = "MenuFragment";
    public enum ActiveMenuItem {
        MENU_ITEM_PROFILE,
        MENU_ITEM_CONTACT
    }
    private View controlView;

    private ProfileButtonFragment profileButton;
    private ContactButtonFragment contactsButton;
    //private UnderscoreImageButton settingsButton;
    private OnMenuEvents menuEvents;


    private volatile boolean isRunning;
    //private TextView hudView;
    //private final CpuMonitor cpuMonitor = new CpuMonitor();


    /**
     * Call control interface for container activity.
     */
    public interface OnMenuEvents {
        public void onProfile();
        public void onContacts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        controlView =
                inflater.inflate(R.layout.fragment_mainmenubar, container, false);

        // Create UI controls.
        profileButton = new ProfileButtonFragment();
        contactsButton = new ContactButtonFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.profile_button_fragment_container, profileButton);
        ft.add(R.id.contact_button_fragment_container, contactsButton);
        ft.commit();

        return controlView;
    }

    @Override
    public void onStart() {
        super.onStart();

//        Bundle args = getArguments();
//        if (args != null) {
//            String contactName = args.getString(VideoCallActivity.EXTRA_CONTACTNAME);
//        }

        isRunning = true;


    }

    public void setActive(ActiveMenuItem menuItem) {
        switch (menuItem)
        {
            case MENU_ITEM_PROFILE:
                profileButton.setActive(true);
                contactsButton.setActive(false);
                break;
            case MENU_ITEM_CONTACT:
                profileButton.setActive(false);
                contactsButton.setActive(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onStop() {
        isRunning = false;
        super.onStop();
    }

    // TODO: Replace with onAttach(Context) once we only support API level 23+.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        menuEvents = (OnMenuEvents) activity;
    }


}
