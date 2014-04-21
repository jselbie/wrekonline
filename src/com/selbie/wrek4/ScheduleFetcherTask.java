package com.selbie.wrek4;

import java.io.IOException;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

public class ScheduleFetcherTask extends AsyncTask<Void, Integer, ArrayList<ScheduleItem>>
{

    // public statics ------------------------------------
    public interface ScheduleFetcherTaskCallback
    {
        void onComplete(ArrayList<ScheduleItem> schedule);
    }

    public static final String TAG = ScheduleFetcherTask.class.getSimpleName();

    public static final String Source = "http://www.selbie.com/wrek/wrek_schedule.json";
    public static final int ConnectionTimeout = 30000;
    public static final int DownloadTimeout = 30000;
    public static final int MAX_ATTEMPTS = 2;

    // private internals ----------------------------------
    ScheduleFetcherTaskCallback _callback;

    public ScheduleFetcherTask(ScheduleFetcherTaskCallback callback)
    {
        _callback = callback;
    }

    @Override
    protected void onPostExecute(ArrayList<ScheduleItem> schedule)
    {
        if (_callback != null)
        {
            _callback.onComplete(schedule);
        }
    }
    
    @Override
    protected ArrayList<ScheduleItem> doInBackground(Void... arg0)
    {
        int attemptcount = 0;
        boolean success = false;
        ArrayList<ScheduleItem> schedule = null;
        
        while ((attemptcount < MAX_ATTEMPTS) && (this.isCancelled() == false) && (success == false))
        {
            try
            {
                attemptcount++;

                Log.d(TAG, "Attempting download");
                String json = ContentDownloader.downloadString(ScheduleFetcherTask.Source);
                Log.d(TAG, "Attempting parse");
                JsonHandler handler = new JsonHandler();
                schedule = handler.ExtractScheduleFromJson(json);
                
                break;
            }
            catch (IOException e)
            {
                Log.d(TAG, "failed to download or parse schedule", e);
            }

            if (attemptcount < MAX_ATTEMPTS)
            {
                // sleep 1 second before trying a download again
                sleepLoop(1000);
            }
        }

        return schedule;
    }

    private void sleepLoop(int milliseconds)
    {
        // cheezy sleep loop that periodically wakes up and checks for
        // cancellation

        long startTime = System.currentTimeMillis();
        long elapsed = 0;

        while ((elapsed < milliseconds) && (this.isCancelled() == false))
        {
            long timeout = (elapsed < 100) ? elapsed : 100;

            try
            {
                Thread.sleep(timeout);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            elapsed = System.currentTimeMillis() - startTime;
        }

    }

}
