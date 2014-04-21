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

public class ScheduleListFragment extends ListFragment
{
    private static final String TAG = "ScheduleListFragment";

    private static final int BITRATE_HIGH_KBIT_SEC = 128;
    private static final int BITRATE_LOW_KBIT_SEC = 24;

    ScheduleFetcherTask _fetcher;
    ArrayList<ScheduleItem> _list;

    @Override
    public void onStart()
    {
        super.onStart();
        StartScheduleDownload(); // kick off a download to go get the schedule()
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Log.d(TAG, "onStop");

        if (_fetcher != null)
        {
            _fetcher.cancel(true);
        }
    }

    void StartScheduleDownload()
    {
        if (_fetcher != null)
        {
            _fetcher.cancel(false);
            _fetcher = null;
        }

        ScheduleFetcherTask.ScheduleFetcherTaskCallback callback = new ScheduleFetcherTask.ScheduleFetcherTaskCallback()
        {

            @Override
            public void onComplete(ArrayList<ScheduleItem> schedule)
            {

                Log.d(TAG, "onComplete");

                if (schedule == null)
                {
                    schedule = new ArrayList<ScheduleItem>();
                }

                Log.d(TAG, "about to create ScheduleListAdapter");
                ScheduleListAdapter adapter = new ScheduleListAdapter(getActivity(), schedule);

                Log.d(TAG, "about to set adapter on ListFragment");
                ScheduleListFragment.this.setListAdapter(adapter);
                Log.d(TAG, "onComplete returns");

            }
        };

        _fetcher = new ScheduleFetcherTask(callback);
        _fetcher.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.schedule_list, container, false);
        return view;
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
