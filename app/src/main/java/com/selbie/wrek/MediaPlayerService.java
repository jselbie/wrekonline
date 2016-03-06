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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MediaPlayerService extends Service
{
    public final static String TAG = MediaPlayerService.class.getSimpleName();
    public final static String INTENT_EXTRA_SONGTITLE = "mps_songtitle";

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
    
    public static void startService(String songtitle)
    {
        Intent intent = new Intent(_applicationContext, MediaPlayerService.class);
        intent.putExtra(INTENT_EXTRA_SONGTITLE, songtitle);
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

    @TargetApi(21)
    Notification.Builder setColorOnBuilderWithReflection(Notification.Builder builder, int color)
    {
        Class [] paramInt = {Integer.TYPE};

        try {
            Method setColor = builder.getClass().getDeclaredMethod("setColor", paramInt);
            setColor.invoke(builder, color);
        } catch (ReflectiveOperationException e) {
            Log.d(TAG, "ReflectiveOperationException", e);
        }

        return builder;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void startForegroundHelper(String songtitle)
    {
        Notification notification = null;
        PendingIntent pendingIntent = null;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        
        String title = this.getResources().getString(R.string.app_name);
        //String subtext = this.getResources().getString(R.string.app_station_id);
        String subtext = (songtitle != null) ? songtitle : "";

        // for lollipop and above, we can use a large 150px icon and Android will scale it down for us
        int large_notification_resource_id = (android.os.Build.VERSION.SDK_INT >= 21) ? R.drawable.logo : R.drawable.notification_large;

        if (android.os.Build.VERSION.SDK_INT >= 16)
        {
            Notification.Builder builder = new Notification.Builder(this);

            builder = builder.setContentTitle(title).setContentText(subtext);
            builder = builder.setWhen(System.currentTimeMillis());
            builder = builder.setSmallIcon(R.drawable.notification);

            if (Build.VERSION.SDK_INT < 21) {
                builder = builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), large_notification_resource_id));
            }
            else  {
                // because we are still building with SDK 19, we can only invoke setColor via reflection
                // when we upgrade to a newer SDK, we can just say "builder = builder.setColor(Color.Black);"
                builder = setColorOnBuilderWithReflection(builder, Color.BLACK);
            }
            builder = builder.setContentIntent(pendingIntent);
            notification = builder.build();
        }
        else
        {
            notification = new Notification(R.drawable.notification, title, System.currentTimeMillis());
            notification.setLatestEventInfo(this, title, subtext, pendingIntent);
        }

        startForeground(1, notification);
    }

    void stopForegroundHelper()
    {
        stopForeground(true);
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

        startForegroundHelper(intent.getStringExtra(INTENT_EXTRA_SONGTITLE));

        // I think NOT_STICKY is the right flag to return here - basically means
        // "if the process is killed due to low system resource,don't bother starting it back up again"
        return START_NOT_STICKY;
    }
}
