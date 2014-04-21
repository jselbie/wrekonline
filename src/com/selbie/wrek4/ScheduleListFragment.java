package com.selbie.wrek4;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.selbie.wrek4.ScheduleFetcher.ScheduleFetcherCallback;

public class ScheduleListFragment extends ListFragment implements ScheduleFetcherCallback
{
    private static final String TAG = "ScheduleListFragment";

    private static final int BITRATE_HIGH_KBIT_SEC = 128;
    private static final int BITRATE_LOW_KBIT_SEC = 24;

    ArrayList<ScheduleItem> _list;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        
        super.onCreate(savedInstanceState);
        
        ScheduleFetcher fetcher = ScheduleFetcher.getInstance();
        fetcher.attachObserver(this);
        
        // note - the view has not been inflated at this point
        
        _list = fetcher.getLastestSchedule(); // this will trigger a refresh if needed
        onNewSchedule(_list);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.schedule_list, container, false);
        return view;
    }
    
    
    @Override public void onStart()
    {
        super.onStart();

        // this is for a corner case. If we navigated back to the MainActivity without it being previously destroyed,
        // but never had a schedule to begin with (or the current one is expired), then force a refresh
        configureSpinnerControl(true);
        ScheduleFetcher fetcher = ScheduleFetcher.getInstance();
        fetcher.startRefreshIfNeeded();
    }
    
    
    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        
        super.onDestroy();
        ScheduleFetcher.getInstance().detachObserver(this);
    }
    
    
    @Override
    public void onNewSchedule(ArrayList<ScheduleItem> schedule)
    {
        ScheduleListAdapter adapter = new ScheduleListAdapter(getActivity(), schedule);
        this.setListAdapter(adapter);
    }
    
    @Override
    public void onScheduleDownloadError(int errorcode)
    {
        // handle the failure case
        // If we already have a non-empty schedule, then we'll just keep using it
        // we can decide later if we want to show an error message elsewhere...
        // If we can't get a schedule - then build a provisional schedule consisting only of the live streams
        
        if (_list.size() > 0)
        {
            Log.d(TAG, "onError - reusing existing schedule since it is non-empty");
        }
        else
        {
            Log.d(TAG, "onError - can't get schedule!");
            configureSpinnerControl(false); // put the empty view into "error mode"
        }
    }
    
    void configureSpinnerControl(boolean spinning)
    {
        View view = getView();
        
        // check to make sure that the view is still inflated.
        if (view == null)
        {
            return;
        }
        
        TextView tv = (TextView)(getView().findViewById(R.id.spinnerText));
        ProgressBar progbar = (ProgressBar)(getView().findViewById(R.id.spinner));
        
        if (spinning)
        {
            tv.setText(R.string.downloading_schedule);
            progbar.setVisibility(View.VISIBLE);
        }
        else
        {
            tv.setText(R.string.downloading_schedule_error);
            progbar.setVisibility(View.GONE);
        }
        
    }
    



    @Override
    public void onListItemClick(ListView listview, View view, int position, long id)
    {
        ScheduleItem item = (ScheduleItem) (getListAdapter().getItem(position));

        int targetBitrate = getTargetBitrate();
        Stream stream = item.getStreamForAllowedBitrate(targetBitrate);
        if ((stream != null) && (stream.getPlayList().size() > 0))
        {
            MediaPlayerPresenter presenter = MediaPlayerPresenter.getInstance();
            Log.d(TAG, "Setting playlist for item " + position + " (" + item.getTitle() + ")");
            presenter.setPlaylist(item.getTitle(), stream.getPlayList(), stream.getIsLiveStream());
        }
        else
        {
            Log.e(TAG, "No stream or playlist for selected item!!!");
        }
    }

    @TargetApi(16)
    int getTargetBitrate()
    {

        ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Activity.CONNECTIVITY_SERVICE);

        boolean isMetered = false;
        boolean isWifi = false;
        boolean isEthernet = false;
        int result = 0;

        int bandwidth_settings = SettingsActivity.getBitrateSetting(getActivity());

        switch (bandwidth_settings)
        {

            case SettingsActivity.BITRATE_SETTINGS_HIGH:
            {
                Log.d(TAG, "Using high bitrate settings");
                result = BITRATE_HIGH_KBIT_SEC;
                break;
            }
    
            case SettingsActivity.BITRATE_SETTINGS_LOW:
            {
                Log.d(TAG, "Using low bitrate settings");
                result = BITRATE_LOW_KBIT_SEC;
                break;
            }
    
            default:
            {
                result = 0;
                break;
            }
        }

        if (result == 0)
        {

            // default to low-bitrate unless we specifically detect we are on a
            // non-mobile connection
            result = BITRATE_LOW_KBIT_SEC;

            Log.d(TAG, "Using automatic bandwidth settings");

            NetworkInfo netinfo = connManager.getActiveNetworkInfo();
            if (netinfo != null)
            {
                isWifi = (netinfo.getType() == ConnectivityManager.TYPE_WIFI);
                isEthernet = (netinfo.getType() == ConnectivityManager.TYPE_ETHERNET);
            }

            // isActiveNetworkMetered was introduced in API 16 (Jelly Bean)
            if (android.os.Build.VERSION.SDK_INT >= 16)
            {
                isMetered = connManager.isActiveNetworkMetered();
            }

            Log.d(TAG, "isWifi = " + isWifi);
            Log.d(TAG, "isEthernet = " + isWifi);
            Log.d(TAG, "isMetered = " + isMetered);

            if ((isWifi || isEthernet) && !isMetered)
            {
                result = BITRATE_HIGH_KBIT_SEC;
            }
        }

        Log.d(TAG, "getTargetBitrate() returns " + result + "kbit/sec");

        return result;
    }



}
