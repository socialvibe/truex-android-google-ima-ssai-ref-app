package com.truex.googlereferenceapp.dagger;

import com.truex.googlereferenceapp.MainActivity;
import com.truex.googlereferenceapp.home.HomeViewFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class FragmentModule {

    @ContributesAndroidInjector()
    abstract MainActivity contributeMainActivityInjector();

    @ContributesAndroidInjector
    abstract HomeViewFragment contributeHomeViewFragmentInjector();
}
