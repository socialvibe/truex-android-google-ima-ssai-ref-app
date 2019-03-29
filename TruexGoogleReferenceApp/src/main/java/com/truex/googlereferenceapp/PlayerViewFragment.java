package com.truex.googlereferenceapp;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import com.truex.googlereferenceapp.ads.TruexAdManager;
import com.truex.googlereferenceapp.player.DisplayMode;
import com.truex.googlereferenceapp.player.PlaybackHandler;
import com.truex.googlereferenceapp.player.PlaybackStateListener;
import com.truex.googlereferenceapp.player.PlayerEventListener;

public class PlayerViewFragment extends Fragment implements PlaybackHandler, PlaybackStateListener {
    private static final String CLASSTAG = PlayerViewFragment.class.getSimpleName();
    private static final String CONTENT_STREAM_URL = "http://media.truex.com/file_assets/2019-01-30/4ece0ae6-4e93-43a1-a873-936ccd3c7ede.mp4";
    private static final String AD_URL_ONE = "http://media.truex.com/file_assets/2019-01-30/eb27eae5-c9da-4a9b-9420-a83c986baa0b.mp4";
    private static final String AD_URL_TWO = "http://media.truex.com/file_assets/2019-01-30/7fe9da33-6b9e-446d-816d-e1aec51a3173.mp4";
    private static final String AD_URL_THREE = "http://media.truex.com/file_assets/2019-01-30/742eb926-6ec0-48b4-b1e6-093cee334dd1.mp4";

    // This player view is used to display a fake stream that mimics actual video content
    private SimpleExoPlayerView playerView;

    // The data-source factory is used to build media-sources
    private DataSource.Factory dataSourceFactory;

    // We need to hold onto the ad manager so that the ad manager can listen for lifecycle events
    private TruexAdManager truexAdManager;

    // We need to identify whether or not the user is viewing ads or the content stream
    private DisplayMode displayMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set-up the video content player
        setupExoPlayer();

        // Set-up the data-source factory
        setupDataSourceFactory();

        // Start the content stream
        displayContentStream();
    }

    @Override
    public void onResume() {
        super.onResume();

        // We need to inform the true[X] ad manager that the application has resumed
        if (truexAdManager != null) {
            truexAdManager.onResume();
        }

        // Resume video playback
        if (playerView.getPlayer() != null && displayMode != DisplayMode.INTERACTIVE_AD) {
            playerView.getPlayer().setPlayWhenReady(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // We need to inform the true[X] ad manager that the application has paused
        if (truexAdManager != null) {
            truexAdManager.onPause();
        }

        // Pause video playback
        if (playerView.getPlayer() != null && displayMode != DisplayMode.INTERACTIVE_AD) {
            playerView.getPlayer().setPlayWhenReady(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // We need to inform the true[X] ad manager that the application has stopped
        if (truexAdManager != null) {
            truexAdManager.onStop();
        }

        // Release the video player
        closeStream();
    }

    /**
     * This method cancels the content stream and releases the video content player
     * Note: We call this method when the application is stopped
     */
    @Override
    public void closeStream() {
        Log.d(CLASSTAG, "cancelStream");
        if (playerView.getPlayer() == null) {
            return;
        }
        SimpleExoPlayer player = playerView.getPlayer();
        playerView.setPlayer(null);
        player.release();
    }

    /**
     * This method resumes and displays the content stream
     * Note: We call this method whenever a true[X] engagement is completed
     */
    @Override
    public void resumeStream() {
        Log.d(CLASSTAG, "resumeStream");
        if (playerView.getPlayer() == null) {
            return;
        }
        playerView.setVisibility(View.VISIBLE);
        playerView.getPlayer().setPlayWhenReady(true);
    }

    /**
     * This method cancels the content stream and begins playing a linear ad
     * Note: We call this method whenever the user cancels an engagement without receiving credit
     */
    @Override
    public void displayLinearAds() {
        Log.d(CLASSTAG, "displayLinearAds");
        if (playerView.getPlayer() == null) {
            return;
        }

        displayMode = DisplayMode.LINEAR_ADS;

        MediaSource[] ads = new MediaSource[3];

        String[] adUrls = {
                AD_URL_ONE, AD_URL_TWO, AD_URL_THREE
        };

        for(int i = 0; i < ads.length; i++) {
            Uri uri = Uri.parse(adUrls[i]);
            MediaSource source = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            ads[i] = source;
        }

        MediaSource adPod = new ConcatenatingMediaSource(ads);
        playerView.getPlayer().prepare(adPod);
        playerView.getPlayer().setPlayWhenReady(true);
        playerView.setVisibility(View.VISIBLE);
    }

    /**
     * Called when the player starts displaying the fake content stream
     * Display the true[X] engagement
     */
    @Override
    public void onPlayerDidStart() {
        displayInteractiveAd();
    }

    /**
     * Called when the media stream is resumed
     */
    @Override
    public void onPlayerDidResume() {
        Log.d(CLASSTAG, "onPlayerDidResume");
    }

    /**
     * Called when the media stream is paused
     */
    @Override
    public void onPlayerDidPause() {
        Log.d(CLASSTAG, "onPlayerDidPause");
    }

    /**
     * Called when the media stream is complete
     */
    @Override
    public void onPlayerDidComplete() {
        if (displayMode == DisplayMode.LINEAR_ADS) {
            displayContentStream();
        }
    }

    /**
     * This method pauses and hides the fake content stream
     * Note: We call this method whenever a true[X] engagement is completed
     */
    private void pauseStream() {
        Log.d(CLASSTAG, "pauseStream");
        if (playerView.getPlayer() == null) {
            return;
        }
        playerView.getPlayer().setPlayWhenReady(false);
        playerView.setVisibility(View.GONE);
    }

    private void displayInteractiveAd() {
        Log.d(CLASSTAG, "displayInteractiveAds");
        if (playerView.getPlayer() == null) {
            return;
        }

        // Pause the stream and display a true[X] engagement
        pauseStream();

        displayMode = DisplayMode.INTERACTIVE_AD;

        // Start the true[X] engagement
        ViewGroup viewGroup = (ViewGroup) getView();
        truexAdManager = new TruexAdManager(getContext(), this);
        truexAdManager.startAd(viewGroup);
    }

    private void displayContentStream() {
        Log.d(CLASSTAG, "displayContentStream");
        if (playerView.getPlayer() == null) {
            return;
        }

        displayMode = DisplayMode.CONTENT_STREAM;

        Uri uri = Uri.parse(CONTENT_STREAM_URL);
        MediaSource source = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        playerView.getPlayer().prepare(source);
        playerView.getPlayer().setPlayWhenReady(true);
    }

    private void setupExoPlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector);

        playerView = getView().findViewById(R.id.player_view);
        playerView.setPlayer(player);

        // Listen for player events so that we can load the true[X] ad manager when the video stream starts
        player.addListener(new PlayerEventListener(this, this));
    }

    private void setupDataSourceFactory() {
        String applicationName = getContext().getApplicationInfo().loadLabel(getContext().getPackageManager()).toString();
        String userAgent = Util.getUserAgent(getContext(), applicationName);
        dataSourceFactory = new DefaultDataSourceFactory(getContext(), userAgent, null);
    }
}
