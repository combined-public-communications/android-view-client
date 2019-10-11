package us.visitel.mobileclient;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import us.visitel.mobileclient.R;

/**
 * Fragment for call control.
 */
public class ContactButtonFragment extends Fragment {
    private static final String TAG = "ContactButtonFragment";
    private View controlView;
    private ImageView contactImage;
    private TextView contactText;
    private ButtonEvent buttonEvent;
    private Context context;

    public ContactButtonFragment() {
        buttonEvent = null;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        controlView =
                inflater.inflate(R.layout.fragment_contact_button, container, false);

        // Create UI controls.
        contactImage =
                (ImageView) controlView.findViewById(R.id.button_contact_image);
        contactText =
                (TextView) controlView.findViewById(R.id.button_contact_text);

        controlView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonEvent.onButtonClicked(MenuFragment.ActiveMenuItem.MENU_ITEM_CONTACT);
            }
        });
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
        buttonEvent = (ButtonEvent) activity;
        context = activity.getBaseContext();
    }

    public void setActive(boolean active) {
        if (active) {
            contactImage.setImageResource(R.drawable.contact_on);
            contactText.setTextColor(Color.rgb(82,173,255));
            //contactText.setCompoundDrawables(context.getResources().getDrawable(R.drawable.contact_on, null), null, null, null);
        } else {
            contactImage.setImageResource(R.drawable.contact_off);
            contactText.setTextColor(Color.rgb(204,204,204));
//            contactText.setCompoundDrawables(context.getResources().getDrawable(R.drawable.contact_off, null), null, null, null);
        }
    }

}
