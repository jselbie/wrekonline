package com.selbie.wrek4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import android.util.Log;

public class ScheduleFetcher implements ScheduleFetcherTask.ScheduleFetcherTaskCallback
{
    public final static String TAG = ScheduleFetcher.class.getSimpleName();

    // public constants --------------------------------
    public interface ScheduleFetcherCallback
    {
        void onNewSchedule(ArrayList<ScheduleItem> schedule);
        void onScheduleDownloadError(int errorcode);
    }
    
    public static final long SECONDS_IN_HOUR = 60 * 60;
    public static final long REFRESH_INTERVAL_SECONDS = 12 * SECONDS_IN_HOUR; // redownload every half day

    // private members ---------------------------------
    boolean _downloadInProgress;
    ArrayList<ScheduleItem> _latest;
    HashSet<ScheduleFetcherCallback> _observerSet;
    Calendar _lastUpdate;
    ScheduleFetcherTask _task;
    
    static ScheduleFetcher _instance;
    

    public ScheduleFetcher()
    {
        _downloadInProgress = false;

        _lastUpdate = Calendar.getInstance();
        _lastUpdate.setTimeInMillis(0); // set timestamp to the epoch so we're guaranteed to get a refresh the next time getInstance is called

        _latest = new ArrayList<ScheduleItem>(); // empty list
        _observerSet = new HashSet<ScheduleFetcherCallback>();
    }
    
    public static ScheduleFetcher getInstance()
    {
        if (_instance == null)
        {
            _instance = new ScheduleFetcher();
        }
        
        return _instance;
    }
    
    
    
    public void attachObserver(ScheduleFetcherCallback callback)
    {
        _observerSet.add(callback);
    }
    
    public void detachObserver(ScheduleFetcherCallback callback)
    {
        _observerSet.remove(callback);
    }
    
    public void shutdown()
    {
        _observerSet.clear();
        if (_task != null)
        {
            _task.cancel(false);
            _task = null;
        }
    }
    
    @SuppressWarnings("unchecked")
    public void notifyObservers(boolean success)
    {
        // iterate over the clone in case one observer takes another one out in the callback
        // this is probably overkill
        HashSet<ScheduleFetcherCallback> observers = (HashSet<ScheduleFetcherCallback>) _observerSet.clone();
        for (ScheduleFetcherCallback callback : observers)
        {
            if (_observerSet.contains(callback))
            {
                if (success)
                {
                    callback.onNewSchedule(_latest);
                }
                else
                {
                    callback.onScheduleDownloadError(0);
                }
            }
        }
    }
    
    public void startRefreshIfNeeded()
    {
        if (isRefreshNeeded())
        {
            startRefresh();
        }
    }

    public ArrayList<ScheduleItem> getLastestSchedule()
    {
        startRefreshIfNeeded();
        return _latest;
    }

    @Override
    public void onComplete(ArrayList<ScheduleItem> schedule)
    {
        _downloadInProgress = false;
        
        if (schedule == null) {
            
            // uh oh... error!
            
            Log.e(TAG, "ScheduleFetcherTask has failed to deliver a schedule");
            Log.e(TAG, "Returning a stale schedule!");
            
            notifyObservers(false);
        }

        if (schedule != null) {
            _latest = schedule;
            _lastUpdate = Calendar.getInstance();
            notifyObservers(true);
        }

    }
    
    boolean isRefreshNeeded()
    {
        long diff_milliseconds = Calendar.getInstance().getTimeInMillis() - _lastUpdate.getTimeInMillis();
        long diff_seconds = diff_milliseconds / 1000;
        return (diff_seconds >= REFRESH_INTERVAL_SECONDS);
    }

    void startRefresh()
    {
        if (_downloadInProgress)
        {
            return;
        }

        _downloadInProgress = true;
        _task = new ScheduleFetcherTask(this);
        _task.execute();
    }
    

}