/*
   Copyright 2014 John Selbie

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


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
            _handler = null;
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
        if ((_handler != null) && (_oneshot == false) && _isStarted)
        {
            // reschedule for the next period
            _handler.postAtTime(_runnable, android.os.SystemClock.uptimeMillis() + _intervalMilliseconds);
        }

    }

}
