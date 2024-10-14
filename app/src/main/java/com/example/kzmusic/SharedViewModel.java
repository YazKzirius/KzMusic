package com.example.kzmusic;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Event<Boolean>> skipEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> EndEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> PauseEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> PlayEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> UpdateBar = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> StopBar = new MutableLiveData<>();


    public void triggerSkipEvent() {
        skipEvent.setValue(new Event<>(true));
    }
    public void triggerEndEvent() {
        EndEvent.setValue(new Event<>(true));
    }
    public void triggerPauseEvent() {
        PauseEvent.setValue(new Event<>(true));
    }
    public void triggerPlayEvent() {
        PlayEvent.setValue(new Event<>(true));
    }
    public void triggerUpdateEvent() {
        skipEvent.setValue(new Event<>(true));
    }
    public void triggerStopEvent() {
        skipEvent.setValue(new Event<>(true));
    }

    public LiveData<Event<Boolean>> getSkipEvent() {
        return skipEvent;
    }
    public LiveData<Event<Boolean>> getEndEvent() {
        return EndEvent;
    }
    public LiveData<Event<Boolean>> getPauseEvent() {
        return PauseEvent;
    }
    public LiveData<Event<Boolean>> getPlayEvent() {
        return PlayEvent;
    }
    public LiveData<Event<Boolean>> getUpdateEvent() {
        return skipEvent;
    }
    public LiveData<Event<Boolean>> getStopEvent() {
        return skipEvent;
    }

}