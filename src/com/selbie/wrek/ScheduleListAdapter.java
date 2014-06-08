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
