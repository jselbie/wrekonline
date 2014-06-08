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


