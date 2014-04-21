package com.selbie.wrek4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ContentDownloader
{
    public static String downloadString(String urlstring) throws IOException
    {
        URL url = new URL(urlstring);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        
        connection.setReadTimeout(30000);
        connection.setConnectTimeout(30000);
        
        connection.connect();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        StringBuilder sb = new StringBuilder();
        char [] buffer = new char[5000];
        
        while (true)
        {
            int result = reader.read(buffer);
            if (result < 0)
            {
                break;
            }
            
            sb.append(buffer, 0, result);
        }
        
        String response = sb.toString();
        return response;
    }
}

