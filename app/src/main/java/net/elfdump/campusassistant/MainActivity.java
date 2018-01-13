package net.elfdump.campusassistant;

import android.graphics.Color;
import android.os.Debug;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.indoorway.android.common.sdk.IndoorwaySdk;
import com.indoorway.android.common.sdk.listeners.generic.Action1;
import com.indoorway.android.common.sdk.model.Coordinates;
import com.indoorway.android.common.sdk.model.IndoorwayMap;
import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;
import com.indoorway.android.common.sdk.model.IndoorwayPoiType;
import com.indoorway.android.common.sdk.model.IndoorwayPosition;
import com.indoorway.android.common.sdk.model.Visitor;
import com.indoorway.android.common.sdk.model.VisitorLocation;
import com.indoorway.android.common.sdk.task.IndoorwayTask;
import com.indoorway.android.fragments.sdk.map.IndoorwayMapFragment;
import com.indoorway.android.fragments.sdk.map.MapFragment;
import com.indoorway.android.location.sdk.IndoorwayLocationSdk;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableCircle;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableIcon;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableText;
import com.indoorway.android.map.sdk.view.drawable.layers.MarkersLayer;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements IndoorwayMapFragment.OnMapFragmentReadyListener {
    private final Handler mHandler = new Handler();
    private MapFragment mapFragment;
    private MarkersLayer myLayer;

    private IndoorwayPosition currentPosition;

    Action1<IndoorwayPosition> listener = new Action1<IndoorwayPosition>() {
        @Override
        public void onAction(IndoorwayPosition position) {
            // store last position as a field
            currentPosition = position;

            // react for position changes...
            mapFragment.getMapView().getNavigation().start(currentPosition, "3-_M01M3r5w_ca808"); // Room 216
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IndoorwayMapFragment.Config config = new IndoorwayMapFragment.Config();
        config.setLocationButtonVisible(true);
        config.setStartPositioningOnResume(true);
        config.setLoadLastKnownMap(true);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        IndoorwayMapFragment fragment = IndoorwayMapFragment.newInstance(this, config);
        fragmentTransaction.add(R.id.fragment_container, fragment, IndoorwayMapFragment.class.getSimpleName());
        fragmentTransaction.commit();

        // TODO: Unregister
        IndoorwayLocationSdk.instance()
            .position()
            .onChange()
            .register(listener);
    }

    private final Runnable mUpdateUI = new Runnable() {
        public void run() {
            IndoorwaySdk.instance()
                .visitors()
                .locations()
                .setOnCompletedListener(new Action1<List<VisitorLocation>>() {
                    @Override
                    public void onAction(List<VisitorLocation> visitorLocations) {
                        Log.i("DDFSFSDF", "WORKING");
                        for(VisitorLocation visitor : visitorLocations) {
                            if (visitor.getLat() == null || visitor.getLon() == null || visitor.getTimestamp() == null) continue; // DLACZEGO TE NULLE

                            myLayer.remove(visitor.getVisitorUuid());

                            if (new Date().getTime() - visitor.getTimestamp().getTime() > 10000) continue; // za stare

                            Log.i("ASDDSFDSFSFS", visitor.toString());
                            myLayer.add(
                                new DrawableCircle(
                                    visitor.getVisitorUuid(),
                                    .5f, // radius in meters, eg. 0.4f
                                    Color.RED, // circle background color, eg. Color.RED
                                    Color.BLUE, // color of outline, eg. Color.BLUE
                                    .1f, // width of outline in meters, eg. 0.1f
                                    new Coordinates(visitor.getLat(), visitor.getLon()) // coordinates of circle center point
                                )
                            );
                        }

                        mHandler.postDelayed(mUpdateUI, 1000);
                    }
                })
                .setOnFailedListener(new Action1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        Log.e("DFSDFSDDFS", "lel");
                        mHandler.postDelayed(mUpdateUI, 1000);
                    }
                })
                .execute();
        }
    };

    @Override
    public void onMapFragmentReady(@NotNull MapFragment mapFragment) {
        this.mapFragment = mapFragment;
        //mapFragment.getMapView().load(IndoorwayConstants.BUIDING_UUID, IndoorwayConstants.FLOOR2_UUID);
        mapFragment.getMapView().setOnMapLoadCompletedListener(new Action1<IndoorwayMap>() {
            @Override
            public void onAction(IndoorwayMap indoorwayMap) {
                myLayer = MainActivity.this.mapFragment.getMapView().getMarker().addLayer(100.0f);

                for(IndoorwayObjectParameters obj : indoorwayMap.getObjects()) {
                    Log.i("FDSfsdfsfdsf", obj.getId()+";"+obj.getName()+";"+obj.getType()+";"+obj.getCenterPoint().toString());
                }

                mHandler.post(mUpdateUI);
            }
        });
    }
}
