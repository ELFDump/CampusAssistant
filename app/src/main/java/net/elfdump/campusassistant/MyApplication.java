package net.elfdump.campusassistant;

import android.app.Application;

import com.indoorway.android.common.sdk.IndoorwaySdk;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // init application context on each Application start
        IndoorwaySdk.initContext(this);

        // it's up to you when to initialize IndoorwaySdk, once initialized it will work forever!
        IndoorwaySdk.configure(IndoorwayConstants.API_KEY);
    }
}
