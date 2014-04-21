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
        Stream lowestStream = null;
        int lowest = Integer.MAX_VALUE;
        int best = 0;
        
        for (Stream s : _streams)
        {
            if ((lowestStream == null) || (s.getBitrate() < lowest))
            {
                lowestStream = s;
                lowest = s.getBitrate();
            }
            
            if (s.getBitrate() <= kbps)
            {
                if ((bestStream == null) || (s.getBitrate() > best))
                {
                    bestStream = s;
                    best = s.getBitrate();
                }
            }
        }

        // if we didn't find a stream that was under the target bitrate, just return the lowest bitrate stream available
        if (bestStream == null)
        {
            bestStream = lowestStream;
        }
        
        return bestStream;
    }
    
    void AddStream(Stream stream)
    {
        _streams.add(stream);
    }

}
