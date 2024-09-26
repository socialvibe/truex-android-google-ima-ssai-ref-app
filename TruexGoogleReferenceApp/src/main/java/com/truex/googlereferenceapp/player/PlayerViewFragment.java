package com.truex.googlereferenceapp.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.media3.ui.PlayerView;

import com.truex.googlereferenceapp.R;
import com.truex.googlereferenceapp.home.StreamConfiguration;

public class PlayerViewFragment extends Fragment {

    protected VideoPlayer videoPlayer;

    protected VideoPlayerWithAds videoPlayerWithAds;

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

        ViewGroup adUiContainer = getView().findViewById(R.id.ad_ui_container);
        PlayerView playerView = getView().findViewById(R.id.player_view);

        videoPlayerWithAds = new VideoPlayerWithAds(getContext(), streamConfiguration, playerView, adUiContainer);
        videoPlayerWithAds.requestAndPlayStream();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the playback
        if (videoPlayerWithAds != null) {
            videoPlayerWithAds.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the playback
        if (videoPlayerWithAds != null) {
            videoPlayerWithAds.resume();
        }
    }

    @Override
    public void onDestroy() {
        cleanUp();
        super.onDestroy();
    }

    private void cleanUp() {
        videoPlayerWithAds.release();
        reset();
    }

    private void reset() {
        videoPlayerWithAds = null;
        videoPlayer = null;
    }
}
