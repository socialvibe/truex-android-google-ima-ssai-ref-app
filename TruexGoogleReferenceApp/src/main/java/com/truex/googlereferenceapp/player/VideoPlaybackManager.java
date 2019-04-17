package com.truex.googlereferenceapp.player;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdProgressInfo;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.CompanionAd;
import com.google.ads.interactivemedia.v3.api.CuePoint;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.StreamDisplayContainer;
import com.google.ads.interactivemedia.v3.api.StreamManager;
import com.google.ads.interactivemedia.v3.api.StreamRequest;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.ads.interactivemedia.v3.api.player.VideoStreamPlayer;
import com.truex.adrenderer.TruexAdRendererConstants;
import com.truex.googlereferenceapp.home.StreamConfiguration;
import com.truex.googlereferenceapp.player.ads.TruexAdManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoPlaybackManager implements PlaybackHandler, AdEvent.AdEventListener, AdErrorEvent.AdErrorListener, AdsLoader.AdsLoadedListener {
    private static final String CLASSTAG = VideoPlaybackManager.class.getSimpleName();

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
    private Listener listener;

    // The renderer that drives the true[X] Engagement experience
    private TruexAdManager truexAdManager;

    /**
     * Creates a new VideoPlaybackManager that implements IMA direct-ad-insertion.
     * @param context the app's context.
     * @param videoPlayer the underlying video player.
     * @param adUiContainer ViewGroup in which to display the ad's UI.
     */
    VideoPlaybackManager(Context context, VideoPlayer videoPlayer,
                                StreamConfiguration streamConfiguration,
                                ViewGroup adUiContainer) {
        this.videoPlayer = videoPlayer;
        this.streamConfiguration = streamConfiguration;
        this.context = context;
        this.adUiContainer = adUiContainer;
        this.playerCallbacks = new ArrayList<>();
        this.sdkFactory = ImaSdkFactory.getInstance();
        this.displayContainer = sdkFactory.createStreamDisplayContainer();
        this.adsLoader = sdkFactory.createAdsLoader(context, sdkFactory.createImaSdkSettings(), displayContainer);
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

        // Clean-up the display container
        if (displayContainer != null) {
            displayContainer.destroy();
            displayContainer = null;
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
     * Set the listener
     * @param listener the listener
     */
    void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Creates a Stream Request from the requested stream configuration
     * This method also sets up the Display Container for video playback
     * @return the new Stream Request that will be used to begin playback
     */
    private StreamRequest buildStreamRequest() {
        // Set-up the video stream player
        VideoStreamPlayer videoStreamPlayer = createVideoStreamPlayer();
        videoPlayer.setCallback(
                new VideoPlayerCallback() {
                    @Override
                    public void onUserTextReceived(String userText) {
                        for (VideoStreamPlayer.VideoStreamPlayerCallback callback :
                                playerCallbacks) {
                            callback.onUserTextReceived(userText);
                        }
                    }
                    @Override
                    public void onSeek(int windowIndex, long milliseconds) {
                        SeekPosition seekPosition = SeekPosition.fromMilliseconds(milliseconds);
                        seekTo(windowIndex, seekPosition);
                    }
                });

        // Set-up the display container
        displayContainer.setVideoStreamPlayer(videoStreamPlayer);
        displayContainer.setAdContainer(adUiContainer);

        // Create the stream request
        return sdkFactory.createVodStreamRequest(
                streamConfiguration.getContentID(),
                streamConfiguration.getVideoID(),
                null);
    }

    /**
     * Seeks to the provided seek position while respecting any ad pods within the stream
     * @param windowIndex the window index, within the stream, that we will be seeking to
     * @param seekPosition the position, within the stream, that we will be seeking to
     */
    private void seekTo(int windowIndex, SeekPosition seekPosition) {
        // See if we would seek past an ad, and if so, jump back to it.
        SeekPosition newSeekPosition = seekPosition;
        if (streamManager != null) {
            CuePoint prevCuePoint  = streamManager.getPreviousCuePointForStreamTime(seekPosition.getSeconds());
            if (prevCuePoint != null && !prevCuePoint.isPlayed()) {
                newSeekPosition = SeekPosition.fromSeconds(prevCuePoint.getStartTime());
            }
        }
        videoPlayer.seekTo(windowIndex, newSeekPosition);
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
        videoPlayer.seekTo(seekPosition);
    }

    /**
     * Handles the ad started event
     * If the ad is a true[X] placeholder ad, we will display an interactive true[X] ad
     * Additionally, if the ad is a true[X] placeholder ad, we will seek past this initial ad
     * @param event the ad started event object
     */
    private void onAdStarted(AdEvent event) {
        Ad ad = event.getAd();

        // [1]
        // Retrieve the true[X] companion ad - if it exists
        CompanionAd truexCompanionAd = getTruexCompanionAd(ad);
        if (truexCompanionAd == null) {
            return;
        }

        // Retrieve the ad pod info
        AdPodInfo adPodInfo = ad.getAdPodInfo();
        if (adPodInfo == null) {
            return;
        }

        // [2]
        // The companion ad contains a URL (the "static resource URL") which contains base64 encoded ad parameters
        JSONObject adParameters = getJSONObjectFromBase64DataURL(truexCompanionAd.getResourceValue());
        if (adParameters == null) {
            return;
        }

        // [3]
        // Pause the underlying stream, in order to present the true[X] experience, and seek over the current ad,
        // which is just a placeholder for the true[X] ad.
        videoPlayer.pause();
        videoPlayer.hide();
        seekPastInitialAd(ad, adPodInfo);

        // [4]
        // Start the true[X] engagement
        String slotType = adPodInfo.getPodIndex() == 0 ? TruexAdRendererConstants.PREROLL : TruexAdRendererConstants.MIDROLL;
        truexAdManager = new TruexAdManager(context, this);
        truexAdManager.startAd(adUiContainer, adParameters, slotType);
    }

    /**
     * Get the JSON object from a base64 data JSON URL
     * @param dataUrl the base64 data JSON URL
     * @return the decoded JSON object
     */
    private JSONObject getJSONObjectFromBase64DataURL(String dataUrl) {
        try {
            String base64String = dataUrl.replace("data:application/json;base64,", "");
            byte[] base64Decoded = Base64.decode(base64String, Base64.DEFAULT);
            String json = new String(base64Decoded, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception e) {
            Log.d(CLASSTAG, "Failed to parse base64 data URL: " + dataUrl);
        }
        return null;
    }

    /**
     * Retrieves the true[X] companion ad - if the provided ad contains one
     * @param ad the ad object
     * @return a true[X] companion ad or null if none exist
     */
    private CompanionAd getTruexCompanionAd(Ad ad) {
        if (ad == null || ad.getCompanionAds() == null || ad.getCompanionAds().isEmpty()) {
            return null;
        }

        for (CompanionAd companionAd : ad.getCompanionAds()) {
            if ("truex".equals(companionAd.getApiFramework())) {
                return companionAd;
            }
        }
        return null;
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
                videoPlayer.setStreamURL(url);
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

                // Re-enable player controls
                videoPlayer.enableControls(true);
            }

            @Override
            public VideoProgressUpdate getContentProgress() {
                return new VideoProgressUpdate(videoPlayer.getCurrentPositionPeriod(),
                        videoPlayer.getDuration());
            }

            public void onAdPeriodStarted() {
                Log.i(CLASSTAG, "Ad Period Started");
            }

            public void onAdPeriodEnded() {
                Log.i(CLASSTAG, "Ad Period Ended");
            }

            public void seek(long milliseconds) {
                Log.i(CLASSTAG, "Seek Called");

                // Seek to the given seek position
                SeekPosition seekPosition = SeekPosition.fromMilliseconds(milliseconds);
                videoPlayer.seekTo(seekPosition);
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
        videoPlayer.display();
        videoPlayer.play();
    }

    @Override
    public void skipCurrentAdBreak() {
        Ad ad = streamManager.getCurrentAd();
        if (ad == null) {
            return;
        }

        AdPodInfo adPodInfo = ad.getAdPodInfo();
        if (adPodInfo == null) {
            return;
        }

        AdProgressInfo adProgressInfo = streamManager.getAdProgressInfo();
        if (adProgressInfo == null) {
            return;
        }

        // Set-up the initial offset for seeking past the ad break
        SeekPosition seekPosition = SeekPosition.fromSeconds(adPodInfo.getTimeOffset());

        // Add the duration of the ad break
        seekPosition.addSeconds(adProgressInfo.getAdBreakDuration());

        // Add three hundred milliseconds to avoid displaying a black screen with a frozen UI
        seekPosition.addMilliseconds(300);

        // Seek past the ad break
        videoPlayer.seekTo(seekPosition);
    }

    /** AdErrorListener implementation **/

    @Override
    public void onAdError(AdErrorEvent event) {
        Log.i(CLASSTAG, String.format("Ad Error: %s", event.getError().getMessage()));
        listener.onVideoPlaybackFailed();
    }

    /** AdEventListener implementation **/

    @Override
    public void onAdEvent(AdEvent event) {
        if (event.getType() == AdEvent.AdEventType.AD_PROGRESS) {
            return;
        }

        Log.i(CLASSTAG, String.format("Event: %s", event.getType()));
        switch (event.getType()) {
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

    public interface Listener {
        void onVideoPlaybackFailed();
    }
}

