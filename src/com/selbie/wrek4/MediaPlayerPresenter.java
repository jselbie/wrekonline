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

import java.io.IOException;
import java.util.ArrayList;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;

public class MediaPlayerPresenter
{
    public static final String TAG = MediaPlayerPresenter.class.getSimpleName();

    public enum PlayerState
    {
        Idle,        // instantiated, but no content url set
        Initialized, // setDataSource has been called (successfully)
        Preparing,   // prepareAsync called
        Prepared,    // onPrepared event
        Started,     // start() called when state==Prepared, Paused, or PlaybackComplete
        Stopped,     // stop() called near logically similar to Initialized state
        Paused,      // pause() called
        PlaybackComplete, // onPlaybackCompleted event
        Error        // onError event
    }

    // public enum Events {
    // Player_Prepared, Player_Completed, Player_Error, View_Play, View_Pause,
    // View_Seek
    // }

    private MediaPlayer _player;
    private PlayerState _state;

    private ArrayList<String> _playlist; // list of tracks
    private int _playlistIndex; // which track is "active"

    private boolean _isLiveSource;
    private MediaPlayerView _view;
    private PeriodicTimer _timer;
    private String _title;

    private static MediaPlayerPresenter _staticInstance;

    public MediaPlayerPresenter()
    {
        _player = createMediaPlayer();
        _state = PlayerState.Idle;

        _playlist = new ArrayList<String>();
        _playlistIndex = 0;

        _isLiveSource = false;
        _view = null;
        _timer = null;
        _title = "";
    }

    static public MediaPlayerPresenter getInstance()
    {
        if (_staticInstance == null)
        {
            _staticInstance = new MediaPlayerPresenter();
        }
        return _staticInstance;
    }

    private void addCallbacksToPlayer()
    {
        _player.setOnPreparedListener(new OnPreparedListener()
        {

            @Override
            public void onPrepared(MediaPlayer player)
            {
                if (player == _player)
                {
                    MediaPlayerPresenter.this.onPrepared();
                }
            }
        });

        _player.setOnCompletionListener(new OnCompletionListener()
        {

            @Override
            public void onCompletion(MediaPlayer player)
            {
                if (player == _player)
                {
                    MediaPlayerPresenter.this.onCompletion();
                }
            }
        });

        _player.setOnErrorListener(new OnErrorListener()
        {

            @Override
            public boolean onError(MediaPlayer player, int what, int extra)
            {
                if (player == _player)
                {
                    return MediaPlayerPresenter.this.onError(what, extra);
                }
                else
                {
                    return false;
                }
            }
        });
        

    }

    private MediaPlayer createMediaPlayer()
    {
        return new MediaPlayer();
    }

    private void destroyPlayer()
    {
        if (_player != null)
        {
            _state = PlayerState.Idle;
            MediaPlayer player = _player;
            _player = null;
            player.reset();
            player.release();
            player = null;
        }
    }

    public void attachView(MediaPlayerView view)
    {
        Log.d(TAG, "attachView");
        // immediate turnaround and tell this view how to display itself
        _view = view;
        updateView();
    }

    public void detachView(MediaPlayerView view)
    {
        Log.d(TAG, "detachView");
        
        if (_view == view)
        {
            stopSeekbarUpdateTimer();
            _view = null;
        }
    }
    

    public void reset()
    {
        detachView(_view);
        destroyPlayer();
    }

    private boolean restartPlayer()
    {
        boolean success = false;

        destroyPlayer();

        _player = createMediaPlayer();
        addCallbacksToPlayer();
        _state = PlayerState.Idle;
        String url = getActiveUrl();

        if (url.isEmpty() == false)
        {
            try
            {
                _player.setDataSource(url);
                success = true;
            }
            catch (IllegalArgumentException e)
            {
                Log.e(TAG, "setDataSource threw an IllegalArgumentException", e);
            }
            catch (SecurityException e)
            {
                Log.e(TAG, "setDataSource threw a SecurityException", e);
            }
            catch (IllegalStateException e)
            {
                Log.e(TAG, "setDataSource threw an IllegalStateException", e);
            }
            catch (IOException e)
            {
                Log.e(TAG, "setDataSource threw an IOException", e);
            }

            if (success)
            {
                _state = PlayerState.Preparing;
                _player.prepareAsync();
            }
            else
            {
                _state = PlayerState.Error;
            }

        }

        updateView();

        return success;
    }

    private String getActiveUrl()
    {
        String url = "";

        if ((_playlist != null) && (_playlistIndex >= 0) && (_playlistIndex < _playlist.size()))
        {
            url = _playlist.get(_playlistIndex);
        }

        return url;
    }

    public boolean setPlaylist(String title, ArrayList<String> playlist, boolean isLiveSource)
    {
        _isLiveSource = isLiveSource;
        _playlist = playlist; // weak reference copy. ScheduleItems and Streams
                              // shouldn't modify themselves after schedule is
                              // downloaded
        _playlistIndex = 0;
        _title = title;
        return restartPlayer();
    }

    private boolean canIncrementPlayListIndex()
    {
        int next = _playlistIndex + 1;

        if ((next < 0) || (_playlist == null) || (next >= _playlist.size()))
        {
            return false;
        }
        return true;
    }

    private boolean canDecrementPlayListIndex()
    {
        int prev = _playlistIndex - 1;

        if ((prev < 0) || (_playlist == null) || (prev >= _playlist.size()))
        {
            return false;
        }
        return true;
    }

    private boolean incrementPlayListIndex()
    {
        boolean canIncrement = canIncrementPlayListIndex();
        if (canIncrement)
        {
            _playlistIndex++;
        }
        return canIncrement;
    }

    private boolean decrementPlayListIndex()
    {
        boolean canDecrement = canDecrementPlayListIndex();
        if (canDecrement)
        {
            _playlistIndex--;
        }
        return canDecrement;
    }

    public boolean isPlaying()
    {
        boolean result = ((_player != null) && (_state == PlayerState.Started));
        return result;
    }

    // -------------------------------------------------------------------------------
    // view callbacks

    public void onPlay()
    {

        if ((_state == PlayerState.Prepared) || (_state == PlayerState.Paused) || (_state == PlayerState.PlaybackComplete))
        {
            _state = PlayerState.Started;
            _player.start();
            updateView();
        }
        else if ((_state == PlayerState.Stopped) || (_state == PlayerState.Error) || (_state == PlayerState.Idle))
        {
            restartPlayer(); // restartPlayer calls updateView()
        }

    }

    public void onPause()
    {
        if (_state == PlayerState.Started)
        {
            _state = PlayerState.Paused;
            _player.pause();
            updateView();
        }
        else if ((_state == PlayerState.Preparing) || (_state == PlayerState.Prepared))
        {
            destroyPlayer();
            updateView();
        }

    }

    public void onSeek(int position)
    {
        if ((_state == PlayerState.Paused) || (_state == PlayerState.Started) || (_state == PlayerState.PlaybackComplete))
        {
            _player.seekTo(position);
        }
    }

    public void onNextTrack()
    {

        if (canIncrementPlayListIndex())
        {
            incrementPlayListIndex();
            restartPlayer(); // restartPlayer calls updateView()
        }
    }

    public void onPrevTrack()
    {

        if (canDecrementPlayListIndex())
        {
            decrementPlayListIndex();
            restartPlayer(); // restartPlayer calls updateView()
        }
    }

    // -------------------------------------------------------------------------------

    // player callbacks
    private void onPrepared()
    {
        if (_state == PlayerState.Preparing)
        {
            _state = PlayerState.Started;
            _player.start();
            updateView();
        }
    }

    private boolean onError(int what, int extra)
    {
        Log.e(TAG, "onError event.  what=" + what + " extra=" + extra);

        destroyPlayer();
        _state = PlayerState.Error;

        updateView();

        return true;
    }

    private void onCompletion()
    {
        Log.d(TAG, "onCompletion event");

        if (_state == PlayerState.Started)
        {
            _state = PlayerState.PlaybackComplete;

            if (canIncrementPlayListIndex())
            {
                incrementPlayListIndex();
                restartPlayer();
            }
            else
            {
                _playlistIndex = 0;
            }

            updateView();
        }
    }

    // -------------------------------------------------------------------------------

    private void updateSeekbarView()
    {

        boolean seekBarEnabled = ((_state == PlayerState.Started) || (_state == PlayerState.Paused) || (_state == PlayerState.PlaybackComplete));
        int duration = 0, position = 0;

        if (seekBarEnabled)
        {
            if (_isLiveSource || (_player == null) || (_player.getDuration() == 0))
            {
                seekBarEnabled = false;
            }
            else
            {
                duration = _player.getDuration();
                position = _player.getCurrentPosition();
            }
        }

        if (_view != null)
        {
            _view.setSeekBarEnabled(seekBarEnabled, duration, position);
        }
    }

    private String getDisplayMessage()
    {
        String message = "";
        String postfix = "";
        boolean addPostfix = false;

        if ((_state == PlayerState.Started) || (_state == PlayerState.Paused))
        {

            message = "Now Playing: " + _title;
            addPostfix = true;
        }
        else if (_state == PlayerState.Preparing)
        {
            message = "Connecting...";
        }
        else if (_state == PlayerState.Error)
        {
            message = "Error";
        }

        if (addPostfix && (_playlist.size() > 1))
        {
            postfix = " (" + (_playlistIndex + 1) + " of " + _playlist.size() + ")";
        }

        return message + postfix;
    }

    private void updateView()
    {
        MediaPlayerView.MainButtonState mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
        boolean seekBarEnabled = false;
        int duration = 0, position = 0;
        boolean isEmptyUrl = getActiveUrl().isEmpty();

        boolean trackButtonsEnabled = false;
        boolean prevButtonEnabled = false;
        boolean nextButtonEnabled = false;
        
        boolean startService = false;
        boolean stopService = false;
        

        if (_view == null)
        {
            return;
        }

        switch (_state)
        {
        case Idle:
        {
            mainButtonState = isEmptyUrl ? MediaPlayerView.MainButtonState.PlayButtonDisabled : MediaPlayerView.MainButtonState.PlayButtonEnabled;
            stopService = true;
            break;
        }

        case Initialized:
        {
            mainButtonState = isEmptyUrl ? MediaPlayerView.MainButtonState.PlayButtonDisabled : MediaPlayerView.MainButtonState.PlayButtonEnabled;
            break;
        }

        case Preparing:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PauseButtonEnabled;
            startService = true;
            break;
        }

        case Prepared:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PauseButtonEnabled;
            startService = true;
            break;
        }

        case Started:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PauseButtonEnabled;
            seekBarEnabled = true;

            // for now, we'll only enable the prev/next buttons when we are
            // actually playing or paused (Consistent with seekbar)
            trackButtonsEnabled = true;
            startService = true;

            break;
        }

        case Paused:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            seekBarEnabled = true;
            // for now, we'll only enable the prev/next buttons when we are
            // actually playing or paused (Consistent with seekbar)
            trackButtonsEnabled = true;
            stopService = true;
            
            break;
        }

        case Stopped:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            stopService = true;
            break;
        }

        case PlaybackComplete:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            seekBarEnabled = true;
            stopService = true;
            break;
        }

        case Error:
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            stopService = true;
            break;

        default:
            break;
        }

        prevButtonEnabled = (_isLiveSource == false) && trackButtonsEnabled && canDecrementPlayListIndex();
        nextButtonEnabled = (_isLiveSource == false) && trackButtonsEnabled && canIncrementPlayListIndex();

        if (_view != null)
        {
            _view.setMainButtonState(mainButtonState);
            _view.setSeekBarEnabled(seekBarEnabled, duration, position);
            _view.setTrackButtonsEnabled(prevButtonEnabled, nextButtonEnabled);
            _view.setDisplayString(getDisplayMessage());
        }

        updateSeekbarView();

        // if the seekbar is enabled, make sure the timer is started
        if (seekBarEnabled)
        {
            startSeekbarUpdateTimer();
        }
        else
        {
            stopSeekbarUpdateTimer();
        }
        
        if (stopService == true)
        {
            MediaPlayerService.StopService();
        }
        else if (startService == true)
        {
            MediaPlayerService.StartService();
        }
        
    }

    private void stopSeekbarUpdateTimer()
    {
        if (_timer != null)
        {
            _timer.Stop();
            _timer = null;
        }
    }

    private void startSeekbarUpdateTimer()
    {

        if ((_timer != null) && _timer.isStarted())
        {
            return;
        }

        PeriodicTimer.PeriodicTimerCallback callback = new PeriodicTimer.PeriodicTimerCallback()
        {

            private boolean _gotFirstCallback = false;

            @Override
            public void onTimerCallback(Object token)
            {

                if (_gotFirstCallback == false)
                {
                    _gotFirstCallback = true;
                    Log.d(TAG, "onTimerCallback (Seekbar) - got first callback");
                }
                updateSeekbarView();

            }
        };

        _timer = new PeriodicTimer(callback, 1000, false, null);
        _timer.Start();
    }

}
