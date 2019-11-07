package com.combinedpublic.mobileclient.Classes;

import com.combinedpublic.mobileclient.services.Contact;

import java.util.ArrayList;

public class User {

    private static User sInstance;
    public String name;
    public String userName;
    public String password;
    public String balance;
    public String id;
    public String device;
    public String token;
    public String pushToken;
    public ArrayList<Contact> contacts;
    public Boolean isLoggedIn;
    public Boolean _isUrlOpen = false;
    public Boolean _isService = false;
    public Boolean _isAllPermsGranted;
    public Boolean _isRestarted = false;
    public Boolean _isCallingShowed = false;
    public Boolean _isMainShowed = false;

    public static User getInstance() {
        if (sInstance == null) {
            sInstance = new User();
        }

        return sInstance;
    }

    // Prevent duplicate objects
    private User() {
    }
}