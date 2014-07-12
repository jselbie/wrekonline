package com.selbie.wrek.metaproxy;

import android.os.Handler;


// This class marshals the "onNewMetadata" callback from the socket thread back to the UI thread
// Pass this as the callback to the MetadataProxy construct. 
// Then call the attach method below with the callback for the UI thread
// Call the dispose() method when the proxy goes away
public abstract class MetadataCallbackMarshaller implements Runnable, IMetadataCallback
{
    Handler _handler;
    String _metadata;
    IMetadataCallback _appCallback;

    public MetadataCallbackMarshaller()
    {
        _metadata = "";
    }
    
    public String getMetadata()
    {
        String meta;
        synchronized(this)
        {
            // Probably don't need to take the lock to do an atomic reference
            // assignment. But it's a good idea since we assign _metadata within the lock
            meta = _metadata;
        }
        return meta;
    }
    
    public void attach(IMetadataCallback appCallback)
    {
        _appCallback = appCallback;

        synchronized(this)
        {
            if (_handler == null)
            {
                _handler = new Handler();
            }
        }
    }
    
    public void detach()
    {
        _appCallback = null;
        
        synchronized(this)
        {
            if (_handler != null)
            {
                _handler.removeCallbacks(this);
                _handler = null;
            }
        }
    }
    
    public void dispose()
    {
        detach();
    }


    @Override
    public void run()
    {
        String meta;
        synchronized(this)
        {
            // Probably don't need to take the lock to do an atomic reference
            // assignment. But it's a good idea since _handler needs to be guarded.
            // And because we assign _metadata within the lock
            meta = _metadata;
        }
        
        if (_appCallback != null)
        {
            _appCallback.onNewMetadataAvailable(meta);
        }
    }
    

    @Override
    public void onNewMetadataAvailable(String metadata)
    {
        synchronized(this)
        {
            if (_handler != null)
            {
                _metadata = metadata; 
                _handler.post(this);
            }
        }
    }
    
}
