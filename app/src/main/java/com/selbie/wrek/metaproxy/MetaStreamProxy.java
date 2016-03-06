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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.util.Log;

public class MetaStreamProxy implements Runnable
{
    public static final String TAG = MetaStreamProxy.class.getSimpleName();

    private ServerSocket _listenSocket;
    private int _listenPort;
    private Thread _thread;
    private boolean _exitFlag;
    private ArrayList<MetaStreamProxySession> _sessions;
    String _listenIP;
    boolean _threadMadeSocket;
    boolean _hasBeenStarted;
    IMetadataCallback _metadataCallback;
    
    public MetaStreamProxy(IMetadataCallback metadataCallback)
    {
        Log.d(TAG, "constructor");
        _sessions = new ArrayList<MetaStreamProxySession>();
        _listenIP = "127.0.0.1"; 
        _metadataCallback = metadataCallback;
        _threadMadeSocket = false;
        _listenPort = 0;
    }
    
    private synchronized void waitForSocketReady()
    {
        Log.d(TAG, "waitForSocketReady - enter");
        while ((_threadMadeSocket == false) && _thread.isAlive())
        {
            try
            {
                wait(250);
            }
            catch (InterruptedException e)
            {
                ;
            }
        }
        Log.d(TAG, "waitForSocketReady - completed");
    }
    
    private synchronized void notifySocketIsReady()
    {
        _threadMadeSocket = true;
        notifyAll();
    }

    private void start()
    {
        Log.d(TAG, "start method called");

        // just create a new instance if you need to restart a proxy
        assert(_hasBeenStarted == false);

        _threadMadeSocket = false;
        _hasBeenStarted = true;
        _exitFlag = false;
        
        // start the listening thread
        _thread = new Thread(this);
        _thread.start();
        
        // wait for the thread to initialize the listening socket
        waitForSocketReady();
        
    }
    
    public static MetaStreamProxy createAndStart(IMetadataCallback metadataCallback)
    {
        MetaStreamProxy proxy = new MetaStreamProxy(metadataCallback);
        proxy.start();
        
        if (proxy.getPort() == 0)
        {
            Log.d(TAG, "createAndStart - getPort returned 0, which means the socket didn't really initialize.  Returning null");
            proxy.stop();
            proxy = null;
        }
        
        return proxy;
    }
    
    public void stop()
    {
        Log.d(TAG, "stop method called");
        
        // This is a soft stop. We're just going to set the exit flag.
        // The thread that is blocked on _listenSocket.accept() will eventually
        // wake up from the connection timeout and exit gracefully
        
        _exitFlag = true;
        closeAllSessions();
    }
    
    private void cleanupListenSocket()
    {
        Log.d(TAG, "cleanupListenSocket method called");
        
        if (_listenSocket != null)
        {
            try
            {
                _listenSocket.close();
            }
            catch(Exception ex)
            {
                Log.e(TAG, "_listenSocket close error", ex);
            }
            finally
            {
                _listenSocket = null;
                _listenPort = 0;
            }
        }    
    }
    
    public int getPort()
    {
        return _listenPort;
    }
    
    public String formatUrl(String originalUrl)
    {
        String encodedUrl = originalUrl;
        
        try
        {
            encodedUrl = encodeOriginalUrl(_listenIP, _listenPort, originalUrl);
        }
        catch(UnsupportedEncodingException ex)
        {
            Log.e(TAG, "formatUrl hit an unsupported encoding exception", ex);
        }
        catch(RuntimeException rtex)
        {
            Log.e(TAG, "formatUrl hit a runtime exception", rtex);
        }
        
        return encodedUrl; // in case of error, return back the original URL.  This will bypass the metadata proxy listener, but is better than not connecting at all
    }
    

    @Override
    public void run()
    {
        Log.d(TAG, "Thread start");
        
        try
        {
            runImpl();
        }
        catch (IOException e)
        {
            Log.d(TAG, "IOException thread caught by run method. Cleaning up and exiting");
            
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            cleanupListenSocket();
        }
        
        notifySocketIsReady(); // in case initListenSocket threw an exception, we still need to unblock the wait loop in the start method
        
        Log.d(TAG, "The Listen Thread has exited");
    }
    
    private void initListenSocket() throws IOException
    {
        // ideally, we'd init the socket on the UI thread, but Android won't let us do that

        InetAddress addrLoopback = InetAddress.getByName(null); // will return either ::1 or 127.0.01
        
        _listenSocket = new ServerSocket(0, 10, addrLoopback);
        _listenPort = _listenSocket.getLocalPort();
        
        InetSocketAddress localSocketAddress = (InetSocketAddress)(_listenSocket.getLocalSocketAddress());
        InetAddress addrLocal = localSocketAddress.getAddress();
        
        _listenIP = addrLocal.getHostAddress();
        
        if (addrLocal instanceof java.net.Inet6Address)
        {
            // RFC 2732. Format for Literal IPv6 Addresses in URLs. Which basically says, "put brackets around the IPv6 address"  
            _listenIP = "[" + _listenIP + "]";
        }
        
        _listenSocket.setSoTimeout(5000); // wake up every 5 seconds to check the exit state
        
        notifySocketIsReady();
    }
    
    private void runImpl() throws IOException
    {
        // wait for incoming connection
        Socket clientSock = null;
        
        initListenSocket();
        
        Log.d(TAG, "Listening on port: " + _listenPort);
        
        while (_exitFlag == false)
        {
            try
            {
                clientSock = _listenSocket.accept();
                
                Log.d(TAG, "Accepting connection from: " + clientSock.getInetAddress().getHostAddress() + ":" + clientSock.getPort());
                
                MetaStreamProxySession session = new MetaStreamProxySession(clientSock, _metadataCallback);
                session.start();
                addSession(session);
            }
            catch (InterruptedIOException iioex)
            {
                // to be expected every 5 seconds;
            }
            catch (IOException ioex)
            {
                Log.e(TAG, "IOException in accept loop", ioex);
            }
        }
        
        cleanupListenSocket();
        
        // closeAllSessions will get called by the stop() method in the app thread, but there's a race condition
        // where addSessions above can get called while we are exiting
        // so we just call it again
        closeAllSessions();
    }
    
    private void addSession(MetaStreamProxySession session)
    {
        Log.d(TAG, "addSession");
        
        // should only be be called by the worker thread
        assert(_thread.getId() == Thread.currentThread().getId());
        
        synchronized(_sessions)
        {
            _sessions.add(session);
        }
    }
    
    private void closeAllSessions()
    {
        Log.d(TAG, "closeAllSessions");

        synchronized(_sessions)
        {
            for (MetaStreamProxySession session : _sessions)
            {
                session.stop();
            }
            
            _sessions.clear();
        }
    }
    
    static public String encodeOriginalUrl(String listenIP, int listenPort, String embeddedUrl) throws UnsupportedEncodingException
    {
        String encodedUrl = "http://" +  listenIP + ":" + listenPort + "/" + java.net.URLEncoder.encode(embeddedUrl, "UTF-8");
        return encodedUrl;
    }
    
    static public String decodeOriginalUrl(String formattedUrl) throws IOException
    {
        String targetUrlEncoded = formattedUrl;
        String targetUrlDecoded = "";
        
        // strip off the leading forward slash from the resource request.  Everything to the right of it is another URL that's been URL encoded
        if ((targetUrlEncoded.length() > 0) && (targetUrlEncoded.charAt(0)=='/'))
        {
            targetUrlEncoded = targetUrlEncoded.substring(1);
        }
        else
        {
            Log.wtf(TAG, "decodeOriginalUrl: expected string to begin with a forward slash");
        }
        
        targetUrlDecoded = java.net.URLDecoder.decode(targetUrlEncoded, "UTF-8");
        
        return targetUrlDecoded;
    }
}
