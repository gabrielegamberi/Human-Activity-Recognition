package com.example.myfirstapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import static android.app.Activity.RESULT_OK;

public class PhoneUtilities {

    public static final int RESULT_PICK_CONTACT =1;

    //Load phone number from Shared Preferences
    public static void loadPhoneNo(Activity activity){
        SharedPreferences sp = activity.getSharedPreferences("settings", MainActivity.MODE_PRIVATE);
        String phoneNo = sp.getString("phone_number", "Contact to notify");
        NavigationView navigationView = activity.findViewById(R.id.navigation_view);
        MenuItem contact = navigationView.getMenu().findItem(R.id.settings_phone_number);
        contact.setTitle(phoneNo);
    }

    //Save phone number into Shared Preferences
    public static void savePhoneNo(Activity activity, String phoneNo){
        //Save the selected contact into Shared Preferences
        SharedPreferences sp = activity.getSharedPreferences("settings", MainActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("phone_number", phoneNo);
        editor.commit();
    }
}
