package com.truex.googlereferenceapp.player.ads;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.truex.adrenderer.TruexAdEvent;
import com.truex.adrenderer.TruexAdRenderer;
import com.truex.googlereferenceapp.player.PlaybackHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * This class holds a reference to the true[X] ad renderer and handles all of the event handling
 * for the example integration application. This class interacts with the video player by resuming
 * the content when the engagement is complete.
 */
public class TruexAdManager {
    private static final String CLASSTAG = TruexAdManager.class.getSimpleName();

    private PlaybackHandler playbackHandler;
    private TruexAdRenderer truexAdRenderer;
    private boolean didReceiveCredit;

    public TruexAdManager(Context context, PlaybackHandler playbackHandler) {
        this.playbackHandler = playbackHandler;

        // Set-up the true[X] ad renderer
        truexAdRenderer = new TruexAdRenderer(context);

        // Listen for all ad events.
        truexAdRenderer.addEventListener(null, this::adEventHandler);
    }

    /**
     * Start displaying the true[X] engagement
     * @param viewGroup - the view group in which you would like to display the true[X] engagement
     * @param adParameters - the ad parameters (i.e. the user_id, placement_hash, and vast_config_url)
     * @param slotType - the slot in which the ad (i.e. "PREROLL" or "MIDROLL")
     */
    public void startAd(ViewGroup viewGroup, JSONObject adParameters, String slotType) {
        try {
            String vastConfigUrl = adParameters.getString("vast_config_url");
            truexAdRenderer.init(vastConfigUrl);
            truexAdRenderer.start(viewGroup);
        } catch (JSONException err) {
            Log.e(CLASSTAG, "could not access vast_config_url: " + adParameters.toString());
            throw new RuntimeException(err);
        }
    }

    /**
     * Inform the true[X] ad renderer that the application has resumed
     */
    public void resume() {
        truexAdRenderer.resume();
    }


    /**
     * Inform the true[X] ad renderer that the application has paused
     */
    public void pause() {
        truexAdRenderer.pause();
    }

    /**
     * Inform that the true[X] ad renderer that the application has stopped
     */
    public void stop() {
        truexAdRenderer.stop();
    }

    /*
       Note: This event is triggered when the ad starts
     */
    private void adEventHandler(TruexAdEvent event, Map<String, ?> data) {
        Log.d(CLASSTAG, "ad event recieved: " + event);

        boolean closeAd = false;
        switch (event) {
            case AD_STARTED:
                break;

           case USER_CANCEL_STREAM:
                // User backed out of the choice card, which means backing out of the entire video.
                // The user would like to cancel the stream, but we are not supporting that in this app.
            case AD_ERROR:
            case AD_COMPLETED:
            case NO_ADS_AVAILABLE:
                closeAd = true;
                break;

            case AD_FREE_POD:
                // the user did sufficient interaction for an ad credit
                didReceiveCredit = true;
                break;

            case OPT_IN:
                // User started the engagement experience
            case OPT_OUT:
                // User cancelled out of the choice card, either explicitly, or implicitly via a timeout.
            case USER_CANCEL:
                // User backed out of the ad, now showing the choice card again.
            case SKIP_CARD_SHOWN:
            default:
                break;
        }
        if (closeAd) {
            if (didReceiveCredit) {
                playbackHandler.skipCurrentAdBreak();
            } else {
                playbackHandler.resumeStream();
            }
        }
    };
}
