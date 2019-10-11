package us.visitel.mobileclient;

import android.app.Activity;
import android.app.Fragment;
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
public interface ButtonEvent {
    public void onButtonClicked(MenuFragment.ActiveMenuItem item);
}
