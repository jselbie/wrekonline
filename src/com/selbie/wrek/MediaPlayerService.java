package com.selbie.wrek;

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
    public final static String TAG = MediaPlayerService.class.getSimpleName();

    MediaPlayerPresenter _presenter;
    ScheduleFetcher _fetcher;
    
    static Context _context;
    
    public static void setContext(Context context)
    {
        _context = context;
    }
    
    public static Context getContext()
    {
        return _context;
    }
    
    public static void startService()
    {
        Intent intent = new Intent(_context, MediaPlayerService.class);
        _context.startService(intent);
    }

    public static void stopService()
    {
        Intent intent = new Intent(_context, MediaPlayerService.class);
        _context.stopService(intent);
        
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
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
        
        String title = this.getResources().getString(R.string.app_name);
        //String subtext = this.getResources().getString(R.string.app_station_id);
        String subtext = "";

        if (android.os.Build.VERSION.SDK_INT >= 16)
        {
            Notification.Builder builder = new Notification.Builder(this);

            builder = builder.setContentTitle(title).setContentText(subtext);
            builder = builder.setWhen(System.currentTimeMillis());
            builder = builder.setSmallIcon(R.drawable.notification);
            builder = builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification_large));
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
        if (_presenter == null)
        {
            // persist a few of our important singletons within the server so
            // they don't get garbage collected out
            _presenter = MediaPlayerPresenter.getInstance();
            _fetcher = ScheduleFetcher.getInstance();
        }

        startForegroundHelper();

        // I think NOT_STICKY is the right flag to return here - basically means
        // "if the process is killed due to low system resource,don't bother starting it back up again"
        return START_NOT_STICKY;
    }
}
