package com.example.kzmusic;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Event<Boolean>> skipEvent = new MutableLiveData<>();

    public void triggerSkipEvent() {
        skipEvent.setValue(new Event<>(true));
    }

    public LiveData<Event<Boolean>> getSkipEvent() {
        return skipEvent;
    }
}