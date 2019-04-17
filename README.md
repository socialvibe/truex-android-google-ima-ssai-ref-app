# TruexGoogleReferenceApp

## Overview

This project contains sample source code that demonstrates how to integrate the true[X]
ad renderer with the Google Ad Manager IMA SDK on Fire TV and Android TV. This document
will step through the various pieces of code that make the integration work, so that
the same basic ideas can be replicated in a real production app.

This reference app covers the essential work. For a more detailed integration guide, please refer to: https://github.com/socialvibe/truex-androidtv-integrations.

## Assumptions

We assume you have either already integrated the IMA SDK with your app, or you are
working from a project that has been created following the instructions at the
[IMA SDK Quickstart page](https://developers.google.com/interactive-media-ads/docs/sdks/android/quickstart).
We also assume you have already acquired the true[X] renderer code through
[Maven](https://github.com/socialvibe/truex-tv-integrations) or direct download,
and have added to your project appropriately.

## References

We've marked the source code with comments containing numbers in brackets: ("[3]", for example),
that correlate with the steps listed below. For example, if you want to see how to parse ad 
parameters, search the `VideoPlaybackManager.java` file for `[3]` and you will find the related
code.

## Steps

### [1] - Look for true[X] companions for a given ad (VideoPlaybackManager.java)

In the IMA delegate method `onAdEvent`, we call `onAdStarted` when the event is an ad
started event. In `onAdStarted`, we call the `Ad`'s `getCompanionAds` method and inspect
each of the companion ads. If any companion has an `apiFramework` value matching `truex`,
then we ignore all other companion ads and begin the true[X] engagement experience.

### [2] - Parse ad parameters (VideoPlaybackManager.java)

The `CompanionAd` object contains a data URL which encodes parameters used by the true[X]
renderer. We parse this base64 string into a `JSONObject`.

### [3] - Prepare to enter the engagement (VideoPlaybackManager.java)

By default the underlying ads, which IMA has stitched into the stream, will keep playing.
First we pause playback. There will be a "placeholder" ad at the first position of the ad
break (this is the true[X] ad also containing information on how to enter the engagement).
We need to seek over the placeholder.

### [4] - Initialize and start the renderer (VideoPlaybackManager.java)

Once we have the ad parameter JSON object, we can initialize the true[X] ad manager and set
our `VideoPlaybackManager` as its `PlaybackHandler`. We then start the true[X] ad experience
by calling `startAd` on the true[X] ad manager that we instantiated, with the ad parameters,
slot type, and the view group in which we will be displaying the true[X] ad experience.

### [5] - Respond to AD_FREE_POD (TruexAdManager.java)

If the user fulfills the requirements to earn true[ATTENTION], the true[X] event listener for
the `AD_FREE_POD` event will be called. We respond by seeking the underlying stream over the
current ad break. This accomplishes the "reward" portion of the engagement.

### [6] - Respond to renderer finish events (TruexAdManager.java)

There are three ways the renderer can finish:

1. There were no ads available. (`NO_ADS_AVAILABLE`)
2. The ad had an error. (`AD_ERROR`)
3. The viewer has completed the engagement. (`AD_COMPLETED`)

In all three of these cases, the renderer will have removed itself from view.
The remaining work is to resume playback.

## Setup

### Pre-Requisites

* [Install Android Studio](https://developer.android.com/studio/)

### Install Steps

* Clone the `master` branch of the `Sheppard` repository
    * `git clone https://github.com/socialvibe/truex-android-google-ad-manager-reference-app.git`

* Open Sheppard with Android Studio
    * Open Android Studio
    * Select `Open an existing Android Studio project` and select the Sheppard folder


### Run Steps

#### Run on Virtual Device (Android TV)
* Create a Virtual Device
    * Select `Run 'TruexGoogleReferenceApp'` or `Debug 'TruexGoogleReferenceApp'` in Android Studio
    * Select `Create New Virtual Device`
    * Select the `TV` category
    * Select a device definition
    * Select a system image
    * Select `Finish` to finish creating the Virtual Device
* Select `Run 'TruexGoogleReferenceApp'` or `Debug 'TruexGoogleReferenceApp'` in Android Studio
* Select the virtual device and press `OK`

#### Run on Physical Device (Fire TV)
* [Enable ADB on the Fire TV Device](http://www.aftvnews.com/how-to-enable-adb-debugging-on-an-amazon-fire-tv-or-fire-tv-stick/)
    * If the device has already been set-up, ignore this step
* [Retrieve the IP Address of the Fire TV Device](http://www.aftvnews.com/how-to-determine-the-ip-address-of-an-amazon-fire-tv-or-fire-tv-stick/)
* Connect to the Fire TV device using ADB
    * Make sure that ADB has been set-up correctly. ADB is included with Android Studio and can be installed separately.
    * `adb connect <ip_address>:5555`, i.e. `adb connect 10.11.6.221:5555`
* Select `Run 'app'` or `Debug 'app'` in Android Studio
* Select the Fire TV and press `OK`

#### Run on Physical Device (Android TV)
* [Enable ADB on the Android TV device](https://developers.google.com/cast/docs/android_tv#setting-up)
    * If the device has already been set-up, ignore this step
* [Connect to the Android TV Device using ADB](https://developers.google.com/cast/docs/android_tv#adb-tcpip)
    * Make sure that ADB has been set-up correctly. ADB is included with Android Studio and can be installed separately.
    * `adb connect <ip_address>:5555`, i.e. `adb connect 10.11.6.176:5555`
* Select `Run 'TruexGoogleReferenceApp'` or `Debug 'TruexGoogleReferenceApp'` in Android Studio
* Select the Android TV and press `OK`
