package com.example.kzmusic;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchResponse {
    @SerializedName("tracks")
    private Tracks tracks;

    public Tracks getTracks() {
        return tracks;
    }

    public class Tracks {
        @SerializedName("items")
        private List<Track> items;

        public List<Track> getItems() {
            return items;
        }
    }

    public class Track {
        @SerializedName("name")
        private String name;

        @SerializedName("artists")
        private List<Artist> artists;

        @SerializedName("album")
        private Album album;

        public String getName() {
            return name;
        }

        public List<Artist> getArtists() {
            return artists;
        }

        public Album getAlbum() {
            return album;
        }
    }

    public class Artist {
        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }
    }

    public class Album {
        @SerializedName("images")
        private List<Image> images;

        public List<Image> getImages() {
            return images;
        }
    }

    public class Image {
        @SerializedName("url")
        private String url;

        public String getUrl() {
            return url;
        }
    }
}