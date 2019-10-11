package us.visitel.mobileclient;

import java.util.HashMap;
import java.util.List;


import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import us.visitel.mobileclient.R;

public class ContactAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private Activity parentActivity;
    private View contactItemView;

    private NclApplication application;
    private NclUser user;

    public ContactAdapter(Context context) {
        this.context = context;
        parentActivity = (Activity)context;

        application = (NclApplication)parentActivity.getApplication();
        user = application.getUser();
    }

//    public ContactAdapter(LayoutInflater inflater) {
//        this.inflater = inflater;
//        Log.d("ContactAdapter", "inflater: " + this.inflater);
//        application = NclApplication.getInstance();
//        user = application.getUser();
//    }

    @Override
    public int getCount() {
        return user.contacts.size();
    }

    @Override
    public Object getItem(int position) {
        return user.contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return null;
        }

        final View view;

        if (convertView == null) {
            view = inflater.inflate(R.layout.contact_item, parent, false);
        } else {
            view = convertView;
        }

        // if (contactItemView == null) {
        //     contactItemView = inflater.inflate(R.layout.contact_item, parent, false);
        // }
        NclUser.Contact contact = user.contacts.get(position);
        ImageView image = (ImageView) view.findViewById(R.id.online_image);

        TextView contactName = (TextView) view.findViewById(R.id.contact_name);
        if (!contact.id.equals("")) {
            image.setImageResource(R.drawable.circle_filled);
            contactName.setTextColor(Color.BLACK);
        } else {
            image.setImageResource(R.drawable.circle_outline);
            contactName.setTextColor(Color.GRAY);
        }

        contactName.setText(contact.name);

//        Log.d("ContactAdapter", "contact " + position + ": " + contact.getName());
        return view;
    }

}