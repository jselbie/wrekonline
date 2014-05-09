package com.selbie.wrek4;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

public class SettingsActivity extends Activity
{
    public static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}

