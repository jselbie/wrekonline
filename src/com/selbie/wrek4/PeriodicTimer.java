package com.selbie.wrek4;

import android.os.Handler;

public class PeriodicTimer
{

    public interface PeriodicTimerCallback
    {
        void onTimerCallback(Object token);
    }

    private int _intervalMilliseconds;
    private Handler _handler;
    private boolean _oneshot;
    private Runnable _runnable;
    private Object _token;
    private PeriodicTimerCallback _callback;
    private long _threadid;
    private boolean _isStarted;

    public PeriodicTimer(PeriodicTimerCallback callback, int intervalMilliseconds, boolean oneshot, Object token)
    {
        assert ((callback != null) && (intervalMilliseconds >= 0));

        _callback = callback;
        _handler = null;
        _oneshot = oneshot;
        _intervalMilliseconds = intervalMilliseconds;
        _runnable = new Runnable()
        {
            @Override
            public void run()
            {
                OnTimerCallback();
            }
        };

        _token = token;
    }

    public void Start()
    {
        if (_handler != null)
        {
            return;
        }

        _threadid = Thread.currentThread().getId();

        _isStarted = true;

        _handler = new Handler();
        _handler.postAtTime(_runnable, android.os.SystemClock.uptimeMillis() + _intervalMilliseconds);
    }

    public void Stop()
    {
        if (_handler != null)
        {
            _handler.removeCallbacksAndMessages(null);
        }

        _isStarted = false;
    }

    public boolean isStarted()
    {
        return _isStarted;
    }

    private void OnTimerCallback()
    {
        if (_handler == null)
        {
            return;
        }

        // assert we are getting called on the thread we started on
        assert (_threadid == Thread.currentThread().getId());

        // if we are oneshot, stop now, that way the app can call Start() within
        // the callback if it wants to restart
        if (_oneshot)
        {
            Stop();
        }

        // callback to the app code that wanted to know about us
        _callback.onTimerCallback(_token);

        // recheck the handler even if we aren't a one shot - Stop may have been
        // called in the callback
        if ((_handler != null) && (_oneshot == false))
        {
            // reschedule for the next period
            _handler.postAtTime(_runnable, android.os.SystemClock.uptimeMillis() + _intervalMilliseconds);
        }

    }

}
