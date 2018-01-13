package net.elfdump.campusassistant;

import android.app.NotificationManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.indoorway.android.common.sdk.IndoorwaySdk;
import com.indoorway.android.common.sdk.listeners.generic.Action1;
import com.indoorway.android.common.sdk.model.Coordinates;
import com.indoorway.android.common.sdk.model.IndoorwayMap;
import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;
import com.indoorway.android.common.sdk.model.IndoorwayPosition;
import com.indoorway.android.common.sdk.model.VisitorLocation;
import com.indoorway.android.common.sdk.task.IndoorwayTask;
import com.indoorway.android.fragments.sdk.map.IndoorwayMapFragment;
import com.indoorway.android.fragments.sdk.map.MapFragment;
import com.indoorway.android.location.sdk.IndoorwayLocationSdk;
import com.indoorway.android.map.sdk.listeners.OnObjectSelectedListener;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableCircle;
import com.indoorway.android.map.sdk.view.drawable.layers.MarkersLayer;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements IndoorwayMapFragment.OnMapFragmentReadyListener {
    private final Handler mHandler = new Handler();
    private MapFragment mapFragment;
    private MarkersLayer myLayer;

    private IndoorwayPosition currentPosition;

    Action1<IndoorwayPosition> positionListener = new Action1<IndoorwayPosition>() {
        @Override
        public void onAction(IndoorwayPosition position) {
            // store last position as a field
            currentPosition = position;

            // react for position changes...
            mapFragment.getMapView().getNavigation().start(currentPosition, IndoorwayConstants.ROOM_216_UUID);
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        IndoorwayLocationSdk.instance()
            .position()
            .onChange()
            .register(positionListener);
    }

    @Override
    protected void onStop() {
        IndoorwayLocationSdk.instance()
            .position()
            .onChange()
            .unregister(positionListener);

        super.onStop();
    }

    private final Runnable mUpdateUI = new Runnable() {
        public void run() {
            IndoorwaySdk.instance()
                .visitors()
                .locations()
                .setOnCompletedListener(new Action1<List<VisitorLocation>>() {
                    @Override
                    public void onAction(List<VisitorLocation> visitorLocations) {
//                        Log.i(IndoorwayConstants.LOG_TAG, "WORKING");
                        for (VisitorLocation visitor : visitorLocations) {
                            if (visitor.getLat() == null || visitor.getLon() == null || visitor.getTimestamp() == null)
                                continue; // DLACZEGO TE NULLE

                            myLayer.remove(visitor.getVisitorUuid());

                            if (new Date().getTime() - visitor.getTimestamp().getTime() > 10000)
                                continue; // za stare

//                            Log.i(IndoorwayConstants.LOG_TAG, visitor.toString());
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

                        mHandler.postDelayed(mUpdateUI, 3000);
                    }
                })
                .setOnFailedListener(new Action1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        Log.e(IndoorwayConstants.LOG_TAG, "lel");
                        mHandler.postDelayed(mUpdateUI, 10000);
                    }
                })
                .execute();
        }
    };

    @Override
    public void onMapFragmentReady(@NotNull final MapFragment mapFragment) {
        this.mapFragment = mapFragment;
        mapFragment.getMapView().setOnMapLoadCompletedListener(new Action1<IndoorwayMap>() {
            @Override
            public void onAction(IndoorwayMap indoorwayMap) {
                myLayer = mapFragment.getMapView().getMarker().addLayer(100.0f);

                mHandler.post(mUpdateUI);
            }
        });
        mapFragment.getMapView().getSelection()
            .setOnObjectSelectedListener(new OnObjectSelectedListener() {
                @Override
                public boolean canObjectBeSelected(IndoorwayObjectParameters parameters) {
                    return parameters.getId().equals(IndoorwayConstants.ROOM_216_UUID);
                }

                @Override
                public void onObjectSelected(IndoorwayObjectParameters parameters) {
                    Log.i(IndoorwayConstants.LOG_TAG, "SELECT "+parameters.getName()+" "+parameters.getId());
                }

                @Override
                public void onSelectionCleared() {
                    Log.i(IndoorwayConstants.LOG_TAG, "DESELECT");
                }
            });
    }
}
