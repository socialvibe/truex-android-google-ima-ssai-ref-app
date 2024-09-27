package com.truex.googlereferenceapp.player;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import androidx.media3.ui.PlayerView;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdProgressInfo;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.CuePoint;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.StreamDisplayContainer;
import com.google.ads.interactivemedia.v3.api.StreamManager;
import com.google.ads.interactivemedia.v3.api.StreamRequest;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.ads.interactivemedia.v3.api.player.VideoStreamPlayer;
import com.truex.googlereferenceapp.home.StreamConfiguration;
import com.truex.googlereferenceapp.player.ads.TruexAdManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoPlayerWithAds implements PlaybackHandler, AdEvent.AdEventListener, AdErrorEvent.AdErrorListener, AdsLoader.AdsLoadedListener {
    private static final String CLASSTAG = VideoPlayerWithAds.class.getSimpleName();

    // The stream configuration for the selected content
    // The Video ID and Content ID are used to initialize the stream with the IMA SDK
    // These values should be set based on your stream
    private StreamConfiguration streamConfiguration;

    // These properties allow us to do the basic work of playing back ad-stitched video
    private Context context;
    private ViewGroup adUiContainer;
    private VideoPlayer videoPlayer;
    private ImaSdkFactory sdkFactory;
    private AdsLoader adsLoader;
    private StreamDisplayContainer displayContainer;
    private StreamManager streamManager;
    private List<VideoStreamPlayer.VideoStreamPlayerCallback> playerCallbacks;

    private long resumePositionAfterSnapbackMs; // Stream time to snap back to, in milliseconds.

    private boolean didSeekPastAdBreak;

    // The renderer that drives the true[X] Engagement experience
    private TruexAdManager truexAdManager;

    /**
     * Creates a new VideoPlaybackManager that implements IMA direct-ad-insertion.
     * @param context the app's context.
     * @param playerView the playerview videos will be displayed in
     * @param adUiContainer ViewGroup in which to display the ad's UI.
     */
    VideoPlayerWithAds(Context context,
                       StreamConfiguration streamConfiguration,
                       PlayerView playerView,
                       ViewGroup adUiContainer) {
        this.videoPlayer = new VideoPlayer(context, playerView);
        this.streamConfiguration = streamConfiguration;
        this.context = context;
        this.adUiContainer = adUiContainer;
        this.playerCallbacks = new ArrayList<>();
        this.sdkFactory = ImaSdkFactory.getInstance();
        ImaSdkSettings settings = sdkFactory.createImaSdkSettings();
        VideoStreamPlayer videoStreamPlayer = createVideoStreamPlayer();
        this.displayContainer = ImaSdkFactory.createStreamDisplayContainer(adUiContainer, videoStreamPlayer);
        videoPlayer.setCallback(
                new VideoPlayerCallback() {
                    @Override
                    public void onUserTextReceived(String userText) {
                        for (VideoStreamPlayer.VideoStreamPlayerCallback callback : playerCallbacks) {
                            callback.onUserTextReceived(userText);
                        }
                    }

                    @Override
                    public void onSeek(int windowIndex, long streamPositionMs) {
                        long allowedPositionMs = streamPositionMs;
                        if (streamManager != null) {
                            CuePoint cuePoint = streamManager.getPreviousCuePointForStreamTimeMs(streamPositionMs);
                            if (cuePoint != null && !cuePoint.isPlayed()) {
                                resumePositionAfterSnapbackMs = streamPositionMs; // Update snap back time.
                                // Missed cue point, so snap back to the beginning of cue point.
                                allowedPositionMs = cuePoint.getStartTimeMs();
                                Log.i(CLASSTAG, "Ad snapback to " + VideoPlayer.positionDisplay(allowedPositionMs)
                                        + " for " + VideoPlayer.positionDisplay(streamPositionMs));
                                videoPlayer.seekTo(windowIndex, allowedPositionMs);
                                videoPlayer.setCanSeek(false);
                                return;
                            }
                        }
                        videoPlayer.seekTo(windowIndex, allowedPositionMs);
                    }
                });
        adsLoader = sdkFactory.createAdsLoader(context, settings, displayContainer);
    }

    /**
     * Builds the stream request and begins playback of the requested stream
     */
    void requestAndPlayStream() {
        // Enable controls for the video player
        videoPlayer.enableControls(true);

        // Request the stream
        adsLoader.addAdErrorListener(this);
        adsLoader.addAdsLoadedListener(this);
        adsLoader.requestStream(buildStreamRequest());
    }

    /**
     * Destroys and releases the video player and stream manager
     */
    void release() {

        // Clean-up the true[X] ad manager
        if (truexAdManager != null) {
            truexAdManager.stop();
            truexAdManager = null;
        }

        // Clean-up the stream manager
        if (streamManager != null) {
            streamManager.destroy();
            streamManager = null;
        }

        // Clean-up the video player
        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }

        if (adsLoader != null) {
            adsLoader.release();
            adsLoader = null;
        }
    }

    /**
     * Resumes playback of the video player
     */
    void resume() {
        // Resume the current ad -- if active
        if (truexAdManager != null) {
            truexAdManager.resume();
            return;
        }

        // Resume video playback
        videoPlayer.requestFocus();
        if (videoPlayer != null && videoPlayer.isStreamRequested()) {
            videoPlayer.play();
        }
    }

    /**
     * Pauses playback of the video player
     */
    void pause() {
        // Pause the current ad -- if active
        if (truexAdManager != null) {
            truexAdManager.pause();
            return;
        }

        // Pause video playback
        if (videoPlayer != null && videoPlayer.isStreamRequested()) {
            videoPlayer.pause();
        }
    }

    /**
     * Creates a Stream Request from the requested stream configuration
     * This method also sets up the Display Container for video playback
     * @return the new Stream Request that will be used to begin playback
     */
    private StreamRequest buildStreamRequest() {
        // Create the stream request
        return sdkFactory.createVodStreamRequest(
                streamConfiguration.getContentID(),
                streamConfiguration.getVideoID(),
                null);
    }

    /**
     * Seeks past the first ad within the current ad pod
     * @param ad the first ad within the current ad pod
     * @param adPodInfo the ad pod info object for the current ad pod
     */
    private void seekPastInitialAd(Ad ad, AdPodInfo adPodInfo) {
        // Set-up the initial offset for seeking past the initial ad
        SeekPosition seekPosition = SeekPosition.fromSeconds(adPodInfo.getTimeOffset());

        // Add the duration of the initial ad
        seekPosition.addSeconds(ad.getDuration());

        // Subtract a hundred milliseconds to avoid displaying a black screen with a frozen UI
        seekPosition.subtractMilliseconds(100);

        // Seek past the ad
        videoPlayer.seekTo(seekPosition.getMilliseconds());
    }

    /**
     * Handles the ad started event
     * If the ad is a true[X] placeholder ad, we will display an interactive true[X] ad
     * Additionally, if the ad is a true[X] placeholder ad, we will seek past this initial ad
     * @param event the ad started event object
     */
    private void onAdStarted(AdEvent event) {
        Ad ad = event.getAd();

        if (!"xtrueX".equals(ad.getAdSystem())) return; // not a trueX ad

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
        videoPlayer.pause();
        videoPlayer.hide();
        seekPastInitialAd(ad, adPodInfo);

        // [4]
        // Start the true[X] engagement
        truexAdManager = new TruexAdManager(context, this);
        truexAdManager.startAd(adUiContainer, vastConfigUrl);
    }

    /**
     * Creates a video stream player object wrapping the video player
     * The video stream player API is used by the IMA SDK to interact
     * with the video player and allows the video player to respond
     * to ad events - such as the beginning or end of an ad pod.
     * @return a video stream player wrapping the video player
     */
    private VideoStreamPlayer createVideoStreamPlayer() {
        return new VideoStreamPlayer() {
            @Override
            public void loadUrl(String url, List<HashMap<String, String>> subtitles) {
                videoPlayer.setStreamUrl(url);
                videoPlayer.play();
            }

            public void pause() {
                videoPlayer.pause();
            }

            public void resume() {
                videoPlayer.play();
            }

            @Override
            public int getVolume() {
                return videoPlayer.getVolume();
            }

            @Override
            public void addCallback(VideoStreamPlayerCallback videoStreamPlayerCallback) {
                playerCallbacks.add(videoStreamPlayerCallback);
            }

            @Override
            public void removeCallback(VideoStreamPlayerCallback videoStreamPlayerCallback) {
                playerCallbacks.remove(videoStreamPlayerCallback);
            }

            @Override
            public void onAdBreakStarted() {
                Log.i(CLASSTAG, "Ad Break Started");

                // Disable player controls
                videoPlayer.enableControls(false);
            }

            @Override
            public void onAdBreakEnded() {
                Log.i(CLASSTAG, "Ad Break Ended");

                if (resumePositionAfterSnapbackMs > 0) {
                    videoPlayer.seekTo(resumePositionAfterSnapbackMs);
                }
                resumePositionAfterSnapbackMs = 0;

                videoPlayer.refreshAdMarkers();

                // Re-enable player controls
                videoPlayer.enableControls(true);
            }

            @Override
            public VideoProgressUpdate getContentProgress() {
                return new VideoProgressUpdate(videoPlayer.getCurrentPositionMs(),
                        videoPlayer.getDuration());
            }

            public void onAdPeriodStarted() {
                Log.i(CLASSTAG, "Ad Period Started");
            }

            public void onAdPeriodEnded() {
                Log.i(CLASSTAG, "Ad Period Ended");
            }

            public void seek(long milliseconds) {
                videoPlayer.seekTo(milliseconds);
            }
        };
    }

    /** PlaybackHandler implementation **/

    @Override
    public void resumeStream() {
        Log.i(CLASSTAG, "Resume Stream Called");

        // Remove the true[X] ad manager reference
        truexAdManager = null;

        // Display and resume the stream
        videoPlayer.show();
        videoPlayer.play();

        if (didSeekPastAdBreak) {
            // Manually call onAdBreakEnded() now
            didSeekPastAdBreak = false;
            VideoStreamPlayer streamPlayer = displayContainer != null ? displayContainer.getVideoStreamPlayer() : null;
            if (streamPlayer != null) {
                streamPlayer.onAdBreakEnded();
            }
        }
    }

    @Override
    public void skipCurrentAdBreak() {
        // Retrieve current ad
        Ad ad = streamManager.getCurrentAd();
        if (ad == null) {
            return;
        }

        // Retrieve ad pod info
        AdPodInfo adPodInfo = ad.getAdPodInfo();
        if (adPodInfo == null) {
            return;
        }

        // Retrieve ad progress info
        AdProgressInfo adProgressInfo = streamManager.getAdProgressInfo();
        if (adProgressInfo == null) {
            return;
        }

        // Set-up the initial offset for seeking past the ad break
        SeekPosition seekPosition = SeekPosition.fromSeconds(adPodInfo.getTimeOffset());

        // Add the duration of the ad break
        seekPosition.addSeconds(adProgressInfo.getAdBreakDuration());

        // Add two seconds to avoid displaying a frozen UI
        seekPosition.addSeconds(2);

        // Seek past the ad break
        videoPlayer.seekTo(seekPosition.getMilliseconds());

        // We will need to manually call onAdBreakEnded() when we resume the stream
        didSeekPastAdBreak = true;
    }

    /** AdErrorListener implementation **/

    @Override
    public void onAdError(AdErrorEvent event) {
        Log.i(CLASSTAG, String.format("Ad Error: %s", event.getError().getMessage()));
        this.release();
    }

    /** AdEventListener implementation **/

    @Override
    public void onAdEvent(AdEvent event) {
        if (event.getType() == AdEvent.AdEventType.AD_PROGRESS) {
            return;
        }

        Log.i(CLASSTAG, String.format("Event: %s", event.getType()));
        switch (event.getType()) {
            case CUEPOINTS_CHANGED:
                videoPlayer.setAdsTimeline(streamManager);
                break;
            case STARTED:
                onAdStarted(event);
                break;
        }
    }

    /** AdsLoadedListener implementation **/

    @Override
    public void onAdsManagerLoaded(AdsManagerLoadedEvent event) {
        streamManager = event.getStreamManager();

        // Create the ads rendering settings
        AdsRenderingSettings adsRenderingSettings = sdkFactory.createAdsRenderingSettings();
        adsRenderingSettings.setFocusSkipButtonWhenAvailable(true);

        // Initialize the stream manager
        streamManager.addAdErrorListener(this);
        streamManager.addAdEventListener(this);
        streamManager.init(adsRenderingSettings);
    }
}

