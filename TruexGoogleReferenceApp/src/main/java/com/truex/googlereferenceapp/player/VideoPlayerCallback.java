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

import com.google.ads.interactivemedia.v3.api.player.VideoStreamPlayer;

/**
 * Video player callback to be called when a seek occurs,
 * in addition to the usual video stream player callbacks.
 */
public interface VideoPlayerCallback extends VideoStreamPlayer.VideoStreamPlayerCallback {
    void onSeek(int windowIndex, long positionMs);
}
