package com.example.kzmusic;

public interface TokenCallback {
    void onSuccess(String newAccessToken);
    void onError(Exception e);
}
