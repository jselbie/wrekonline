package com.selbie.wrek.test;

import junit.framework.TestCase;
import com.selbie.wrek.metaproxy.IcecastMetadata;


public class IcecastMetadataUnitTest extends TestCase
{
    public String STREAM_TITLE = "StreamTitle";
    public String SIMPLE_METADATA = STREAM_TITLE + "='title of the song';";
    public String URLENCODED_METADATA = STREAM_TITLE + "='Bj%C3%B6rk - Human Behavior';";
    public String URLENCODED_TITLE_EXPECTED = "Bj\u00F6rk - Human Behavior";
    
    public String NOTITLE_METADATA = "Artist='Metallica';Album='Master of Puppets';Song='Battery';";
    
    public String EMBEDDED_QUOTE_METADATA_1 = "StreamTitle='Charlie%27s Angels';";
    public String EMBEDDED_QUOTE_METADATA_2 = "StreamTitle='E%3DMC2';";

    public void testMetadataParsing()
    {
        IcecastMetadata metadata;
        String str;
        
        metadata = new IcecastMetadata(SIMPLE_METADATA);
        assertEquals(metadata.getValue("StreamTitle"), "title of the song");
        assertEquals(metadata.getStreamTitle(), "title of the song");
     
        // url encode test
        metadata = new IcecastMetadata(URLENCODED_METADATA);
        assertEquals(metadata.getStreamTitle(), URLENCODED_TITLE_EXPECTED);

        
        metadata = new IcecastMetadata(NOTITLE_METADATA);
        // getStreamTitle should always return an empty string if there is no title
        str = metadata.getStreamTitle();
        assertNotNull(str);
        assertTrue(str.isEmpty());
        assertEquals(metadata.getValue("Artist"), "Metallica");
        assertEquals(metadata.getValue("Album"), "Master of Puppets");
        assertEquals(metadata.getValue("Song"), "Battery");
        
        metadata = new IcecastMetadata(EMBEDDED_QUOTE_METADATA_1);
        assertEquals(metadata.getStreamTitle(), "Charlie's Angels");
        
        metadata = new IcecastMetadata(EMBEDDED_QUOTE_METADATA_2);
        assertEquals(metadata.getStreamTitle(), "E=MC2");

    }
}
