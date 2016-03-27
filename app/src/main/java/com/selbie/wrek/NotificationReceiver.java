/*
   Copyright 2016 John Selbie

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String TAG = BroadcastReceiver.class.getSimpleName();

    public NotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        MediaPlayerPresenter presenter = MediaPlayerPresenter.getInstance();

        String action = intent.getAction();
        int eventcode = intent.getIntExtra(MediaPlayerService.INTENT_EXTRA_EVENTCODE, -1);
        Log.d(TAG, "onReceive: " + action + " (eventcode == " + eventcode + ")");

        if (intent.getAction().equals(MediaPlayerService.INTENT_BUTTON_CLICK)) {
            switch (eventcode) {
                case MediaPlayerService.EVENTCODE_STOP: {
                    Log.d(TAG, "about to call onPause");
                    presenter.onPause();
                    break;
                }
                case MediaPlayerService.EVENTCODE_PREV: {
                    Log.d(TAG, "about to call onPrevTrack");
                    presenter.onPrevTrack();
                    break;
                }
                case MediaPlayerService.EVENTCODE_NEXT: {
                    Log.d(TAG, "about to call onNextTrack");
                    presenter.onNextTrack();
                    break;
                }
            }
        }

    }
}