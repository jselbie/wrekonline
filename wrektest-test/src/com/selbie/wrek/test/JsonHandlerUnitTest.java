package com.selbie.wrek.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.test.AndroidTestCase;
import android.util.Log;

import com.selbie.wrek.JsonHandler;
import com.selbie.wrek.ScheduleItem;
import com.selbie.wrek.Stream;




public class JsonHandlerUnitTest extends AndroidTestCase
{
    public final static String TAG = JsonHandlerUnitTest.class.getSimpleName();
    
    protected Resources getResources(String packageName) throws NameNotFoundException {
        PackageManager pm = getContext().getPackageManager();
        return pm.getResourcesForApplication(packageName);
    }
    
    String loadJson() throws IOException, NotFoundException, NameNotFoundException
    {
        InputStream stream = this.getResources("com.selbie.wrek.test").openRawResource(R.raw.sample_json);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        
        StringBuffer sb = new StringBuffer();
        
        char [] temp = new char[50000];
        while (true)
        {
            int result = reader.read(temp);
            if (result == -1)
            {
                break;
            }
            sb.append(temp, 0, result);
        }
        
        return sb.toString();
        
    }
    
    public void testJsonHandler()
    {
    
        JsonHandler handler = new JsonHandler();
        ArrayList<ScheduleItem> items = null;
        String sample_json = "";
        
        
        try
        {
            sample_json = loadJson();
        }
        catch (NotFoundException | NameNotFoundException | IOException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        
        try
        {
            items = handler.extractScheduleFromJson(sample_json);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        assertNotNull(items);
        assertEquals(43, items.size());
        
        
        for (int x = 0; x < items.size(); x++)
        {
            ScheduleItem item = items.get(x);
            
            Log.d(TAG, item.getTitle());
            
            assertNotNull(item.getGenre());
            assertNotNull(item.getLogoURL());
            assertNotNull(item.getTime());
            assertNotNull(item.getTitle());
            
            assertFalse(item.getTitle().isEmpty());
            assertFalse(item.getLogoURL().isEmpty());
            
            assertNotNull(item.getStreams());
            assertNotNull(item.getStreams().size() > 0);
        }
        
        // now do some individual validation
        
        
        // LIVE STREAM
        ScheduleItem item = items.get(0);
        Stream stream;
        
        assertEquals("Live air stream", item.getTitle());
        assertTrue(item.getTime().isEmpty());
        assertEquals("http://www.selbie.com/wrek/radio2.png", item.getLogoURL());
        assertEquals("What's on the air right now", item.getGenre());
        
        stream = item.getStreams().get(0);
        assertEquals(24, stream.getBitrate());
        assertTrue(stream.getHasIcyMetaInt());
        assertTrue(stream.getIsLiveStream());
        assertEquals("http://streaming.wrek.org:8000/wrek_live-24kb-mono.m3u", stream.getURL());
        assertEquals(1, stream.getPlayList().size());
        assertEquals("http://streaming.wrek.org:8000/wrek_live-24kb-mono", stream.getPlayList().get(0));
        
        stream = item.getStreams().get(1);
        assertEquals(128, stream.getBitrate());
        assertTrue(stream.getHasIcyMetaInt());
        assertTrue(stream.getIsLiveStream());
        assertEquals("http://streaming.wrek.org:8000/wrek_live-128kb.m3u", stream.getURL());
        assertEquals(1, stream.getPlayList().size());
        assertEquals("http://streaming.wrek.org:8000/wrek_live-128kb", stream.getPlayList().get(0));
        

        // SHOW STREAM
        item = items.get(42);
        assertEquals("Personality Crisis", item.getTitle());
        assertEquals("Sun 7/27 10:00 PM", item.getTime());
        assertEquals("http://www.wrek.org/wp-content/themes/wrek/images/ss_icons/PC.png", item.getLogoURL());
        assertEquals("New Wave, Punk, Electronic", item.getGenre());
        
        stream = item.getStreams().get(1);
        assertEquals(128, stream.getBitrate());
        assertFalse(stream.getHasIcyMetaInt());
        assertFalse(stream.getIsLiveStream());
        assertEquals("http://www.wrek.org/playlist.php/main/128kbs/current/PC.m3u", stream.getURL());
        assertEquals(4, stream.getPlayList().size());
        assertEquals("http://archive.wrek.org/main/128kb/Sun2330.mp3", stream.getPlayList().get(3));
        
    }
    
    
    
    
}


