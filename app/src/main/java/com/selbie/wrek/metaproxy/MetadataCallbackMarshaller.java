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

import android.os.Handler;


// This class marshals the "onNewMetadata" callback from the socket thread back to the UI thread
// Pass this as the callback to the MetadataProxy construct. 
// Then call the attach method below with the callback for the UI thread
// Call the dispose() method when the proxy goes away
public class MetadataCallbackMarshaller implements Runnable, IMetadataCallback
{
    Handler _handler;
    String _metadata;
    IMetadataCallback _appCallback;
    boolean _isDirty;

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
            _isDirty = false;
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
            // And because we assign _metadata within the lock.
            meta = _metadata;
            _isDirty = false;
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
                
                if (_isDirty == false)
                {
                    _isDirty = true;
                    _handler.post(this);
                }
            }
        }
    }
    
}
