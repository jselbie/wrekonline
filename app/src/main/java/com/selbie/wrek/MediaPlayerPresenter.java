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

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.CountDownTimer;
import android.util.Log;

import com.selbie.wrek.metaproxy.IMetadataCallback;
import com.selbie.wrek.metaproxy.IcecastMetadata;
import com.selbie.wrek.metaproxy.MetaStreamProxy;
import com.selbie.wrek.metaproxy.MetadataCallbackMarshaller;

public class MediaPlayerPresenter implements IMetadataCallback
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
    private boolean _isLiveSource;
    private boolean _hasIcyMetaInt;
    private int _playlistIndex; // which track is "active"

    private int _secondaryProgressPercent;
    private MediaPlayerView _view;
    private CountDownTimer _timer;
    private CountDownTimer _timerPauseSafety; // the pause safety timer
    private String _title;
    
    private IcecastMetadata _metadata;
    private MetadataCallbackMarshaller _metadataCallbackMarshaller;
    private MetaStreamProxy _metaproxy;


    private static MediaPlayerPresenter _staticInstance;

    public MediaPlayerPresenter()
    {
        _player = createMediaPlayer();
        _state = PlayerState.Idle;

        _playlist = new ArrayList<String>();
        _playlistIndex = 0;
        _secondaryProgressPercent = 0;

        _isLiveSource = false;
        _hasIcyMetaInt = false;
        _view = null;
        _timer = null;
        _timerPauseSafety = null;
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
        
        _player.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer player, int percent)
            {
                if (player == _player)
                {
                    MediaPlayerPresenter.this.onBufferingUpdate(percent);
                }
            }});
        

    }

    private MediaPlayer createMediaPlayer()
    {
        MediaPlayer player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        return player;
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
            _secondaryProgressPercent = 0;
        }
        
        _metadata = null;
        
        if (_metadataCallbackMarshaller != null)
        {
            _metadataCallbackMarshaller.dispose();
            _metadataCallbackMarshaller = null;
        }
        
        if (_metaproxy != null)
        {
            _metaproxy.stop();
            _metaproxy = null;
        }
        
    }

    public void attachView(MediaPlayerView view)
    {
        Log.d(TAG, "attachView");
        // immediately turnaround and tell this view how to display itself
        _view = view;
        updateView();
    }

    public void detachView(MediaPlayerView view)
    {
        Log.d(TAG, "detachView");
        
        if (_view == view)
        {
            _view = null;
            updateView(); // even though we don't have a view, updateView will take of start/stop the appropriate timers
        }
    }


    public void reset()
    {
        detachView(_view);
        destroyPlayer();
    }
    
    public boolean canProxyBeUsed()
    {
        Context context = MediaPlayerService.getContext();
        boolean result = SettingsFragment.isMetadataProxyEnabled(context);
        
        Log.d(TAG, "canProxyBeUsed() returns: " + result);
        return result;
    }

    private boolean restartPlayer()
    {
        int proxyport;
        boolean success = false;

        destroyPlayer();

        // create the player
        _player = createMediaPlayer();
        addCallbacksToPlayer();
        _state = PlayerState.Idle;
        String url = getActiveUrl();
        
        
        // create the proxy if this stream supports metadata (live sources only for the part)
        if (_hasIcyMetaInt && canProxyBeUsed())
        {
            this._metadataCallbackMarshaller = new MetadataCallbackMarshaller();
            this._metadataCallbackMarshaller.attach(this);
            
            Log.d(TAG, "Creating MetaStreamProxy instance");
            _metaproxy = MetaStreamProxy.createAndStart(_metadataCallbackMarshaller);
            
            if (_metaproxy == null)
            {
                Log.e(TAG, "failed to create metaproxy. No song titles for you.");
            }
            else
            {
                proxyport = _metaproxy.getPort();
                Log.d(TAG, "proxyport is " + proxyport);
                url = _metaproxy.formatUrl(url);
                Log.d(TAG, "meta proxy created.  Tunnel URL is: " + url);
            }
        }
        
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

    public boolean setPlaylist(String title, ArrayList<String> playlist, boolean isLiveSource, boolean hasIcyMetaInt)
    {
        _isLiveSource = isLiveSource;
        _hasIcyMetaInt = hasIcyMetaInt;
        _playlist = playlist; // weak reference copy. ScheduleItems and Streams
                              // shouldn't modify themselves after schedule is
                              // downloaded
        _playlistIndex = 0;
        
        if (title != null)
        {
            _title = title;
        }
        
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

    // startPlaying should only be called when the state is Prepared, Paused, or PlaybackComplete
    private void startPlayingAndUpdateView()
    {
        _state = PlayerState.Started;
        _player.start();
        updateView();
    }

    // -------------------------------------------------------------------------------
    // view callbacks

    public void onPlay()
    {

        if ((_state == PlayerState.Prepared) || (_state == PlayerState.Paused) || (_state == PlayerState.PlaybackComplete))
        {
            startPlayingAndUpdateView();
        }
        else if ((_state == PlayerState.Stopped) || (_state == PlayerState.Error) || (_state == PlayerState.Idle))
        {
            restartPlayer(); // restartPlayer calls updateView()
        }
    }

    public void onPause()
    {
        // The unfortunate thing about MediaPlayer.pause() is that it keeps streaming from the remote server at
        // some arbitrary rate that makes no sense. This was validated with DDMS and Wireshark. It appears to be
        // keeping the stream active for keep-alive purposes by buffing up a few hundred kilobytes every 15 seconds.
        // But it doesn't seem to ever stop. Perhaps it does stop on for a fixed length stream, but it seems
        // to go on forever for a live stream. The android process manager eventually stops the process
        // when the app goes go into the background (with the service stopped), but there's no guarantee.
        
        // So for now, we'll make pausing a live stream as a hard stop.
        
        // Non-live streams will stay paused for 5 minutes. This is a nice balance between consuming resources while still
        // providing some convenience of being able to toggle back to listening to music with no delay.
        
        if ((_state == PlayerState.Started) && (_isLiveSource == false))
        {
            _state = PlayerState.Paused;
            _player.pause();
            updateView();
        }
        else if ((_state == PlayerState.Preparing) || (_state == PlayerState.Prepared) || (_isLiveSource))
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
            Log.d(TAG, "onPrepared");

            startPlayingAndUpdateView();
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
            else if (_isLiveSource)
            {
                // The live streams should never end normally. But when they do, it likely means a transient network error
                // Simple approach is to just restart the player
                Log.d(TAG, "restarting for live source");
                restartPlayer();
            }
            else
            {
                _playlistIndex = 0;
            }

            updateView();
        }
    }

    private void onBufferingUpdate(int percent)
    {
        // As per Android documentation:
        //   Called to update status in buffering a media stream received through progressive HTTP download.
        //   The received buffering percentage indicates how much of the content has been buffered or played.
        //   For example a buffering update of 80 percent when half the content has already been played indicates
        //   that the next 30 percent of the content to play has been buffered.
        
        if (percent < 0)
        {
            // percent can be something awkward like "-2147483648" on a live source. Just clamp it.
            percent = 0;
        }
        else if (percent > 100)
        {
            percent = 100;
        }

        // For some reason with the WREK streams, there is an early
        // call of onBufferingUpdate(100) prior to onPrepared.  This causes the displayed buffering line on the seek control
        // to become a solid line for about 1 second after the start of the stream.  The workaround is
        // to just ignore onBufferingUpdate while in the Preparing state
        if (_state == PlayerState.Preparing)
        {
            Log.d(TAG, "onBufferingUpdate - ignoring value while in Preparing state. (percent==" + percent + ")");
        }
        else if (_secondaryProgressPercent != percent)
        {
            Log.d(TAG, "onBufferingUpdate - " + percent);
            _secondaryProgressPercent = percent;
        }
    }

    // -------------------------------------------------------------------------------

    private void updateSeekbarView()
    {

        boolean seekBarEnabled = ((_state == PlayerState.Started) || (_state == PlayerState.Paused) || (_state == PlayerState.PlaybackComplete));
        int duration = 0, position = 0, secondaryProgress = 0;

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
                secondaryProgress = (duration * _secondaryProgressPercent) / 100;
            }
        }

        if (_view != null)
        {
            _view.setSeekBarEnabled(seekBarEnabled, duration, position, secondaryProgress);
        }
    }
    
    
    private String getDisplayTitle()
    {
        String message = "";
        String prefix = "";
        String postfix = "";
        boolean addPostfix = false;
        String songtitle = getSongTitle();

        if ((_state == PlayerState.Started) || (_state == PlayerState.Paused))
        {
            prefix = "Now Playing: ";
            message = songtitle.isEmpty() ? _title : songtitle;
            addPostfix = true; // in theory, we shouldn't have a song title for non-live streams
        }
        else if (_state == PlayerState.Preparing)
        {
            prefix = "Connecting...";
        }
        else if (_state == PlayerState.Error)
        {
            prefix = "Error";
        }
        else if ((_state == PlayerState.Idle) && (_title != null) && !_title.isEmpty())
        {
            prefix = "Selected: ";
            message = _title; // show only the show title if we wind up stopped, but with a playlist
            addPostfix = true;
        }

        if (addPostfix && (_playlist.size() > 1))
        {
            postfix = " (" + (_playlistIndex + 1) + " of " + _playlist.size() + ")";
        }
        

        return prefix + message + postfix;
    }
    
    private String getSongTitle()
    {
        String message = "";
        
        if (_state == PlayerState.Started)
        {
            if (_metadata != null)
            {
                message = _metadata.getStreamTitle();
            }
        }
        
        return message;
    }

    private void updateView()
    {
        MediaPlayerView.MainButtonState mainButtonState = MediaPlayerView.MainButtonState.PlayButtonEnabled;
        boolean seekBarEnabled = false;
        boolean isEmptyUrl = getActiveUrl().isEmpty();

        boolean trackButtonsEnabled = false;
        boolean prevButtonEnabled = false;
        boolean nextButtonEnabled = false;
        
        boolean startService = false;
        boolean stopService = false;
        
        boolean needPauseTimer = false;
        
        // even if _view is null, this method will take care of turning on/off the timers as appropriate 

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
            needPauseTimer = true;
            
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
            _view.setTrackButtonsEnabled(prevButtonEnabled, nextButtonEnabled);
            _view.setTitle(getDisplayTitle());
            updateSeekbarView();
        }

        // if the seekbar is enabled (and we have a view), make sure the timer is started
        if (seekBarEnabled && (_view != null))
        {
            startSeekbarUpdateTimer();
        }
        else
        {
            stopSeekbarUpdateTimer();
        }
        
        if (stopService == true)
        {
            MediaPlayerService.stopService();
        }
        else if (startService == true)
        {
            String title = _title;
            String songtitle = getSongTitle();
            
            if (songtitle.isEmpty() == false)
            {
                title = songtitle;
            }
            
            // "startService" is overloaded to also update the subtitle field of the notification area with the current song title
            Log.d(TAG, "About to call MediaPlayerService.startService");
            MediaPlayerService.startService(title);
            Log.d(TAG, "Return from MediaPlayerService.startService");
        }
        
        if (needPauseTimer)
        {
            startPauseSafetyTimer();
        }
        else
        {
            stopPauseSafetyTimer();
        }
        
    }

    private void stopSeekbarUpdateTimer()
    {
        if (_timer != null)
        {
            _timer.cancel();
            _timer = null;
        }
    }

    private void startSeekbarUpdateTimer()
    {

        if (_timer != null)
        {
            return;
        }
        
        long forever = Long.MAX_VALUE;
        assert(forever > 0);
        long onesecond = 1000;
        
        assert(_timer == null);
        _timer = new CountDownTimer(forever, onesecond) {
            
            boolean _gotFirstCallback = false;

            @Override
            public void onTick(long millisUntilFinished)
            {
                if (_timer != this)
                {
                    Log.wtf(TAG, "weird. onTick was called, but _timer is either null or not this instance");
                    return;
                }
                
                if (_gotFirstCallback == false)
                {
                    _gotFirstCallback = true;
                    Log.d(TAG, "onTimerCallback (Seekbar) - got first callback");
                }
                
                // do the callback
                updateSeekbarView();
            }

            @Override
            public void onFinish()
            {
                // this should never happen for a million years and some more
                Log.wtf(TAG, "unexpected end to repeating timer");
                _timer = null;
            }
        };
        
        _timer.start();
    }


    private void startPauseSafetyTimer()
    {
        // About the pause safety timer.
        //
        // When the stream is "paused" by the user, the Android MediaPlayer will still drizzle download bits from the server if its not already completed
        // Hence, the user could pause a stream and exit the activity. Android should eventually kill the process (since the service is stopped), but there's no
        // guarantee. So the pause timer will wake up 5 minutes later and make sure the MediaPlayer is shutdown
        // If the user returns to the app within 5 minutes, the pause timer can be reset by unpausing
        //
        // Ideally, when the app goes into the background, we just stop any paused stream immediately, but remember its URL and progress
        // such that when the player is restarted, the stream picks up near where it left off before
        
        if (_timerPauseSafety != null)
        {
            return;
        }
        
        Log.d(TAG, "pause timer - started");
        
        int pausetimeout = 5 * 60 * 1000; // 5 minutes
        assert(_timerPauseSafety == null);
        _timerPauseSafety = new CountDownTimer(pausetimeout, pausetimeout) {

                @Override
                public void onFinish()
                {
                    Log.d(TAG, "CountDownTimer.onFinish called");
                    _timerPauseSafety = null;
                    onPauseSafetyTimerCallback();
                }
    
                @Override public void onTick(long millisUntilFinished) {}
                
            };
            
        _timerPauseSafety.start();
        
    }

    private void stopPauseSafetyTimer()
    {
        if (_timerPauseSafety != null)
        {
            Log.d(TAG, "pause timer - stopped");
            _timerPauseSafety.cancel();
            _timerPauseSafety = null;
        }
    }

    private void onPauseSafetyTimerCallback()
    {
        if (_state == PlayerState.Paused)
        {
            Log.d(TAG, "onPauseSafetyTimerCallback - stopping player because we've been paused for too long");
            destroyPlayer();
            updateView(); // this will call stopPauseSafetyTimer above. This is ok, since _timerPauseSafety should be null before this method is called
        }
        else
        {
            Log.w(TAG, "onPauseSafetyTimerCallback - we aren't in the paused state - nothing to do!");
        }
    }

    @Override
    public void onNewMetadataAvailable(String metadata)
    {
        Log.d(TAG, "onNewMetadataAvailable: " + metadata);
        
        _metadata = new IcecastMetadata(metadata);
        
        // updateView will call MediaPlayerService.startService again with the updated song title
        // see the notes in MediaPlayerService.onStartCommand
        this.updateView();
    }

    
}
