package com.selbie.wrek4;

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
