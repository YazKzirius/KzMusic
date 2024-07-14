package com.example.kzmusic;

//This class handles Music file storage for further manipulation
public class MusicFile {
    private long id;
    private String name;
    private String artist;
    private String path;
    private long albumId;
    public MusicFile(long id, String name, String artist, String path, long albumId) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.path = path;
        this.albumId = albumId;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public String getPath() {
        return path;
    }

    public long getAlbumId() {
        return albumId;
    }
}
