package us.visitel.mobileclient;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


public class NclUser {
    private static final String TAG = "NclUser";

    public class Contact {
        String id;
        String name;

        public Contact(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public final String getId() {
            return id;
        }

        public final String getName() {
            return name;
        }
    }

    public String id;
    public String userId;
    public String name;
    public String balance;
    public String balanceLabel;
    public ArrayList<Contact> contacts;

    public NclUser() {
        balance = "0";
        contacts = new ArrayList<Contact>();
    }

    public void setInformation(String id, String userId, String name, String balance, String balanceLabel) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.balance = balance;
        this.balanceLabel = balanceLabel;
    }

    public Contact getContact(final String id) {
        Contact contact = null;
        for (Contact c : contacts) {
            if (c.id.equals(id)) {
                contact = c;
                break;
            }
        }
        return contact;
    }

    public Contact getContact(final int index) {
        if (index >= 0 && index < contacts.size()) {
            return contacts.get(index);
        }
        return null;
    }

    public void updateContacts(JSONArray contactsJson) {
        contacts.clear();
        try {
            for (int i = 0; i < contactsJson.length(); i++) {
                JSONObject contactJson;
                contactJson = contactsJson.getJSONObject(i);
                Contact contact = new Contact(
                        contactJson.getString("id"),
                        contactJson
                                .getString("displayname"));
                contacts.add(contact);
//                Log.d(TAG, "contact id:"+contact.id + " name:"+contact.name);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Error to add contact", e);
        }
    }
}
