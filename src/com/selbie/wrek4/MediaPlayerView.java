package com.selbie.wrek4;

public interface MediaPlayerView
{

    public enum MainButtonState
    {
        PlayButtonEnabled, PauseButtonEnabled, PlayButtonDisabled, PauseButtonDisabled
    }

    void setMainButtonState(MainButtonState state);

    void setSeekBarEnabled(boolean enabled, int duration, int position);

    void setTrackButtonsEnabled(boolean prevEnabled, boolean nextEnabled);

    void setDisplayString(String message);
}
