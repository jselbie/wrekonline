package com.selbie.wrek;

import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;

public class TextViewRotator
{
    public final static String TAG = TextViewRotator.class.getSimpleName();
    
    TextView _textview;
    
    String _primary;
    String _alternate;
    boolean _showingPrimary;
    CountDownTimer _timer;
    int _interval;
    
    public TextViewRotator(TextView tv, int intervalMilliseconds)
    {
        assert(tv != null);
        _textview = tv;
        _primary = "";
        _alternate = "";
        _interval = intervalMilliseconds;
        _showingPrimary = true;
    }
    
    public void stop()
    {
        if (_timer != null)
        {
            _timer.cancel();
            _timer = null;
        }        
    }
    
    private boolean canRotate()
    {
        boolean result = !(_primary.equals("") ||  _alternate.equals(""));
        return result;
    }
    
    private void updateText()
    {
        if (!_primary.equals(""))
        {
            Log.d(TAG, "updateText called - switching to primary text");
            
            _showingPrimary = true;
            _textview.setText(_primary);
        }
        else
        {
            Log.d(TAG, "updateText called - switching to alternate text");
            _showingPrimary = false;
            _textview.setText(_alternate);
        }
    }
    
    private void rotate()
    {
        
        if (_showingPrimary)
        {
            Log.d(TAG, "rotate called - switching to alt text");
            
            _showingPrimary = false;
            _textview.setText(_alternate);
        }
        else
        {
            Log.d(TAG, "rotate called - switching to primary text");
            
            _showingPrimary = true;
            _textview.setText(_primary);
        }
    }
    
    public void setText(String primary, String alternate)
    {
        boolean noChange = false;
        
        if (primary == null)
        {
            primary = "";
        }
        
        if (alternate == null)
        {
            alternate = "";
        }
        
        noChange = (primary.equals(_primary) && alternate.equals(_alternate));
        
        _primary = primary;
        _alternate = alternate;
        
        if (noChange == false)
        {
            // switch back to primary for new string set
            updateText();
        }
        
        if (canRotate())
        {
            stop(); // always kill any existing timer before starting a new one!
            
            long forever = Long.MAX_VALUE;
            _timer = new CountDownTimer(forever, _interval) {

                @Override public void onFinish() {} 

                @Override
                public void onTick(long millisUntilFinished)
                {
                    rotate();
                }};
            _timer.start();
        }
        else
        {
            stop();
        }
    }
    
    
}
