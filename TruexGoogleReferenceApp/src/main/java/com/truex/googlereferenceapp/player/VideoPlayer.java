/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.truex.googlereferenceapp.player;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

/**
 * A video player that plays HLS or DASH streams using ExoPlayer.
 */
public class VideoPlayer {

    private static final String CLASSTAG = VideoPlayer.class.getSimpleName();

    private static final String USER_AGENT = "ImaSamplePlayer (Linux;Android "
            + Build.VERSION.RELEASE + ") ImaSample/1.0";

    private Context context;

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private VideoPlayerCallback playerCallback;

    private Timeline.Period period = new Period();

    private String streamURL;
    private Boolean isStreamRequested;
    private boolean canSeek;

    public VideoPlayer(Context context, PlayerView playerView) {
        this.context = context;
        this.playerView = playerView;
        isStreamRequested = false;
        canSeek = true;
    }

    private void initPlayer() {
        release();

        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        DefaultTrackSelector.Parameters params = new DefaultTrackSelector.ParametersBuilder().setPreferredTextLanguage("en").build();
        trackSelector.setParameters(params);

        player = ExoPlayerFactory.newSimpleInstance(this.context, new DefaultRenderersFactory(context),
                trackSelector, new DefaultLoadControl());
        playerView.setPlayer(player);
        playerView.setControlDispatcher(new ControlDispatcher() {
            @Override
            public boolean dispatchSetPlayWhenReady(Player player, boolean playWhenReady) {
                player.setPlayWhenReady(playWhenReady);
                return true;
            }

            @Override
            public boolean dispatchSeekTo(Player player, int windowIndex, long positionMs) {
                if (canSeek) {
                    if (playerCallback != null) {
                        playerCallback.onSeek(windowIndex, positionMs);
                    } else {
                        player.seekTo(windowIndex, positionMs);
                    }
                }
                return true;
            }

            @Override
            public boolean dispatchSetRepeatMode(Player player, int repeatMode) {
                return false;
            }

            @Override
            public boolean dispatchSetShuffleModeEnabled(Player player,
                                                         boolean shuffleModeEnabled) {
                return false;
            }

            @Override
            public boolean dispatchStop(Player player, boolean reset) {
                return false;
            }
        });
    }

    public void play() {
        play(Player.REPEAT_MODE_OFF, 1);
    }

    public void play(int repeatMode, int volume) {
        if (isStreamRequested) {
            // Stream requested, just resume.
            player.setPlayWhenReady(true);
            return;
        }

        initPlayer();

        player.setRepeatMode(repeatMode);
        player.setVolume(volume);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, USER_AGENT);
        int type = Util.inferContentType(Uri.parse(streamURL));
        MediaSource mediaSource;
        switch (type) {
            case C.TYPE_HLS:
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(streamURL));
                break;
            case C.TYPE_DASH:
                mediaSource = new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                        .createMediaSource(Uri.parse(streamURL));
                break;
            case C.TYPE_OTHER:
                mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(streamURL));
                break;
            default:
                Log.e(CLASSTAG, "Error! Invalid Media Source, exiting");
                return;
        }

        player.prepare(mediaSource);

        // Register for ID3 events.
        player.addMetadataOutput((Metadata metadata) -> {
            for (int i = 0; i < metadata.length(); i++) {
                Metadata.Entry entry = metadata.get(i);
                if (entry instanceof TextInformationFrame) {
                    TextInformationFrame textFrame = (TextInformationFrame) entry;
                    if ("TXXX".equals(textFrame.id)) {
                        Log.d(CLASSTAG, "Received user text: " + textFrame.value);
                        if (playerCallback != null) {
                            playerCallback.onUserTextReceived(textFrame.value);
                        }
                    }
                }
            }
        });

        player.setPlayWhenReady(true);
        isStreamRequested = true;
    }

    public void pause() {
        player.setPlayWhenReady(false);
    }

    public void hide() {
        playerView.setVisibility(View.GONE);
    }

    public void display() {
        playerView.setVisibility(View.VISIBLE);
    }

    void seekTo(SeekPosition seekPosition) {
        player.seekTo(seekPosition.getMilliseconds());
    }

    void seekTo(int windowIndex, SeekPosition seekPosition) {
        player.seekTo(windowIndex, seekPosition.getMilliseconds());
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
            isStreamRequested = false;
        }
    }

    public void setStreamURL(String streamURL) {
        this.streamURL = streamURL;
        isStreamRequested = false; //request new stream on play
    }

    public void enableControls(boolean doEnable) {
        if (doEnable) {
            playerView.showController();
            playerView.setControllerAutoShow(true);
        } else {
            playerView.hideController();
            playerView.setControllerAutoShow(false);
        }
        canSeek = doEnable;
    }

    public void requestFocus() {
        playerView.requestFocus();
    }

    public boolean isStreamRequested() {
        return isStreamRequested;
    }

    // Methods for exposing player information.
    void setCallback(VideoPlayerCallback callback) {
        playerCallback = callback;
    }

    long getCurrentPositionPeriod() {
        // Adjust position to be relative to start of period rather than window, to account for DVR
        // window.
        long position = player.getCurrentPosition();
        Timeline currentTimeline = player.getCurrentTimeline();
        if (!currentTimeline.isEmpty()) {
            position -= currentTimeline.getPeriod(player.getCurrentPeriodIndex(), period)
                    .getPositionInWindowMs();
        }
        return position;
    }

    int getVolume() {
        return Math.round(player.getVolume() * 100);
    }

    long getDuration() {
        return player.getDuration();
    }
}
