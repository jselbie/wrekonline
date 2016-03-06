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


package com.selbie.wrek.metaproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import android.util.Log;

public class MetaStreamProxySession implements Runnable
{
    public final static String TAG = MetaStreamProxySession.class.getSimpleName();
    
    private Socket _clientSocket;
    private Thread _thread;
    private boolean _exitFlag;
    IMetadataCallback _metadataCallback;

    // constructor
    public MetaStreamProxySession(Socket clientSocket, IMetadataCallback metadataCallback)
    {
        Log.d(TAG, "constructor");
        _clientSocket = clientSocket;
        _metadataCallback = metadataCallback;
    }
    
    public void start()
    {
        Log.d(TAG, "start");
        if (_thread == null)
        {
            _thread = new Thread(this);
            _thread.start();
        }
    }
    
    public void stop()
    {
        Log.d(TAG, "stop");

        // this is a soft stop - just set a flag to signal the thread to exit 
        this._exitFlag = true;
    }

    @Override
    public void run()
    {
        try
        {
            processConnection();
        }
        catch (IOException ioex)
        {
            Log.d(TAG, "IOException in proxy session thread", ioex);
        }
        finally
        {
            cleanupConnection();
        }
        
        Log.d(TAG, "thread has exited");
    }

    private void processConnection() throws IOException
    {
        String request = readRequest();
        
        if (exitCheck() || (request == null) || (request.length()==0))
        {
            return;
        }
        
        request = getTargetFromUrl(request);
        
        startDownload(request);
        
        cleanupConnection();
    }    


    private boolean exitCheck()
    {
        if (_exitFlag)
        {
            Log.d(TAG, "exit flag has been set - aborting connection");
        }
        return _exitFlag;
    }

    private void cleanupConnection()
    {
        Log.d(TAG, "cleanupConnection");
        if (_clientSocket != null)
        {
            try
            {
                _clientSocket.close();
            }
            catch(IOException ioex)
            {
                Log.e(TAG, "error closing client socket", ioex);
            }
            finally
            {
                _clientSocket = null;
            }
        }
    }
    
    
    private String readRequest() throws IOException
    {
        ArrayList<String> requestheaders = new ArrayList<String>();
        String target = null;
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(_clientSocket.getInputStream()));
        
        while (exitCheck() == false)
        {
            String line = reader.readLine();
            if ((line == null) || line.equals(""))
            {
                break;
            }
            else
            {
                requestheaders.add(line);
            }
        }
        
        Log.d(TAG, "request has been received");
        for (String header : requestheaders)
        {
            Log.d(TAG, header);
        }
        
        if (requestheaders.size() > 0)
        {
            ArrayList<String> tokens = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(requestheaders.get(0));
            
            while (st.hasMoreTokens())
            {
                tokens.add(st.nextToken());
            }
            
            if (tokens.size() >= 2)
            {
                target = tokens.get(1);
            }
        }
        
        return target;

    }
    
    private String getTargetFromUrl(String formattedUrl) throws IOException
    {
        String target = "";
        Log.d(TAG, "getTargetFromUrl: " + formattedUrl);
        
        // decodeOriginalUrl (which is a wrapper for urlDecode) can throw an IllegalArgumentException.  Like the other places in the code where we call UrlDecode, we
        // catch all RuntimeExceptions as a precaution
        
        try
        {
            target = MetaStreamProxy.decodeOriginalUrl(formattedUrl);
        }
        catch (RuntimeException rtex)
        {
            throw new IOException("runtime exception when decoding url", rtex);
        }
        
        return target;
    }
    
    private void startDownload(String targetURL) throws IOException
    {
        URL url = new URL(targetURL);
        String statusline;
        String headerstrings = "";
        String responsePrelude = "";
        Map<String, List<String>> headers = null;
        Set<String> keys = null;
        HttpURLConnection connection = null;
        int chunkLogCount = 0;
        MetadataStreamFilter filter = new MetadataStreamFilter();
        int icymetaint = 0; // interval of 0 means "no metadata interval in stream"
        
        if (exitCheck())
        {
            return;
        }
        
        connection = (HttpURLConnection)url.openConnection();
        
        // we need to set the timeout values early before the connection is established - otherwise, they may not work
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(15000); // give the thread a chance to wake up and exit.

        // this is how we request the ICEcast server to send inline metadata within the mp3 stream
        connection.setRequestProperty("Icy-MetaData", "1");
        
        connection.connect();
        
        if (exitCheck())
        {
            return;
        }
        
        // read the headers from the response
        statusline = connection.getHeaderField(null); // the "null" header field is the status line according to the docs
        
        if (statusline == null)
        {
            Log.wtf(TAG, "connection.getHeaderField(null) returned null instead of a statusline");
            throw new IOException("connection.getHeaderField(null) returned null instead of a statusline");
        }
        
        headers = connection.getHeaderFields();
        
        // With some ad-hoc testing, I had a null pointer exception (crash) in this function, but my source was partially out of sync with the build. It happened as a result 
        // of toggling the network wifi and mobile data connections on/off while the app was running
        // It was something to do with the 'headers' or the 'keySet' variable being null. So we'll be a little more defensive in this code path and check for null
        // where we normally don't expect to
        
        if (headers == null)
        {
            Log.wtf(TAG, "connection.getHeaderFields returned null");
            throw new IOException("connection.getHeaderFields returned null");
        }
        
        keys = headers.keySet();
        
        if (keys == null)
        {
            Log.wtf(TAG, "headers.keySet returned null");
            throw new IOException("headers.keySet returned null");
        }
        
        for (String key : keys)
        {
            if ((key == null) || key.equals(""))
            {
                continue;
            }
            
            for (String val : headers.get(key))
            {
                
                String k = key.trim();
                String v = "";
                
                if (val != null)
                {
                    v = val.trim();
                }
                
                if (k.equals("icy-metaint"))
                {
                    try
                    {
                        icymetaint = Integer.parseInt(v);
                    }
                    catch (NumberFormatException nfex)
                    {
                        Log.wtf(TAG, "Trying to parse icy-metaint and it just threw a NumberFormatException");
                        throw new IOException("embedded NumberFormatException", nfex);
                    }
                }
                else
                {
                    // pass everything but the icy-metaint header back
                    headerstrings += k + ":";
                    
                    if (val != null)
                    {
                        headerstrings += " " + v;
                    }
                    
                    headerstrings += "\r\n";
                }
            }
        }
        
        // now put it all together
        responsePrelude = statusline + "\r\n" + headerstrings + "\r\n"; 
        
        Log.d(TAG, "---------responsePrelude--------");
        {
            String responsePreludeDebug = responsePrelude.replace("\r\n", "\n");
            Log.d(TAG, responsePreludeDebug);
        }
        Log.d(TAG, "---------------------------");
        
        // now convert to ascii bytes
        byte [] responsePreludeBytes = responsePrelude.getBytes("UTF-8");  // can throw UnsupportedEncodingException (which derives from IOException)
        _clientSocket.getOutputStream().write(responsePreludeBytes);
        
        // now configure the filter to understand the metadata interval and pass it the output stream
        // if icymetaint is "0", then that essentially means "no metadata expected"
        filter.init(icymetaint, _clientSocket.getOutputStream(), _metadataCallback);


        // main loop
        byte [] buffer = new byte[4096];
        while (exitCheck() == false)
        {
            int readresult = 0;
            
            try
            {
                InputStream inputstream = connection.getInputStream();
                
                if (inputstream == null)
                {
                    throw new IOException("connection.getInputStream returned null");
                }
                
                readresult = inputstream.read(buffer);
            }
            catch(SocketTimeoutException ex)
            {
                Log.d(TAG, "timeout waiting for http input stream to deliver some bytes");
                Log.d(TAG, "ex.bytesTransferred == " + ex.bytesTransferred);
                if (ex.bytesTransferred != 0)
                    Log.e(TAG, "read timed out, but bytesTranserred was non-zero???");
                readresult = 0; // should this be set to ex.bytesTransferred? That would be weird if there was a timeout exception and data actually transferred
            }
            
            if (readresult == -1)
            {
                Log.d(TAG, "connection.getInputStream.read() returned -1");
                break;
            }
            
            if (exitCheck())
            {
                break;
            }

            if (readresult > 0)
            {
                // // // _clientSocket.getOutputStream().write(buffer, 0, readresult);
                filter.write(buffer, 0, readresult);
                
                if (chunkLogCount < 10)
                {
                    // log the first 10 chunks so we can see in the debugger that streaming started
                    Log.d(TAG, "wrote " + readresult);
                    chunkLogCount++;
                }
            }
        }
    }

}
