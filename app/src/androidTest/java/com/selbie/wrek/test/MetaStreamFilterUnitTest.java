package com.selbie.wrek.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;

import com.selbie.wrek.metaproxy.IMetadataCallback;
import com.selbie.wrek.metaproxy.MetadataStreamFilter;

class ChunkDetails
{
    public int length;
    public int hash;
    public String meta;
}


public class MetaStreamFilterUnitTest extends TestCase implements IMetadataCallback
{
    public final static String TAG = MetaStreamFilterUnitTest.class.getSimpleName();
    
    public void testStreaming()
    {
        try
        {
            streamInChunks(1600, 10, 1600);
            streamInChunks(1600, 10, 1599);
            streamInChunks(1600, 10, 1601);
            streamInChunks(1600, 10, 799);
            streamInChunks(1600, 10, 800);
            streamInChunks(1600, 10, 801);
            
            for (int x = 0; x < 10; x++)
            {
                streamInChunks(1600, 10, 0);
            }
        }
        catch (IOException e)
        {
            fail();
        }
    }

    
    
    static int dumbHash(byte [] data, int offset, int length)
    {
        int h = 0;
        int highorder = 0;
        
        for (int x = 0; x < length; x++)
        {
            h = h << 5;
            h = h ^ (highorder >>> 27);
            h = h ^ data[x + offset];
        }
        
        return h;
    }
    
    static String createRandomMetaString(Random rand)
    {
        
        // 10% of the time, we want to simulate "no metadata" at the interval mark
        boolean isEmpty = rand.nextInt(10) == 0;
        
        if (isEmpty)
        {
            //Log.d(TAG, "Creating empty meta string");
            return "";
        }
        
        int len = rand.nextInt(4080) ; // random value between 1..4080
        //Log.d(TAG, "Creating random meta string of length: " + len);
        
        StringBuffer sb = new StringBuffer();
        for (int y = 0; y < len; y++)
        {
            char c = (char) ('A' + rand.nextInt(26));
            sb.append(c);
        }
        
        return sb.toString();
    }
    
    void addMetaStringToStream(String str, ByteArrayOutputStream stream)
    {
        byte [] charbytes  = str.getBytes();
        int padding_required = (16 - (charbytes.length % 16)) % 16;
        
        int chunks = ((charbytes.length + 15) / 16);
        assertTrue(chunks <= 255);
        assertTrue(chunks >= 0);
        stream.write(chunks);
        stream.write(charbytes, 0, charbytes.length);
        
        for (int x = 0; x < padding_required; x++)
        {
            stream.write(0);
        }
    }
    
    ArrayList<ChunkDetails> _chunksAllocated;
    ArrayList<ChunkDetails> _chunksParsed;
    ArrayList<String> _metadataParsed;
    ByteArrayInputStream _inputStream;
    ByteArrayOutputStream _outputStream;
    Random _rand = new Random(444);
    int _chunkSize;
    
    void setupStreams(int numChunks, int chunkSize)
    {
        _chunkSize = chunkSize;
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        _chunksAllocated = new ArrayList<ChunkDetails>();
        
        for (int chunkIndex=0; chunkIndex < numChunks; chunkIndex++)
        {
            byte [] databytes = new byte[chunkSize];
            _rand.nextBytes(databytes);
            int hash = dumbHash(databytes, 0, databytes.length);
            String meta = createRandomMetaString(_rand);
            
            ChunkDetails chunk = new ChunkDetails();
            chunk.hash = hash;
            chunk.length = chunkSize;
            chunk.meta = meta;
            _chunksAllocated.add(chunk);
            
            // now write the chunk out
            stream.write(databytes, 0, databytes.length);
            addMetaStringToStream(meta, stream);
        }
        
        _inputStream = new ByteArrayInputStream(stream.toByteArray());
        _outputStream = new ByteArrayOutputStream();
        _chunksParsed = new ArrayList<ChunkDetails>();
        _metadataParsed = new ArrayList<String>();
    }
    
    void confirmResults()
    {
        byte [] outputBytes = _outputStream.toByteArray();
        
        int numChunks = outputBytes.length / _chunkSize;
        assertEquals(_chunksAllocated.size(), numChunks);
        
        for (int x = 0; x < numChunks; x++)
        {
            int hash = dumbHash(outputBytes, x * _chunkSize, _chunkSize);
            ChunkDetails details = new ChunkDetails();
            details.hash = hash;
            details.length = _chunkSize;
            details.meta = "";
            
            assertEquals(_chunksAllocated.get(x).length, details.length);
            assertEquals(_chunksAllocated.get(x).hash, details.hash);
        }

        int y = 0;
        for (int x = 0; x < _chunksAllocated.size(); x++)
        {
            ChunkDetails chunk = _chunksAllocated.get(x);
            if (chunk.meta.isEmpty() == false)
            {
                String metaCompare = _metadataParsed.get(y);
                assertEquals(metaCompare, chunk.meta);
                y++;
            }
        }
    }
    
    
    void streamInChunks(int chunkSize, int numChunks, int readSize) throws IOException
    {
        setupStreams(numChunks, chunkSize);
        
        byte [] tempbuffer = new byte[Math.max(chunkSize, readSize) * 2 + 1];
        MetadataStreamFilter filter = new MetadataStreamFilter();
        filter.init(chunkSize, _outputStream, this);
        int count;
        
        while (true)
        {
            int toRead = tempbuffer.length;
            if (readSize == 0)
            {
                toRead = _rand.nextInt(tempbuffer.length);
            }
            
            count = _inputStream.read(tempbuffer, 0, toRead);
            if (count == -1)
            {
                break;
            }
            filter.write(tempbuffer, 0, count);
        }
        
        confirmResults();
    }
    

    

    @Override
    public void onNewMetadataAvailable(String metadata)
    {
        _metadataParsed.add(metadata);
    }

    
    
    
    
    
    
}
