package com.selbie.wrek4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ProgressBar;


class ScheduleItemTemp
{
    public String Title;
    public String Description;
    public String ShowTime;
    public String URL;
    public int PictureID;
}


public class MainActivity extends Activity
{
    public final static String TAG = MainActivity.class.getSimpleName();
    
    ListView _listview;
    ProgressBar _progbar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        // load up our preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        MediaPlayerService.setContext(this.getApplicationContext());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected (int featureId, MenuItem item) {
        
        if (item.getItemId() == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            this.startActivity(i);
        }
        else if (item.getItemId() == R.id.action_about) {
            Intent i = new Intent(this, AboutActivity.class);
            this.startActivity(i);
        }
        
        return false;
        
    }
    
    @Override
    protected void onStop()
    {
        super.onStop();
    }
    
}

