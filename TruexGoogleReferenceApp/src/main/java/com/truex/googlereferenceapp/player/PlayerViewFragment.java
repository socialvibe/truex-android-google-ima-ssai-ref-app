package com.truex.googlereferenceapp.player;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.truex.googlereferenceapp.R;
import com.truex.googlereferenceapp.home.StreamConfiguration;

public class PlayerViewFragment extends Fragment {

    protected VideoPlayer videoPlayer;

    protected VideoPlaybackManager videoPlaybackManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Retrieve the stream configuration from the arguments bundle
        StreamConfiguration streamConfiguration = getArguments().getParcelable(StreamConfiguration.class.getSimpleName());

        // Retrieve the Ad UI Container ViewGroup
        ViewGroup adUiContainer = getView().findViewById(R.id.ad_ui_container);

        // Set-up the video playback manager
        videoPlayer = new VideoPlayer(getContext(), getView().findViewById(R.id.player_view));
        videoPlaybackManager = new VideoPlaybackManager(getContext(), videoPlayer, streamConfiguration, adUiContainer);
        videoPlaybackManager.setListener(videoPlaybackManager::release);

        // Begin playback of the stream
        videoPlaybackManager.requestAndPlayStream();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the playback
        if (videoPlaybackManager != null) {
            videoPlaybackManager.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the playback
        if (videoPlaybackManager != null) {
            videoPlaybackManager.resume();
        }
    }

    @Override
    public void onDestroy() {
        cleanUp();
        super.onDestroy();
    }

    private void cleanUp() {
        videoPlaybackManager.release();
        reset();
    }

    private void reset() {
        videoPlaybackManager = null;
        videoPlayer = null;
    }
}
