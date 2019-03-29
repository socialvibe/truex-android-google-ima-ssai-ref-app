# TruexGoogleReferenceApp

## Reference app for Android TV and Fire TV TAR (TruexAdRenderer) integrations using the Google Ad Manager (GAM)

This is an Android TV and Fire TV application exposing direct calls into `TruexAdRenderer` instances, enabling functional testing as well as prototyping. The application is set up with a simple activity and the calls in the `MainActivity` should be self-explanatory.

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
