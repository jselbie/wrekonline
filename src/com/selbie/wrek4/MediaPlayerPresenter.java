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

    MediaPlayer _player;
    PlayerState _state;

    ArrayList<String> _playlist; // list of tracks
    int _playlistIndex; // which track is "active"

    boolean _isLiveSource;
    MediaPlayerView _view;
    PeriodicTimer _timer;
    String _title;

    static MediaPlayerPresenter _staticInstance;

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

    void addCallbacksToPlayer()
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

    MediaPlayer createMediaPlayer()
    {
        return new MediaPlayer();
    }

    void destroyPlayer()
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

    void attachView(MediaPlayerView view)
    {
        // immediate turnaround and tell this view how to display itself
        _view = view;
        updateView();
    }

    void detachView()
    {
        stopSeekbarUpdateTimer();
        _view = null;
    }

    public void reset()
    {
        detachView();
        destroyPlayer();
    }

    boolean restartPlayer()
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

    boolean canIncrementPlayListIndex()
    {
        int next = _playlistIndex + 1;

        if ((next < 0) || (_playlist == null) || (next >= _playlist.size()))
        {
            return false;
        }
        return true;
    }

    boolean canDecrementPlayListIndex()
    {
        int prev = _playlistIndex - 1;

        if ((prev < 0) || (_playlist == null) || (prev >= _playlist.size()))
        {
            return false;
        }
        return true;
    }

    boolean incrementPlayListIndex()
    {
        boolean canIncrement = canIncrementPlayListIndex();
        if (canIncrement)
        {
            _playlistIndex++;
        }
        return canIncrement;
    }

    boolean decrementPlayListIndex()
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
    void onPrepared()
    {
        if (_state == PlayerState.Preparing)
        {
            _state = PlayerState.Started;
            _player.start();
            updateView();
        }
    }

    boolean onError(int what, int extra)
    {
        Log.e(TAG, "onError event.  what=" + what + " extra=" + extra);

        destroyPlayer();
        _state = PlayerState.Error;

        updateView();

        return true;
    }

    void onCompletion()
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

    void updateSeekbarView()
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

    String getDisplayMessage()
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

    void updateView()
    {
        MediaPlayerView.MainButtonState mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
        boolean seekBarEnabled = false;
        int duration = 0, position = 0;
        boolean isEmptyUrl = getActiveUrl().isEmpty();

        boolean trackButtonsEnabled = false;
        boolean prevButtonEnabled = false;
        boolean nextButtonEnabled = false;

        if (_view == null)
        {
            return;
        }

        switch (_state)
        {
        case Idle:
        {
            mainButtonState = isEmptyUrl ? MediaPlayerView.MainButtonState.PlayButtonDisabled : MediaPlayerView.MainButtonState.PlayButtonEnabled;
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
            break;
        }

        case Prepared:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PauseButtonEnabled;
            break;
        }

        case Started:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PauseButtonEnabled;
            seekBarEnabled = true;

            // for now, we'll only enable the prev/next buttons when we are
            // actually playing or paused (Consistent with seekbar)
            trackButtonsEnabled = true;

            break;
        }

        case Paused:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            seekBarEnabled = true;
            // for now, we'll only enable the prev/next buttons when we are
            // actually playing or paused (Consistent with seekbar)
            trackButtonsEnabled = true;
            break;
        }

        case Stopped:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            break;
        }

        case PlaybackComplete:
        {
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            seekBarEnabled = true;
            break;
        }

        case Error:
            mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
            break;

        default:
            break;
        }

        prevButtonEnabled = (_isLiveSource == false) && trackButtonsEnabled && canDecrementPlayListIndex();
        nextButtonEnabled = (_isLiveSource == false) && trackButtonsEnabled && canIncrementPlayListIndex();

        if (_view != null)
        {
            _view.SetMainButtonState(mainButtonState);
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
    }

    void stopSeekbarUpdateTimer()
    {
        if (_timer != null)
        {
            _timer.Stop();
            _timer = null;
        }
    }

    void startSeekbarUpdateTimer()
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
