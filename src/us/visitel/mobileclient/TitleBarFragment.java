package us.visitel.mobileclient;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.webrtc.StatsReport;

import java.util.HashMap;
import java.util.Map;


/**
 * Fragment for call control.
 */
public class TitleBarFragment extends Fragment {
    public static final String TITLE =
            "us.visitel.mobileclient.FRAGMENTTITLE";
    private static final String TAG = "TitleBarFragment";
    private View controlView;

    private ImageButton backButton;
    private TextView textTitle;
    //private UnderscoreImageButton settingsButton;
    private OnButtonEvents buttonEvents;



    /**
     * Call control interface for container activity.
     */
    public interface OnButtonEvents {
        public void onBack();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        controlView =
                inflater.inflate(R.layout.fragment_titlebar, container, false);

        // Create UI controls.
        backButton =
                (ImageButton) controlView.findViewById(R.id.button_back);
        textTitle =
                (TextView) controlView.findViewById(R.id.title);

        Bundle bundle = this.getArguments();
        String title = bundle.getString(TITLE, "");
        textTitle.setText(title);

        // Add buttons click events.
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonEvents.onBack();
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
        buttonEvents = (OnButtonEvents) activity;
    }

}
