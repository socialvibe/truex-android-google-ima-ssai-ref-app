package com.truex.googlereferenceapp.player;

import static androidx.media3.common.C.CONTENT_TYPE_HLS;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource;
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionUriBuilder;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdProgressInfo;
import com.truex.googlereferenceapp.R;
import com.truex.googlereferenceapp.home.StreamConfiguration;
import com.truex.googlereferenceapp.player.ads.TruexAdManager;

@OptIn(markerClass = UnstableApi.class)
public class PlayerViewFragment extends Fragment implements PlaybackHandler {
    private static final String CLASSTAG = VideoPlayer.class.getSimpleName();

    // The stream configuration for the selected content
    // The Video ID and Content ID are used to initialize the stream with the IMA SDK
    // These values should be set based on your stream
    private StreamConfiguration streamConfiguration;

    private ExoPlayer player;
    private PlayerView playerView;
    private ImaServerSideAdInsertionMediaSource.AdsLoader adsLoader;

    // The renderer that drives the true[X] Engagement experience
    private TruexAdManager truexAdManager;

    private long seekPositionAfterAdBreak;
    private long seekPositionOnResume;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        playerView = getView().findViewById(R.id.player_view);

        // Retrieve the stream configuration from the arguments bundle
        this.streamConfiguration = getArguments().getParcelable(StreamConfiguration.class.getSimpleName());

        initializePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        playerView.onPause();
        if (player != null) player.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        playerView.onResume();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }

    private void initializePlayer() {
        if (player != null) return;

        Context context = getContext();

        // Create a server side ad insertion (SSAI) AdsLoader.
        ImaServerSideAdInsertionMediaSource.AdsLoader.Builder adsLoaderBuilder =
                new ImaServerSideAdInsertionMediaSource.AdsLoader.Builder(context, playerView);
        adsLoader = adsLoaderBuilder
                .setAdEventListener(event -> onAdEvent(event))
                .setAdErrorListener(event -> onAdError(event))
                .build();

        // Set up the factory for media sources, passing the ads loader.
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

        // MediaSource.Factory to create the ad sources for the current player.
        ImaServerSideAdInsertionMediaSource.Factory adsMediaSourceFactory =
                new ImaServerSideAdInsertionMediaSource.Factory(adsLoader, mediaSourceFactory);

        // 'mediaSourceFactory' is an ExoPlayer component for the DefaultMediaSourceFactory.
        // 'adsMediaSourceFactory' is an ExoPlayer component for a MediaSource factory for IMA server
        // side inserted ad streams.
        mediaSourceFactory.setServerSideAdInsertionMediaSourceFactory(adsMediaSourceFactory);

        // Create a SimpleExoPlayer and set it as the player for content and ads.
        player = new ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build();
        playerView.setPlayer(player);
        adsLoader.setPlayer(player);

        // Build an IMA SSAI media item to prepare the player with.
        Uri ssaiLiveUri =
                new ImaServerSideAdInsertionUriBuilder()
                        .setContentSourceId(streamConfiguration.getContentID())
                        .setVideoId(streamConfiguration.getVideoID())
                        .setFormat(CONTENT_TYPE_HLS) // Use CONTENT_TYPE_DASH for dash streams.
                        .build();

        // Create the MediaItem to play, specifying the stream URI.
        MediaItem ssaiMediaItem = MediaItem.fromUri(ssaiLiveUri);

        // Prepare the content and ad to be played with the ExoPlayer.
        player.setMediaItem(ssaiMediaItem);
        player.prepare();

        // Start playing content.
        player.setPlayWhenReady(true);
    }

    private void release() {
        playerView.setPlayer(null);

        // Clean-up the true[X] ad manager
        if (truexAdManager != null) {
            truexAdManager.stop();
            truexAdManager = null;
        }

        if (adsLoader != null) {
            adsLoader.release();
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void onAdError(AdErrorEvent event) {
        Log.i(CLASSTAG, String.format("Ad Error: %s", event.getError().getMessage()));
        this.release();
    }

    private void onAdEvent(AdEvent event) {
        AdEvent.AdEventType eventType = event.getType();
        if (eventType == AdEvent.AdEventType.AD_PROGRESS) return;
        Log.i(CLASSTAG, "IMA Ad Event: " + eventType);
        switch (eventType) {
            case STARTED:
                onAdStarted(event);
                break;
        }

    }

    /**
     * Handles the ad started event
     * If the ad is a true[X] placeholder ad, we will display an interactive true[X] ad
     * Additionally, if the ad is a true[X] placeholder ad, we will seek past this initial ad
     * @param event the ad started event object
     */
    private void onAdStarted(AdEvent event) {
        Ad ad = event.getAd();

        if (!"trueX".equals(ad.getAdSystem())) return; // not a trueX ad

        // Retrieve the ad pod info
        AdPodInfo adPodInfo = ad.getAdPodInfo();
        if (adPodInfo == null) return;

        // [2]
        // The ad description contains the trueX vast config url
        String vastConfigUrl = ad.getDescription();
        if (vastConfigUrl == null || !vastConfigUrl.contains("get.truex.com")) return; // invalid vast config url

        // [3]
        // Pause the underlying stream, in order to present the true[X] experience, and seek over the current ad,
        // which is just a placeholder for the true[X] ad.
        player.pause();
        playerView.setVisibility(View.GONE);

        // Ensure we will at least skip the truex ad or the ad break once we are done.

        // By default we will seek past the initial trueX ad to the fallback ad videos.
        SeekPosition seekPosition = SeekPosition.fromSeconds(adPodInfo.getTimeOffset());
        seekPosition.addSeconds(ad.getDuration());
        seekPosition.subtractMilliseconds(100); // back a bit to skip a black screen with frozen UI
        seekPositionOnResume = seekPosition.getMilliseconds();

        // We also want to might skip past the ad break if the user gets the credit.
        seekPosition = SeekPosition.fromSeconds(adPodInfo.getTimeOffset());
        seekPosition.addSeconds(adPodInfo.getMaxDuration());
        seekPosition.addSeconds(2); // skip a bit to avoid any frozen UI
        seekPositionAfterAdBreak = seekPosition.getMilliseconds();

        // [4]
        // Start the true[X] engagement
        ViewGroup adUiContainer = getView().findViewById(R.id.ad_ui_container);
        truexAdManager = new TruexAdManager(getContext(), this);
        truexAdManager.startAd(adUiContainer, vastConfigUrl);
    }

    @Override
    public void skipCurrentAdBreak() {
        seekPositionOnResume = seekPositionAfterAdBreak;
    }

    @Override
    public void resumeStream() {
        if (player == null) return;
        playerView.setVisibility(View.VISIBLE);
        player.seekTo(seekPositionOnResume);
        player.setPlayWhenReady(true);
    }
}
