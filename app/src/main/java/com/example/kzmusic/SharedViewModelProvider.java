package com.example.kzmusic;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

public class SharedViewModelProvider {
    private static SharedViewModel sharedViewModel;

    public static void initViewModel(ViewModelStoreOwner owner) {
        sharedViewModel = new ViewModelProvider(owner).get(SharedViewModel.class);
    }

    public static SharedViewModel getViewModel(Context context) {
        return sharedViewModel;
    }
}
