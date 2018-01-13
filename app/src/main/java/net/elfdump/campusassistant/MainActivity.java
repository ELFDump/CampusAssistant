package net.elfdump.campusassistant;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.indoorway.android.fragments.sdk.map.IndoorwayMapFragment;
import com.indoorway.android.fragments.sdk.map.MapFragment;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity implements IndoorwayMapFragment.OnMapFragmentReadyListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onMapFragmentReady(@NotNull MapFragment mapFragment) {
        mapFragment.getMapView().load(IndoorwayConstants.BUILDING_UUID, IndoorwayConstants.FLOOR2_UUID);
    }
}
