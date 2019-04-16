package com.truex.googlereferenceapp;

import com.truex.googlereferenceapp.dagger.DaggerAppComponent;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;

public class MainApplication extends DaggerApplication {

    @Override
    protected AndroidInjector<? extends MainApplication> applicationInjector() {
        return DaggerAppComponent.builder().create(this);
    }
}
