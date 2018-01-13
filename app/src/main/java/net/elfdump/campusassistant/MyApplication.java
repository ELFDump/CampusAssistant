package net.elfdump.campusassistant;

import android.app.Application;
import android.util.Log;

import com.indoorway.android.common.sdk.IndoorwaySdk;
import com.indoorway.android.common.sdk.model.Visitor;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // init application context on each Application start
        IndoorwaySdk.initContext(this);

        // it's up to you when to initialize IndoorwaySdk, once initialized it will work forever!
        IndoorwaySdk.configure(IndoorwayConstants.API_KEY);

        Visitor visitor = new Visitor();
        visitor.setShareLocation(true);
        IndoorwaySdk.instance().visitor().setup(visitor);
        Log.i("APPPPPPPP", "App started");
    }
}
