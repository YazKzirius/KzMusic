package com.example.kzmusic;

//Imports
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//This class manages retrofit api calls from Spotify's API
public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String accessToken) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            return chain.proceed(request);
        }).build();

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.spotify.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}
