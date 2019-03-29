package com.truex.googlereferenceapp.ads;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.truex.adrenderer.IEventHandler;
import com.truex.adrenderer.TruexAdRenderer;
import com.truex.adrenderer.TruexAdRendererConstants;

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
    private boolean didReceiveCredit;
    private TruexAdRenderer truexAdRenderer;

    public TruexAdManager(Context context, PlaybackHandler playbackHandler) {
        this.playbackHandler = playbackHandler;

        didReceiveCredit = false;

        // Set-up the true[X] ad renderer
        truexAdRenderer = new TruexAdRenderer(context);

        // Set-up the event listeners
        truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_STARTED, this.adStarted);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_COMPLETED, this.adCompleted);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_ERROR, this.adError);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.NO_ADS_AVAILABLE, this.noAds);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_FREE_POD, this.adFree);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.POPUP_WEBSITE, this.popup);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.USER_CANCEL, this.userCancel);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.OPT_IN, this.optIn);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.OPT_OUT, this.optOut);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.SKIP_CARD_SHOWN, this.skipCardShown);
        truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_FETCH_COMPLETED, this.adFetchCompleted);
    }

    /**
     * Start displaying the true[X] engagement
     * @param viewGroup - the view group in which you would like to display the true[X] engagement
     */
    public void startAd(ViewGroup viewGroup) {
        try {
            String json = String.format("{\"user_id\":\"3e47e82244f7aa7ac3fa60364a7ede8453f3f9fe\",\"placement_hash\":\"%s\",\"vast_config_url\":\"%s\"}\n", "81551ffa2b851abc5372ab9ed9f1f58adabe5203", "http://qa-get.truex.com/81551ffa2b851abc5372ab9ed9f1f58adabe5203/vast/config?asnw=&flag=%2Bamcb%2Bemcr%2Bslcb%2Bvicb%2Baeti-exvt&fw_key_values=&metr=0&prof=g_as3_truex&ptgt=a&pvrn=&resp=vmap1&slid=fw_truex&ssnw=&vdur=&vprn=");
            JSONObject adParams = new JSONObject(json);

            truexAdRenderer.init(adParams, TruexAdRendererConstants.PREROLL);
            truexAdRenderer.start(viewGroup);
        } catch (JSONException e) {
            Log.e(CLASSTAG, "JSON ERROR");
        }
    }

    /**
     * Inform the true[X] ad renderer that the application has resumed
     */
    public void onResume() {
        truexAdRenderer.resume();
    }


    /**
     * Inform the true[X] ad renderer that the application has paused
     */
    public void onPause() {
        truexAdRenderer.pause();
    }

    /**
     * Inform that the true[X] ad renderer that the application has stopped
     */
    public void onStop() {
        truexAdRenderer.stop();
    }

    /**
     * This method should be called once the true[X] ad manager is done
     */
    private void onCompletion() {
        if (didReceiveCredit) {
            // The user received true[ATTENTION] credit
            // Resume the content stream (and skip any linear ads)
            playbackHandler.resumeStream();
        } else {
            // The user did not receive credit
            // Continue the content stream and display linear ads
            playbackHandler.displayLinearAds();
        }
    }

    /*
       Note: This event is triggered when the ad starts
     */
    private IEventHandler adStarted = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "adStarted");
    };

    /*
       Note: This event is triggered when the engagement is completed,
       either by the completion of the engagement or the user exiting the engagement
     */
    private IEventHandler adCompleted = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "adCompleted");

        // We are now done with the engagement
        onCompletion();
    };

    /*
       Note: This event is triggered when an error is encountered by the true[X] ad renderer
     */
    private IEventHandler adError = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "adError");

        // There was an error trying to load the enagement
        onCompletion();
    };

    /*
       Note: This event is triggered if the engagement fails to load,
       as a result of there being no engagements available
     */
    private IEventHandler noAds = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "noAds");

        // There are no engagements available
        onCompletion();
    };

    /*
       Note: This event is not currently being used
     */
    private IEventHandler popup = (Map<String, ?> data) -> {
        String url = (String) data.get("url");
        Log.d(CLASSTAG, "popup");
        Log.d(CLASSTAG, "url: " + url);
    };

    /*
       Note: This event is triggered when the viewer has earned their true[ATTENTION] credit. We
       could skip over the linear ads here, so that when the ad is complete, all we would need
       to do is resume the stream.
     */
    private IEventHandler adFree = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "adFree");
        didReceiveCredit = true;
    };

    /*
       Note: This event is triggered when a user cancels an interactive engagement
     */
    private IEventHandler userCancel = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "userCancel");
    };

    /*
       Note: This event is triggered when a user opts-in to an interactive engagement
     */
    private IEventHandler optIn = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "optIn");
    };

    /*
       Note: This event is triggered when a user opts-out of an interactive engagement,
       either by time-out, or by choice
     */
    private IEventHandler optOut = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "optOut");
    };

    /*
       Note: This event is triggered when a skip card is being displayed to the user
       This occurs when a user is able to skip ads
     */
    private IEventHandler skipCardShown = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "skipCardShown");
    };

    /*
       Note: This event is triggered when the ad has been fetched
       This event occurs before the following events: AD_STARTED, NO_ADS_AVAILABLE, and AD_ERROR.
     */
    private IEventHandler adFetchCompleted = (Map<String, ?> data) -> {
        Log.d(CLASSTAG, "adFetchCompleted");
    };
}
