package com.selbie.wrek4;

import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

class ScheduleListAdapter extends ArrayAdapter<ScheduleItem> 
{
    private Activity _activity;
    
    public ScheduleListAdapter(Activity activity,  ArrayList<ScheduleItem> items)
    {
        super(activity, 0, items);
        _activity = activity;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        
        View view = convertView;
        ScheduleItem item = getItem(position);
        if (view == null)
        {
            view = _activity.getLayoutInflater().inflate(R.layout.schedule_item2, null);
        }
        
        ((TextView)view.findViewById(R.id.tvTitle)).setText(item.getTitle());
        ((TextView)view.findViewById(R.id.tvGenre)).setText(item.getGenre());
        ((TextView)view.findViewById(R.id.tvShowTime)).setText(item.getTime());

        ImageView iv = (ImageView)view.findViewById(R.id.showLogo);
        UrlImageViewHelper.setUrlDrawable(iv, item.getLogoURL());
        
        return view;
    }
    
}
