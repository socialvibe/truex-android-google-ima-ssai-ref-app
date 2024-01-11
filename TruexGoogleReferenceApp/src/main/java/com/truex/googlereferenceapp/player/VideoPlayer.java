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

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.Timeline.Period;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.dash.DefaultDashChunkSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

/**
 * A video player that plays HLS or DASH streams using ExoPlayer.
 */
public class VideoPlayer {

    private static final String CLASSTAG = VideoPlayer.class.getSimpleName();

    private static final String USER_AGENT = "ImaSamplePlayer (Linux;Android "
            + Build.VERSION.RELEASE + ") ImaSample/1.0";

    private Context context;

    private ExoPlayer player;
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

        player = new ExoPlayer.Builder(this.context).build();
        playerView.setPlayer(player);
        player.setPlayWhenReady(true);

//        playerView.setControlDispatcher(new ControlDispatcher() {
//            @Override
//            public boolean dispatchSetPlayWhenReady(Player player, boolean playWhenReady) {
//                player.setPlayWhenReady(playWhenReady);
//                return true;
//            }
//
//            @Override
//            public boolean dispatchSeekTo(Player player, int windowIndex, long positionMs) {
//                if (canSeek) {
//                    if (playerCallback != null) {
//                        playerCallback.onSeek(windowIndex, positionMs);
//                    } else {
//                        player.seekTo(windowIndex, positionMs);
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean dispatchSetRepeatMode(Player player, int repeatMode) {
//                return false;
//            }
//
//            @Override
//            public boolean dispatchSetShuffleModeEnabled(Player player,
//                                                         boolean shuffleModeEnabled) {
//                return false;
//            }
//
//            @Override
//            public boolean dispatchStop(Player player, boolean reset) {
//                return false;
//            }
//        });
    }

    public void play() {
        play(Player.REPEAT_MODE_OFF, 1);
    }

    @OptIn(markerClass = UnstableApi.class)
    public void play(int repeatMode, int volume) {
        if (isStreamRequested) {
            // Stream requested, just resume.
            player.setPlayWhenReady(true);
            return;
        }

        initPlayer();

        player.setRepeatMode(repeatMode);
        player.setVolume(volume);

        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);
        int type = Util.inferContentType(Uri.parse(streamURL));
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamURL));
        MediaSource mediaSource;
        switch (type) {
            case C.CONTENT_TYPE_HLS:
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem);
                break;
            case C.CONTENT_TYPE_DASH:
                mediaSource = new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                        .createMediaSource(mediaItem);
                break;
            case C.CONTENT_TYPE_OTHER:
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                break;
            default:
                Log.e(CLASSTAG, "Error! Invalid Media Source, exiting");
                return;
        }

        player.prepare(mediaSource);

        player.setPlayWhenReady(true);
        isStreamRequested = true;
    }

    public void pause() {
        player.setPlayWhenReady(false);
    }

    public void hide() {
        playerView.setVisibility(View.GONE);
    }

    public void show() {
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

    @OptIn(markerClass = UnstableApi.class)
    public void enableControls(boolean doEnable) {
        if (doEnable) {
            playerView.showController();
            playerView.setControllerAutoShow(true);
            playerView.setClickable(true);
        } else {
            playerView.hideController();
            playerView.setControllerAutoShow(false);
            playerView.setClickable(false);
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
        if (player == null) return 0;

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
        return player == null ? 0 : Math.round(player.getVolume() * 100);
    }

    long getDuration() {
        return player == null ? 0 : player.getDuration();
    }
}
