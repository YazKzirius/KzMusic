package com.example.kzmusic;
import android.os.Parcel;
import android.os.Parcelable;
//This class handles Music file storage for further manipulation
public class MusicFile implements Parcelable{
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

    // Getters and setter methods
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
    // Parcelable implementation
    protected MusicFile(Parcel in) {
        name = in.readString();
        artist = in.readString();
        path = in.readString();
        albumId = in.readLong();
    }

    public static final Creator<MusicFile> CREATOR = new Creator<MusicFile>() {
        @Override
        public MusicFile createFromParcel(Parcel in) {
            return new MusicFile(in);
        }

        @Override
        public MusicFile[] newArray(int size) {
            return new MusicFile[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(artist);
        dest.writeString(path);
        dest.writeLong(albumId);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
