package com.selbie.wrek4;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;

public class MediaPlayerService extends Service
{
    MediaPlayerPresenter _presenter;
    
    
    public static void StartService(Context context)
    {
        Intent intent = new Intent(context, MediaPlayerService.class);
        context.startService(intent);
    }
    
    public static void StopService(Context context)
    {
        Intent intent = new Intent(context, MediaPlayerService.class);
        context.stopService(intent);
    }
    
    
    public final static String TAG = MediaPlayerService.class.getSimpleName();

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void startForegroundHelper()
    {
        Notification notification = null;
        PendingIntent pendingIntent = null;
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        
        if (android.os.Build.VERSION.SDK_INT >= 16)
        {
            Notification.Builder builder = new Notification.Builder(this);
            
            String title = this.getResources().getString(R.string.app_name);
            String subtext = this.getResources().getString(R.string.app_station_id);
            
            builder = builder.setContentTitle(title).setContentText(subtext);
            builder = builder.setWhen(System.currentTimeMillis());
            builder = builder.setSmallIcon(R.drawable.logo);
            builder = builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
            builder = builder.setContentIntent(pendingIntent);
            notification = builder.build();
        }
        else
        {
            notification = new Notification(R.drawable.logo, getResources().getString(R.string.app_name), System.currentTimeMillis());
            notification.setLatestEventInfo(this, getResources().getString(R.string.app_name), getResources().getString(R.string.app_station_id), pendingIntent);
        }
        
        startForeground(1, notification);
    }
    
    void stopForegroundHelper()
    {
        stopForeground(true);
    }
    
    
    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        if (_presenter == null)
        {
            _presenter = MediaPlayerPresenter.getInstance();
        }
        
        startForegroundHelper();
        
        // I think NOT_STICKY is the right flag to return here - basically means "if the process is killed due to low system resource,don't bother starting it back up again"
        return START_NOT_STICKY;
    }
    


}
