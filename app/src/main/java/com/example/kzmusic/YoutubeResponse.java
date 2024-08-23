package com.example.kzmusic;

import com.google.gson.annotations.SerializedName;

public class YoutubeResponse {
    @SerializedName("items")
    public Item[] items;

    public static class Item {
        @SerializedName("id")
        public Id id;

        public static class Id {
            @SerializedName("videoId")
            public String videoId;
        }
    }
}
