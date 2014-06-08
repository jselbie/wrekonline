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

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PlayControlFragment extends Fragment implements MediaPlayerView
{
    public final static String TAG = PlayControlFragment.class.getSimpleName();

    private MediaPlayerPresenter _presenter;
    private SeekBar _seekbar;
    private TextView _timestamp;
    private boolean _seekbarIsAdjusting; // true if the user has his finger on the seekbar
    private ImageButton _playbutton;
    private ImageButton _nextbutton;
    private ImageButton _prevbutton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreateView");
        
        View view = inflater.inflate(R.layout.media_player2, parent, false);
        
        _seekbarIsAdjusting = false;

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();


        _seekbar = (SeekBar) (getView().findViewById(R.id.seekBar));
        _playbutton = (ImageButton) (getView().findViewById(R.id.buttonPlayStop));
        _prevbutton = (ImageButton) (getView().findViewById(R.id.buttonPrev));
        _nextbutton = (ImageButton) (getView().findViewById(R.id.buttonNext));
        _timestamp = (TextView)(getView().findViewById(R.id.timestamp));
        
        _presenter = MediaPlayerPresenter.getInstance();
        _presenter.attachView(this);
        

        _seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar seekbar, int position, boolean fromUser)
            {
                if (fromUser)
                {
                    PlayControlFragment.this.updateTimestamp(seekbar.getMax(), position);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar)
            {
                _seekbarIsAdjusting = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekbar)
            {
                _seekbarIsAdjusting = false;
                int position = seekbar.getProgress();
                _presenter.onSeek(position);
            }
        });

        _playbutton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View arg0)
            {
                Integer tagint = (Integer)_playbutton.getTag(R.string.playpause_tag);
                
                if (tagint == null)
                {
                    Log.e(TAG, "No tag on the play/pause button!!!!");
                    return;
                }
                
                if (tagint.intValue() == R.drawable.play_button)
                {
                    _presenter.onPlay();
                }
                else
                {
                    _presenter.onPause();
                }
            }
        });

        _prevbutton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View arg0)
            {
                _presenter.onPrevTrack();
            }
        });

        _nextbutton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View arg0)
            {
                _presenter.onNextTrack();
            }
        });

    }

    @Override
    public void onStop()
    {
        super.onStop();

        if (_presenter != null)
        {
            _presenter.detachView(this);
        }
    }

    @Override
    public void setMainButtonState(MainButtonState state)
    {

        boolean enabled = ((state == MainButtonState.PauseButtonEnabled) || (state == MainButtonState.PlayButtonEnabled));
        ImageButton btn = (ImageButton) (getView().findViewById(R.id.buttonPlayStop));
        int resid = R.drawable.play_button;

        if ((state == MainButtonState.PauseButtonEnabled) || (state == MainButtonState.PauseButtonDisabled))
        {
            resid = R.drawable.pause_button;
        }
        
        btn.setEnabled(enabled);
        Drawable img = (Drawable)getResources().getDrawable(resid);
        btn.setImageDrawable(img);
        
        // hack - tag the button so we know whether it's in the play or pause state
        Integer taginteger = Integer.valueOf(resid);
        btn.setTag(R.string.playpause_tag, taginteger);
        
    }
    
    @SuppressLint("DefaultLocale")
    static String millisecondsToTime(int milliseconds)
    {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    void updateTimestamp(int duration, int position)
    {
        _timestamp.setText(millisecondsToTime(position) + " / " + millisecondsToTime(duration));
    }

    @Override
    public void setSeekBarEnabled(boolean enabled, int duration, int position, int secondaryPosition)
    {

        _seekbar.setEnabled(enabled);
        _seekbar.setMax(duration);
        _timestamp.setEnabled(enabled);
        
        // don't move the seekbar position if the user is actively moving it with his finger
        if (_seekbarIsAdjusting == false)
        {
            _seekbar.setProgress(position);
            _seekbar.setSecondaryProgress(secondaryPosition);
            updateTimestamp(duration, position);
        }
        
    }

    @Override
    public void setTrackButtonsEnabled(boolean prevEnabled, boolean nextEnabled)
    {
        _prevbutton.setEnabled(prevEnabled);
        _nextbutton.setEnabled(nextEnabled);
    }

    @Override
    public void setDisplayString(String message)
    {
        TextView tv = (TextView) getView().findViewById(R.id.tvNowPlaying);
        tv.setText(message);
    }

}
