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

package com.selbie.wrek;

import android.os.CountDownTimer;
import android.widget.TextView;

public class TextSwapper
{
    private boolean _isPrimaryShown;
    private TextView _tv;
    private CountDownTimer _timer;
    private String _primary;
    private String _secondary;
    
    public TextSwapper(TextView tv)
    {
        _isPrimaryShown = true;
        _tv = tv;
        _primary = "";
        _secondary = "";
    }
    
    
    // starts the rotation time if it is not already running. Does not modify any text fields  until the timer period elapses
    private void start()
    {
        _timer = new CountDownTimer(Long.MAX_VALUE, 4000) // 4 second interval
        {
            @Override public void onFinish() {}

            @Override public void onTick(long millisUntilFinished)
            {
                String str = _isPrimaryShown ? _secondary : _primary;
                _isPrimaryShown = !_isPrimaryShown;
                _tv.setText(str);
            }
        };
        
        _timer.start();
        
    }
    
    // cancels and nulls out the timer
    public void stop()
    {
        if (_timer != null)
        {
            _timer.cancel();
            _timer = null;
        }
    }
    
    boolean areBothStringsSet()
    {
        boolean both = (!_primary.equals("") && !_secondary.equals(""));
        return both;
    }
    
    // sets the primary string if it has changed
    // makes the new string active
    // stops and restarts the timer if the secondary string is non-empty
    public void setPrimary(String str)
    {
        if (str == null)
        {
            str = "";
        }
        
        // don't mess with the timer logic if nothing changed
        if (_primary.equals(str))
        {
            return;
        }
        
        _primary = str;
        
        stop();
        
        _tv.setText(_primary);
        _isPrimaryShown = true;

        if (areBothStringsSet())
        {
            start();
        }
        
    }

    // sets the secondary string if it has changed
    // if the active string is the previous secondary string, show the new secondary string
    // restart timer as appropriate
    public void setSecondary(String str)
    {
        if (str == null)
        {
            str = "";
        }

        // don't mess with the timer logic if nothing changed
        if (_secondary.equals(str))
        {
            return;
        }
        
        _secondary = str;
        if (!_isPrimaryShown)
        {
            _tv.setText(_secondary);
        }
        
        if (areBothStringsSet())
        {
            stop();
            start();
        }
        
    }
    
    

}
