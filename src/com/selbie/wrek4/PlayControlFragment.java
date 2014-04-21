package com.selbie.wrek4;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PlayControlFragment extends Fragment implements MediaPlayerView
{

    private MediaPlayerPresenter _presenter;
    private SeekBar _seekbar;
    private Button _playbutton;
    private Button _nextbutton;
    private Button _prevbutton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.media_player, parent, false);

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        _presenter = MediaPlayerPresenter.getInstance();
        _presenter.attachView(this);

        _seekbar = (SeekBar) (getView().findViewById(R.id.seekBar));
        _playbutton = (Button) (getView().findViewById(R.id.buttonPlayStop));
        _prevbutton = (Button) (getView().findViewById(R.id.buttonPrev));
        _nextbutton = (Button) (getView().findViewById(R.id.buttonNext));

        _seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar arg0, int position, boolean fromUser)
            {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekbar)
            {
                int position = seekbar.getProgress();
                _presenter.onSeek(position);
            }
        });

        _playbutton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View arg0)
            {
                String strPlay = getActivity().getResources().getString(R.string.play);
                String currentButtonText = _playbutton.getText().toString();

                if (currentButtonText.equals(strPlay))
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
            _presenter.detachView();
        }
    }

    @Override
    public void SetMainButtonState(MainButtonState state)
    {

        boolean enabled = ((state == MainButtonState.PauseButtonEnabled) || (state == MainButtonState.PlayButtonEnabled));
        Button btn = (Button) (getView().findViewById(R.id.buttonPlayStop));
        btn.setEnabled(enabled);

        if ((state == MainButtonState.PauseButtonEnabled) || (state == MainButtonState.PauseButtonDisabled))
        {
            btn.setText(R.string.pause);
        }
        else
        {
            btn.setText(R.string.play);
        }
    }

    @Override
    public void setSeekBarEnabled(boolean enabled, int duration, int position)
    {
        // TODO Auto-generated method stub

        SeekBar seekbar = (SeekBar) (getView().findViewById(R.id.seekBar));
        seekbar.setEnabled(enabled);
        seekbar.setMax(duration);
        seekbar.setProgress(position);
    }

    @Override
    public void setTrackButtonsEnabled(boolean prevEnabled, boolean nextEnabled)
    {
        // TODO Auto-generated method stub

        Button btn = (Button) (getView().findViewById(R.id.buttonPrev));
        btn.setEnabled(prevEnabled);

        btn = (Button) (getView().findViewById(R.id.buttonNext));
        btn.setEnabled(nextEnabled);
    }

    @Override
    public void setDisplayString(String message)
    {
        // TODO Auto-generated method stub

        TextView tv = (TextView) getView().findViewById(R.id.tvNowPlaying);
        tv.setText(message);
    }

}
