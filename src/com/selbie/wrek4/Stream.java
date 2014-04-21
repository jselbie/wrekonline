package com.selbie.wrek4;

import java.util.ArrayList;

public class Stream
{
    private int _bitrate;
    private String _url_m3u;
    private ArrayList<String> _playlist;
    private boolean _isLiveStream;
    
    public Stream()
    {
        _bitrate = 0;
        _url_m3u = "";
        _playlist = new ArrayList<String>();
        _isLiveStream = false;
    }
    
    public int getBitrate()
    {
        return _bitrate;
    }
    
    public void setBitrate(int bitrateKBPS)
    {
        _bitrate = bitrateKBPS;
    }
    
    public String getURL()
    {
        return _url_m3u;
    }
    
    public void setURL(String val)
    {
        _url_m3u = val;
    }
    
    public void addToPlayList(String url)
    {
        _playlist.add(url);
    }
    
    public ArrayList<String> getPlayList()
    {
        return _playlist;
    }
    
    public boolean getIsLiveStream() {
        return _isLiveStream;
    }
    
    public void setIsLiveStream(boolean isLiveStream) {
        _isLiveStream = isLiveStream;
    }
    
    
}


