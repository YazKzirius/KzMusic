package com.example.kzmusic;
//Imports
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

//Spotify API service interface
public interface SpotifyApiService {
    @Headers("Authorization: Bearer {token}")
    @GET("v1/search")
    Call<SearchResponse> searchTracks(@Query("q") String query, @Query("type") String type);
}
