package com.example.kzmusic;

public interface TokenRefreshCallback {
    void onTokenRefreshed(String newAccessToken);
    void onTokenRefreshFailed();
}
