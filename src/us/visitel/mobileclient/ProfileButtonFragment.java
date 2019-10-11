package us.visitel.mobileclient;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.Image;
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


/**
 * Fragment for call control.
 */
public class ProfileButtonFragment extends Fragment {
    private static final String TAG = "ProfileButtonFragment";
    private View controlView;
    private ImageView profileImage;
    private TextView profileText;
    private ButtonEvent buttonEvent;
    private Context context;

    public ProfileButtonFragment() {
        buttonEvent = null;
    }

    /**
     * Call control interface for container activity.
     */
//  public interface OnCallEvents {
//    public void onCallHangUp();
//    public void onCameraSwitch();
//    public void onVideoScalingSwitch(ScalingType scalingType);
//  }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        controlView =
                inflater.inflate(R.layout.fragment_profile_button, container, false);

        // Create UI controls.
        profileImage =
                (ImageView) controlView.findViewById(R.id.button_profile_image);
        profileText =
                (TextView) controlView.findViewById(R.id.button_profile_text);

        controlView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonEvent.onButtonClicked(MenuFragment.ActiveMenuItem.MENU_ITEM_PROFILE);
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
            profileImage.setImageResource(R.drawable.profile_on);
            profileText.setTextColor(Color.rgb(82,173,255));
//            profileText.setCompoundDrawables(context.getResources().getDrawable(R.drawable.profile_on, null), null, null, null);
        } else {
            profileImage.setImageResource(R.drawable.profile_off);
            profileText.setTextColor(Color.rgb(204,204,204));
//            profileText.setCompoundDrawables(context.getResources().getDrawable(R.drawable.profile_off, null), null, null, null);
        }
    }

}
