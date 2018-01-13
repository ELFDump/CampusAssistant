package net.elfdump.campusassistant;

import android.content.Context;
import android.util.Log;

import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;
import com.indoorway.android.map.sdk.config.MapViewConfig;

import org.jetbrains.annotations.NotNull;

class CustomMapViewConfig extends MapViewConfig {

    public CustomMapViewConfig(@NotNull Context applicationContext) {
        super(applicationContext);
    }

    // override attributes, getters, methods etc.

    @Override
    public int getRoomBackgroundColor(@NotNull IndoorwayObjectParameters obj) {
        Log.e("nngjknfjngfdj", obj.toString());
        return getApplicationContext().getResources().getColor(R.color.white);
    }

    @Override
    public int getRoomOutlineColor(@NotNull IndoorwayObjectParameters obj) {
        return getApplicationContext().getResources().getColor(R.color.colorPrimary);
    }

}
