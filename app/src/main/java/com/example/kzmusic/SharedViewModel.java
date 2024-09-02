package com.example.kzmusic;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Boolean> skipEvent = new MutableLiveData<>();


    public void triggerSkipEvent() {
        skipEvent.setValue(true);
    }


    public LiveData<Boolean> getSkipEvent() {
        return skipEvent;
    }
}