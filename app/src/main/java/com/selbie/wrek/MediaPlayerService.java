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


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MediaPlayerService extends Service
{
    public final static String TAG = MediaPlayerService.class.getSimpleName();
    public final static String INTENT_EXTRA_SONGTITLE =   "com.selbie.wrek.songtitle";
    public final static String INTENT_EXTRA_ISLIVE =      "com.selbie.wrek.islive";
    public final static String INTENT_EXTRA_ENABLE_PREV = "com.selbie.wrek.enable.prev";
    public final static String INTENT_EXTRA_ENABLE_NEXT = "com.selbie.wrek.enable.next";
    public final static String INTENT_BUTTON_CLICK =      "com.selbie.wrek.notification";
    public final static String INTENT_EXTRA_EVENTCODE =   "com.selbie.wrek.eventcode";

    public final static int EVENTCODE_PREV = 22;
    public final static int EVENTCODE_NEXT = 33;
    public final static int EVENTCODE_STOP = 44;

    MediaPlayerPresenter _presenter;
    ScheduleFetcher _fetcher;
    
    static Context _applicationContext;   // this needs to be the Application context, otherwise it's a leak
    
    public static void setApplicationContext(Context context)
    {
        _applicationContext = context;
    }
    
    public static Context getContext()
    {
        return _applicationContext;
    }
    
    public static void startService(String songTitle, boolean isLive, boolean enablePrev, boolean enableNext)
    {
        Intent intent = new Intent(_applicationContext, MediaPlayerService.class);
        intent.putExtra(INTENT_EXTRA_SONGTITLE, songTitle);
        intent.putExtra(INTENT_EXTRA_ISLIVE, isLive);
        intent.putExtra(INTENT_EXTRA_ENABLE_PREV, enablePrev);
        intent.putExtra(INTENT_EXTRA_ENABLE_NEXT, enableNext);
        _applicationContext.startService(intent);
    }

    public static void stopService()
    {
        Log.d(TAG, "stopService");

        Intent intent = new Intent(_applicationContext, MediaPlayerService.class);
        _applicationContext.stopService(intent);

    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }


    PendingIntent createPendingIntentForNotificationButton(int eventCode)
    {
        Intent buttonClickIntent = new Intent(INTENT_BUTTON_CLICK);

        buttonClickIntent.putExtra(INTENT_EXTRA_EVENTCODE, eventCode);

        // must pass a unique requestCode, otherwise the previously created PendingIntent is returned
        return PendingIntent.getBroadcast(this, eventCode, buttonClickIntent, Intent.FILL_IN_DATA);
    }

    @TargetApi(21)
    void setColorOnBuilderWithReflection(Notification.Builder builder, int color)
    {
        // Since we are still compiling on SDK 19, we're using reflection to get to the Color API

        try {
            Class [] paramInt = {Integer.TYPE};
            Method setColor = builder.getClass().getDeclaredMethod("setColor", paramInt);
            if (setColor != null) {
                setColor.invoke(builder, color);
            }
        } catch (NoSuchMethodException e) {
           Log.d(TAG, "NoSuchMethodException", e);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "IllegalAccessException", e);
        } catch (InvocationTargetException e) {
            Log.d(TAG, "InvocationTargetException", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "IllegalArgumentException", e);
        }
    }

    PendingIntent createPendingIntent() {
        PendingIntent pendingIntent;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        return pendingIntent;
    }

    @SuppressWarnings("deprecation")
    Notification createNotification(String songtitle) {

        Notification notification;
        PendingIntent pendingIntent = createPendingIntent();

        String title = this.getResources().getString(R.string.app_name);
        String subtext = (songtitle != null) ? songtitle : "";

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(title);
        builder.setContentText(subtext);
        builder.setSmallIcon(R.drawable.notification);
        if (Build.VERSION.SDK_INT < 21) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.notification_large);
            builder.setLargeIcon(bmp);
        } else {
            // on Lollipop and on up, just use a small icon, but surround it with a dark grey circle
            // the setColor API is available on on API 21 and up
            int darkgrey = Color.argb(0xff, 0x32, 0x30, 0x31);
            setColorOnBuilderWithReflection(builder, darkgrey);
        }
        builder.setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        }
        else {
            // for Android 14/15
            notification = builder.getNotification();
        }
        return notification;
    }

    @SuppressWarnings("deprecation")
    Notification createModernNotification(String songTitle, boolean isLive, boolean enablePrev, boolean enableNext) {

        // convert songTitle into two lines (artist and song)
        songTitle = songTitle.replaceFirst(" - ", "\n"); // convert into two lines

        Notification notification;
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);

        remoteViews.setTextViewText(R.id.notification_songtitle, songTitle);

        remoteViews.setBoolean(R.id.notification_button_prev, "setEnabled", enablePrev);
        remoteViews.setBoolean(R.id.notification_button_next, "setEnabled", enableNext);

        // For the live streams (where previous and next are both disabled, just hide them both)
        // This gives more room for the song title to be shown without getting truncated
        remoteViews.setViewVisibility(R.id.notification_button_prev, isLive ? View.GONE : View.VISIBLE);
        remoteViews.setViewVisibility(R.id.notification_button_next, isLive ? View.GONE : View.VISIBLE);

        // Set an intent on the buttons even if we don't need it.  Because API 14/15 crash hard on a null pendingIntent
        // http://stackoverflow.com/a/19240425/104458
        PendingIntent buttonIntentPrev = createPendingIntentForNotificationButton(EVENTCODE_PREV);
        PendingIntent buttonIntentNext = createPendingIntentForNotificationButton(EVENTCODE_NEXT);
        remoteViews.setOnClickPendingIntent(R.id.notification_button_prev, buttonIntentPrev);
        remoteViews.setOnClickPendingIntent(R.id.notification_button_next, buttonIntentNext);

        remoteViews.setOnClickPendingIntent(R.id.notification_button_cancel, createPendingIntentForNotificationButton(EVENTCODE_STOP));

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContent(remoteViews);
        builder.setContentIntent(createPendingIntent());
        builder.setSmallIcon(R.drawable.notification);

        if (Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        }
        else {
            // for Android 14/15
            notification = builder.getNotification();
        }

        return notification;
    }


    private void startForegroundHelper(String songTitle, boolean isLive, boolean enablePrev, boolean enableNext)
    {
        Notification notification;

        if (Build.VERSION.SDK_INT < 21) {
            // The modern notification crashes on API 15 when startForeground is called
            // Even though we have fixed it above, it's not worth the risk to ship
            // See http://stackoverflow.com/a/19240425/104458 for more details

            // Then plan was always to create the modern notification to have an expanded view
            // on Lollipop/Marshmallow.  So even though we could allow the modern notification
            // on (SDK_INT >= 16), we'll limit it to just to the newer platforms (>= 21)

            notification = createNotification(songTitle);
        }
        else {
            notification = createModernNotification(songTitle, isLive, enablePrev, enableNext);
        }

        Log.d(TAG, "notification: " + notification.toString());

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand (flags=" + Integer.toHexString(flags) + ")");
        
        // onStartCommand is overloaded to be used as an "update notification with song title" method
        // onStartCommand will get called even when MediaPlayerPresenter calls startService - even if the service is already running
        // This allows us to avoid doing the whole "onBind" thing, which is a little extra code for accomplishing what we need
        // http://stackoverflow.com/questions/5528288/how-do-i-update-the-notification-text-for-a-foreground-service-in-android

        
        if (_presenter == null)
        {
            // persist a few of our important singletons within the server so
            // they don't get garbage collected out
            _presenter = MediaPlayerPresenter.getInstance();
            _fetcher = ScheduleFetcher.getInstance();
        }


        startForegroundHelper(intent.getStringExtra(INTENT_EXTRA_SONGTITLE), intent.getBooleanExtra(INTENT_EXTRA_ISLIVE, false), intent.getBooleanExtra(INTENT_EXTRA_ENABLE_PREV, false), intent.getBooleanExtra(INTENT_EXTRA_ENABLE_NEXT, false));

        // I think NOT_STICKY is the right flag to return here - basically means
        // "if the process is killed due to low system resource,don't bother starting it back up again"
        return START_NOT_STICKY;
    }
}
