package com.selbie.wrek4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

public class ScheduleFetcher implements ScheduleFetcherTask.ScheduleFetcherTaskCallback
{

    // public constants --------------------------------
    public interface ScheduleFetcherCallback
    {
        void onNewSchedule(ArrayList<ScheduleItem> schedule);
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
        _lastUpdate.setTimeInMillis(0);

        _latest = new ArrayList<ScheduleItem>(); // empty list
        _observerSet = new HashSet<ScheduleFetcherCallback>();
    }
    
    public ScheduleFetcher getInstance()
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
    public void notifyObservers()
    {
        // iterate over the clone in case one observer takes another one out in the callback
        // this is probably overkill
        HashSet<ScheduleFetcherCallback> observers = (HashSet<ScheduleFetcherCallback>) _observerSet.clone();
        for (ScheduleFetcherCallback callback : observers)
        {
            if (_observerSet.contains(callback))
            {
                callback.onNewSchedule(_latest);
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
        _latest = schedule;
        _lastUpdate = Calendar.getInstance();

        notifyObservers();
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