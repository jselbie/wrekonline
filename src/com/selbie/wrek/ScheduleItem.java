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


public class ScheduleItem
{
    String _title;
    String _genre;
    String _time;
    String _logoURL;
    ArrayList<Stream> _streams;
    
    public ScheduleItem()
    {
        _title = "";
        _time = "";
        _logoURL = "";
        _streams = new ArrayList<Stream>();
    }
    
    
    public String getTitle()
    {
        return _title;
    }
    
    public void setTitle(String val)
    {
        _title = val;
    }
    
    public String getGenre()
    {
        return _genre;
    }
    
    public void setGenre(String val)
    {
        _genre = val;
    }
    
    public String getTime()
    {
        return _time;
    }
    
    public void setTime(String val)
    {
        _time = val;
    }
    
    public String getLogoURL()
    {
        return _logoURL;
    }
    
    public void setLogoURL(String val)
    {
        _logoURL = val;
    }
    
    @SuppressWarnings("unchecked")
    public ArrayList<Stream> getStreams()
    {
        return (ArrayList<Stream>) _streams.clone();
    }
    
    Stream getStreamForAllowedBitrate(int kbps)
    {
        Stream bestStream = null;
        int best = 0;
        int current = Integer.MAX_VALUE;
        
        for (Stream s : _streams)
        {
            current = s.getBitrate();
            
            if (bestStream == null)
            {
                bestStream = s;
                best = current;
            }
            else if ((best > kbps) && (current < best))
            {
                bestStream = s;
                best = current;
            }
            else if ((best < kbps) && (current > best) && (current <= kbps))
            {
                bestStream = s;
                best = current;
            }
        }

        return bestStream;
    }
    
    void AddStream(Stream stream)
    {
        _streams.add(stream);
    }

}
