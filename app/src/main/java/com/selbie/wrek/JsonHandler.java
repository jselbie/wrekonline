/*
   Copyright 2016 John Selbie

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

package com.selbie.wrek;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class JsonHandler {

    private Stream parseStream(JSONObject streamNode) throws JSONException {

        Stream stream = new Stream();

        stream.setBitrate(streamNode.getInt("bitrate"));           // required
        stream.setHasIcyMetaInt(streamNode.optBoolean("metaint")); // optional, default to false
        stream.setIsLiveStream(streamNode.optBoolean("live"));     // optional, default to false
        stream.setURL(streamNode.optString("url_m3u"));            // optional, not actually used

        JSONArray arr = streamNode.getJSONArray("playlist"); // mandatory, throw if not there

        if (arr.length() <= 0) {
            throw new JSONException("playlist should be a json array with at least one item");
        }

        for (int i = 0; i < arr.length(); i++) {
            String strURL = arr.getString(i);   // mandatory, throw if not there
            stream.addToPlayList(strURL);
        }

        return stream;
    }

    private ScheduleItem parseItem(JSONObject item) throws JSONException {

        ScheduleItem scheduleitem = new ScheduleItem();

        String title = item.getString("title"); // mandatory
        String genre = item.getString("genre");
        String logo = item.getString("logo");
        String time = item.getString("time");

        scheduleitem.setTitle(title);
        scheduleitem.setGenre(genre);
        scheduleitem.setLogoURL(logo);
        scheduleitem.setTime(time);

        JSONArray streams = item.getJSONArray("streams");  // mandatory that a schedule item has at least one stream

        if (streams.length() <= 0) {
            throw new JSONException("streams should be a json array with at least one item");
        }

        for (int i = 0; i < streams.length(); i++) {
            JSONObject streamNode = streams.getJSONObject(i);
            Stream stream = parseStream(streamNode);
            scheduleitem.AddStream(stream);
        }

        return scheduleitem;
    }

    public ArrayList<ScheduleItem> extractScheduleFromJson(String str) throws JSONException {
        JSONObject root = new JSONObject(str);

        ArrayList<ScheduleItem> scheduleItems = new ArrayList<ScheduleItem>();

        JSONArray items = root.getJSONArray("items");

        if (items.length() <= 0) {
            throw new JSONException("items should be a json array with at least one item");
        }

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            ScheduleItem scheduleItem = parseItem(item);
            scheduleItems.add(scheduleItem);
        }

        return scheduleItems;
    }
}


/*
{"items": [
    {
        "streams": [
            {
                "playlist": ["http://streaming.wrek.org:8000/wrek_live-24kb-mono"],
                "bitrate": 24,
                "metaint": true,
                "live": true,
                "url_m3u": "http://streaming.wrek.org:8000/wrek_live-24kb-mono.m3u"
            },
            {
                "playlist": ["http://streaming.wrek.org:8000/wrek_live-128kb"],
                "bitrate": 128,
                "metaint": true,
                "live": true,
                "url_m3u": "http://streaming.wrek.org:8000/wrek_live-128kb.m3u"
            }
        ],
        "genre": "What's on the air right now",
        "logo": "http://www.selbie.com/wrek/radio2.png",
        "time": "",
        "title": "Live air stream"
    },
    {
        "streams": [{
            "playlist": ["http://streaming.wrek.org:8000/wrek_HD-2"],
            "bitrate": 128,
            "metaint": true,
            "live": true,
            "url_m3u": "http://streaming.wrek.org:8000/wrek_HD-2.m3u"
        }],
        "genre": "Alternate programming",
        "logo": "http://www.selbie.com/wrek/hd.png",
        "time": "",
        "title": "HD2 Subchannel"
    },
    {
        "streams": [
            {
                "playlist": [
                    "http://archive.wrek.org/main/24kb/Fri0000.mp3",
                    "http://archive.wrek.org/main/24kb/Fri0030.mp3",
                    "http://archive.wrek.org/main/24kb/Fri0100.mp3",
                    "http://archive.wrek.org/main/24kb/Fri0130.mp3",
                    "http://archive.wrek.org/main/24kb/Fri0200.mp3",
                    "http://archive.wrek.org/main/24kb/Fri0230.mp3"
                ],
                "bitrate": 24,
                "live": false,
                "url_m3u": "http://www.wrek.org/playlist.php/main/24kbs/current/ATM_50000.m3u"
            },
            {
                "playlist": [
                    "http://archive.wrek.org/main/128kb/Fri0000.mp3",
                    "http://archive.wrek.org/main/128kb/Fri0030.mp3",
                    "http://archive.wrek.org/main/128kb/Fri0100.mp3",
                    "http://archive.wrek.org/main/128kb/Fri0130.mp3",
                    "http://archive.wrek.org/main/128kb/Fri0200.mp3",
                    "http://archive.wrek.org/main/128kb/Fri0230.mp3"
                ],
                "bitrate": 128,
                "live": false,
                "url_m3u": "http://www.wrek.org/playlist.php/main/128kbs/current/ATM_50000.m3u"
            }
        ],
        "genre": "Ambient, drone, spaced-out",
        "logo": "http://www.selbie.com/wrek/atmospherics.png",
        "time": "Fri 2/19 12:00 AM",
        "title": "Atmospherics"
    },


 */