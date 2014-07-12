package com.selbie.wrek.metaproxy;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import android.util.Log;

public class IcecastMetadata
{
    static final String TAG = IcecastMetadata.class.getSimpleName();
    
    private HashMap<String, String> _map;

    private static String urlDecodeWrapper(String s)
    {
        String result = s;
        
        try
        {
            result = URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            Log.d(TAG, "urlDecodeWrapper failed", e);
        }
        catch(RuntimeException rtex)
        {
            // UrlDecoder.decode will throw IllegalArgumentException if it encounters a badly formatted % sequence
            //
            // For the sake of robustness, since we are parsing network data, let's go ahead and catch all
            // RT exceptions.
            Log.d(TAG, "urlDecodeWrapper failed on runtime exception", rtex);
        }
        
        return result;
    }
    
    public IcecastMetadata(String metadata)
    {
        // tick marks within a key or value will be URL escaped (in theory), so let's parse off of that
        // example: StreamTitle='Ozzy Osbourne - Crazy Train';Foobar='';StreamUrl='http://myserver.org/music';
        
        int length = (metadata == null) ? 0 : metadata.length();
        int start=0;
        int end;
        String key, val;
        _map = new HashMap<String, String>();
        
        while (start < length)
        {
            end = metadata.indexOf("='", start);
            
            if ((end == -1) || (end <= start)) // you can't have an empty key
            {
                break;
            }
            
            key = metadata.substring(start, end);
            
            start = end+2;
            if (start >= length)
            {
                break;
            }
            
            end = metadata.indexOf("';", start);
            
            if ((end == -1) || (end < start)) // but you can have an empty value where (end == start) (e.g. foobar='';)
            {
                break;
            }
            
            val = metadata.substring(start, end);
            
            key = urlDecodeWrapper(key);
            val = urlDecodeWrapper(val);
            
            _map.put(key, val);
            
            start = end+2;
        }
        
    }
    
    public String getValue(String key)
    {
        return _map.get(key);
    }
    
    public String getStreamTitle()
    {
        String result = getValue("StreamTitle");
        return (result == null) ? "" : result;
    }
}
