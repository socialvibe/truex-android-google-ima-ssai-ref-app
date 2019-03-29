package com.truex.googlereferenceapp.player;

import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;

/**
 * This class simply listens for playback events and informs the listeners when any playback events
 * occur. Additionally, this class cancels the video stream when and if any playback errors occur.
 */
public class PlayerEventListener extends Player.DefaultEventListener {
    private static final String CLASSTAG = PlayerEventListener.class.getSimpleName();

    private PlaybackHandler playbackHandler;
    private PlaybackStateListener listener;
    private boolean playbackDidStart;

    public PlayerEventListener(PlaybackHandler playbackHandler, PlaybackStateListener listener) {
        this.playbackHandler = playbackHandler;
        this.listener = listener;
        playbackDidStart = false;
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.d(CLASSTAG, "onPlayerError");
        playbackHandler.closeStream();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            listener.onPlayerDidComplete();
        } else if (playWhenReady && playbackState == Player.STATE_READY) {
            if (!playbackDidStart) {
                listener.onPlayerDidStart();
                playbackDidStart = true;
            } else {
                listener.onPlayerDidResume();
            }
        } else if (!playWhenReady && playbackState == Player.STATE_IDLE) {
            listener.onPlayerDidPause();
        }
    }
}
