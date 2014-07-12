package com.selbie.wrek.metaproxy;

import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

class MetadataStreamFilter
{
    public final static String TAG = MetadataStreamFilter.class.getSimpleName(); 
    
    enum State
    {
        Data,         // reading mp3 bytes
        MetaSizeByte, // next byte read will be used to compute _metaTotal
        MetaData      // reading metadata bytes
    }
    
    private IMetadataCallback _callback;
    
    private OutputStream _outputstream;
    
    private State _state = State.Data;
    
    private int _metaInterval;
    private int _dataCount;
    
    private byte [] _metaBuffer; // temp buffer for holding meta data
    private int _metaCount;      // how many bytes have been written to the metaBuffer;
    private int _metaTotal;      // how many total metadata bytes we are expecting to read
    
    private static final int MAX_METABUFFER_SIZE = 16*255;
    
    private String _latestMetaData;  // is null when there is no new metadata to be consumed
    
    public MetadataStreamFilter()
    {
        // The metadata length is computed by reading the length byte from the stream and multiplying by 16
        // Hence, the largest metabuffer size can only be 16*255 bytes 
        _metaBuffer = new byte[MAX_METABUFFER_SIZE];  
        init(0,null, null);
    }
    
    public void init(int interval, OutputStream outputstream, IMetadataCallback callback)
    {
        _metaCount = 0;
        _metaTotal = 0;
        
        _outputstream = outputstream;
        _metaInterval = interval;
        _dataCount = 0;
        
        _state = State.Data;

        _latestMetaData = null;
        
        _callback = callback;
    }
    
    public void write(byte [] buffer, int offset, int count) throws IOException
    {
        while (count > 0)
        {
            if (_state == State.Data)
            {
                int toWrite = _metaInterval - _dataCount;
                if ((_metaInterval == 0) || (toWrite > count))
                {
                    toWrite = count;
                }
                
                writeToOutputStream(buffer, offset, toWrite);
                offset += toWrite;
                count -= toWrite;
            }
            else if (_state == State.MetaSizeByte)
            {
                // consume exactly 1 byte, and compute the amount of meta bytes to follow
                _metaCount = 0;
                
                byte b = buffer[offset];
                int b_int = b & 0xff; // treat byte as unsigned and upcast to int
                
                _metaTotal = (b_int * 16);
                offset++;
                count--;
                
                assert(_metaTotal >= 0);
                assert(_metaTotal <= MAX_METABUFFER_SIZE);
                
                if (_metaTotal == 0)
                {
                    _state = State.Data;
                }
                else
                {
                    _state = State.MetaData;
                }
            }
            else if (_state == State.MetaData)
            {
                int toWrite = _metaTotal - _metaCount;
                if (toWrite > count)
                {
                    toWrite = count;
                }
                processMetaData(buffer, offset, toWrite);
                offset += toWrite;
                count -= toWrite;
            }
        } // while
    } // write method
    
    // returns the latest metadata string generated from the last write call (if any). Subsequent calls return null until new metadata is written
    public String getLatestMetadata()
    {
        String result = _latestMetaData;
        _latestMetaData = null;
        return result;
    }    
    
    private void writeToOutputStream(byte [] buffer, int offset, int count) throws IOException
    {
        assert (_state == State.Data);
        
        _outputstream.write(buffer, offset, count);
        
        if (_metaInterval != 0)
        {
            _dataCount += count;
        
            assert(_dataCount <= _metaInterval);
            assert(_state == State.Data);
            
            if (_dataCount == _metaInterval)
            {
                _dataCount = 0;
                _state = State.MetaSizeByte;
            }
        }
    }
    
    private void processMetaData(byte [] buffer, int offset, int count)
    {

        assert (_state == State.MetaData);
        
        System.arraycopy(buffer, offset, _metaBuffer, _metaCount, count);
        _metaCount += count;
        
        assert(_metaCount <= _metaTotal);
        
        if (_metaCount == _metaTotal)
        {
            int metaLength = 0;
            
            // The icy metadata is in 16-byte chunks - padded with zero bytes.
            // The null bytes just generate noise in the conversion to string.
            // So we ignore those in the conversion
            for (int x = 0; x < _metaTotal; x++)
            {
                if (_metaBuffer[x] != 0)
                {
                    metaLength++;
                }
                else
                {
                    break;
                }
            }
            
            _latestMetaData = new String(_metaBuffer, 0, metaLength);
            _state = State.Data;
            _dataCount = 0;
            _metaTotal = 0;
            _metaCount = 0;
            
            Log.d(TAG, "New Metadata: " + _latestMetaData);
            
            if (_callback != null)
            {
                _callback.onNewMetadataAvailable(_latestMetaData);
            }
        }
    }
}
