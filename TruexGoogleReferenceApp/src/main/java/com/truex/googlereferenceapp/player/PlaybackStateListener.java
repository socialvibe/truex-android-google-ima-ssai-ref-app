package com.truex.googlereferenceapp.player;

public interface PlaybackStateListener {
    void onPlayerDidStart();
    void onPlayerDidResume();
    void onPlayerDidPause();
    void onPlayerDidComplete();
}
