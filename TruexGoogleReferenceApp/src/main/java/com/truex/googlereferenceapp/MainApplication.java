package com.truex.googlereferenceapp;

import com.crashlytics.android.Crashlytics;
import com.truex.googlereferenceapp.dagger.DaggerAppComponent;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import io.fabric.sdk.android.Fabric;

public class MainApplication extends DaggerApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(
                new Fabric.Builder(this)
                        .kits( new Crashlytics())
                        .appIdentifier(BuildConfig.APPLICATION_ID)
                        .build()
        );
    }

    @Override
    protected AndroidInjector<? extends MainApplication> applicationInjector() {
        return DaggerAppComponent.builder().create(this);
    }
}
