package com.selbie.wrek4;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import android.util.JsonReader;

public class JsonHandler
{
    public Stream ParseStream(JsonReader reader) throws IOException
    {
        String url_m3u = "";
        int bitrate = -1;
        boolean isLive = false;
        ArrayList<String> playlist = new ArrayList<String>();

        reader.beginObject();

        while (reader.hasNext())
        {
            String name = reader.nextName();

            if (name.equals("url_m3u"))
            {
                url_m3u = reader.nextString();
            }
            else if (name.equals("bitrate"))
            {
                bitrate = reader.nextInt();
            }
            else if (name.equals("playlist"))
            {
                reader.beginArray();
                while (reader.hasNext())
                {
                    String url = reader.nextString();
                    playlist.add(url);
                }
                reader.endArray();
            }
            else if (name.equals("live"))
            {
                isLive = reader.nextBoolean();
            }
            else
            {
                reader.skipValue();
            }
        }

        reader.endObject();

        if ((url_m3u.equals("") || bitrate == -1) || (playlist.size() == 0))
        {
            throw new IOException("JsonHandler.ParseStream - failed to parse stream");
        }

        Stream stream = new Stream();
        stream.setBitrate(bitrate);
        stream.setURL(url_m3u);
        stream.setIsLiveStream(isLive);
        for (String url : playlist)
        {
            stream.addToPlayList(url);
        }
        return stream;
    }

    public ScheduleItem ParseItem(JsonReader reader) throws IOException
    {
        reader.beginObject();
        String title = null, timestring = null, logoURL = null, genre = null;
        ArrayList<Stream> streams = new ArrayList<Stream>();

        while (reader.hasNext())
        {
            String name = reader.nextName();

            if (name.equals("title"))
            {
                title = reader.nextString();
            }
            else if (name.equals("genre"))
            {
                genre = reader.nextString();
            }
            else if (name.equals("time"))
            {
                timestring = reader.nextString();
            }
            else if (name.equals("logo"))
            {
                logoURL = reader.nextString();
            }
            else if (name.equals("streams"))
            {
                reader.beginArray();

                while (reader.hasNext())
                {
                    streams.add(ParseStream(reader));
                }

                reader.endArray();
            }
            else
            {
                reader.skipValue();
            }
        }

        reader.endObject();

        if ((title == null) || (genre == null) || (timestring == null) || (logoURL == null) || (streams.size() == 0))
        {
            throw new IOException("parse error");
        }

        ScheduleItem item = new ScheduleItem();
        item.setTitle(title);
        item.setGenre(genre);
        item.setTime(timestring);
        item.setLogoURL(logoURL);

        for (Stream s : streams)
        {
            item.AddStream(s);
        }

        return item;
    }

    public ArrayList<ScheduleItem> ParseItemsArray(JsonReader reader) throws IOException
    {
        ArrayList<ScheduleItem> items = new ArrayList<ScheduleItem>();

        reader.beginArray();
        while (reader.hasNext())
        {
            items.add(ParseItem(reader));
        }
        reader.endArray();
        return items;
    }

    public ArrayList<ScheduleItem> ExtractScheduleFromJson(String str) throws IOException
    {
        ArrayList<ScheduleItem> items = null;
        JsonReader reader = new JsonReader(new StringReader(str));

        reader.beginObject();

        while (reader.hasNext())
        {
            String name = reader.nextName();

            if (name.equals("items"))
            {
                items = ParseItemsArray(reader);
            }
            else
            {
                reader.skipValue();
            }
        }

        reader.endObject();

        return items;
    }

}
