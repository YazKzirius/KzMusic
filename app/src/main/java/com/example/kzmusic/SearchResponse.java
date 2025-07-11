package com.example.kzmusic;

//Imports
import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.List;

//This class manages the information retrieved from API calls
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
    //Stores information about track
    public class Track {
        @SerializedName("name")
        private String name;

        @SerializedName("artists")
        private List<Artist> artists;

        @SerializedName("album")
        private Album album;
        @SerializedName("uri")
        private String uri;


        public String getName() {
            return name;
        }

        public List<Artist> getArtists() {
            return artists;
        }

        public Album getAlbum() {
            return album;
        }
        public String getUri() { return uri;}

    }
    //Stores information about Artist
    public class Artist {
        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }
    }
    //Stores information about Album
    public class Album {
        @SerializedName("images")
        private List<Image> images;
        String name;
        String uri;
        private String release_date; // Release date as a string
        private String release_date_precision; // Precision of the release date (year, month, day)

        public List<Image> getImages() {
            return images;
        }
        public String getName() { return name;}

        public String getUri() {
            return uri;
        }

        public String getRelease_date() {
            return release_date;
        }

        public String getRelease_date_precision() {
            return release_date_precision;
        }
    }
    //Stores information about album image
    public class Image {
        @SerializedName("url")
        private String url;

        public String getUrl() {
            return url;
        }
    }
}