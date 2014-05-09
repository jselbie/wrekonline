package com.selbie.wrek4;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener
{
    public final static String TAG = SettingsFragment.class.getSimpleName();
    
    // these constants match preferences.xml and strings.xml values
    public static final String BITRATE_KEY = "pref_key_list_bandwidth";
    public static final int BITRATE_SETTINGS_AUTOMATIC = 0;
    public static final int BITRATE_SETTINGS_HIGH = 1;
    public static final int BITRATE_SETTINGS_LOW = 2;

    public static int getBitrateSetting(Context context)
    {
        // As far as I can tell, ListPreference has to be a string type. So we
        // likely have to do some annoying conversions between string and int
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value_as_string = sharedPrefs.getString(BITRATE_KEY, Integer.toString(BITRATE_SETTINGS_AUTOMATIC));
        int result = BITRATE_SETTINGS_AUTOMATIC;

        try
        {
            result = Integer.parseInt(value_as_string);
        }
        catch (NumberFormatException ex)
        {
            Log.e(TAG, "Unable to convert settings bitrate from string to integer", ex);
        }

        return result;
    }    


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        ListPreference listpref = (ListPreference) getPreferenceManager().findPreference(BITRATE_KEY);
        String listval = listpref.getValue();
        updateSummaryFromValue(listpref, listval);

        listpref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        updateSummaryFromValue(preference, (String) newValue);
        return true;
    }
    
    private void updateSummaryFromValue(Preference pref, String val)
    {

        boolean found = false;
        String[] bitrate_values = this.getResources().getStringArray(R.array.bitrates_values);
        String[] bitrate_summary = this.getResources().getStringArray(R.array.bitrates_summary);

        String summary = bitrate_summary[0];

        for (int index = 0; index < bitrate_values.length; index++)
        {
            if (bitrate_values[index].equals(val))
            {
                summary = bitrate_summary[index];
                found = true;
                break;
            }
        }

        if (found == false)
        {
            Log.e(TAG, "Mapping settings value for bandwidth back to summary string has failed");
        }

        pref.setSummary(summary);
    }
}

