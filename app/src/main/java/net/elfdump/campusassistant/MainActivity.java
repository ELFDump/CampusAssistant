package net.elfdump.campusassistant;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.indoorway.android.common.sdk.IndoorwaySdk;
import com.indoorway.android.common.sdk.listeners.generic.Action1;
import com.indoorway.android.common.sdk.model.Coordinates;
import com.indoorway.android.common.sdk.model.IndoorwayMap;
import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;
import com.indoorway.android.common.sdk.model.IndoorwayPosition;
import com.indoorway.android.common.sdk.model.VisitorLocation;
import com.indoorway.android.common.sdk.model.proximity.IndoorwayProximityEvent;
import com.indoorway.android.common.sdk.task.IndoorwayTask;
import com.indoorway.android.fragments.sdk.map.IndoorwayMapFragment;
import com.indoorway.android.fragments.sdk.map.MapFragment;
import com.indoorway.android.location.sdk.IndoorwayLocationSdk;
import com.indoorway.android.map.sdk.listeners.OnObjectSelectedListener;
import com.indoorway.android.map.sdk.view.IndoorwayMapView;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableCircle;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawablePolygon;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableText;
import com.indoorway.android.map.sdk.view.drawable.layers.MarkersLayer;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements IndoorwayMapFragment.OnMapFragmentReadyListener {
    private MapFragment mapFragment;
    private MarkersLayer myLayer;

    private IndoorwayPosition currentPosition;
    private String selectedObject;

    private Timer timer;

    private Map<String, Integer> peopleCount;

    Action1<IndoorwayPosition> positionListener = new Action1<IndoorwayPosition>() {
        @Override
        public void onAction(IndoorwayPosition position) {
            currentPosition = position;

            if (selectedObject != null)
                mapFragment.getMapView().getNavigation().start(currentPosition, selectedObject);
            else
                mapFragment.getMapView().getNavigation().stop();
        }
    };

    Action1<IndoorwayProximityEvent> proximityListener = new Action1<IndoorwayProximityEvent>() {
        @Override
        public void onAction(IndoorwayProximityEvent indoorwayProximityEvent) {
            if (indoorwayProximityEvent.getTrigger() == IndoorwayProximityEvent.Trigger.EXIT) {
                ((TextView) findViewById(R.id.notification)).setText("-");
            }

            if (indoorwayProximityEvent.getTrigger() != IndoorwayProximityEvent.Trigger.ENTER)
                return;

            IndoorwayMap indoorwayMap = mapFragment.getCurrentMap();
            if (!indoorwayProximityEvent.isForBuildingAndMap(indoorwayMap.getBuildingUuid(), indoorwayMap.getMapUuid()))
                return;

            String roomId = indoorwayProximityEvent.getIdentifier().split("\\+")[0];
            IndoorwayObjectParameters room = indoorwayMap.objectWithId(roomId);
            assert room != null;
            String roomName = room.getName();
            Log.e("jdnfsjfbdsfbhfb", roomName);

            ((TextView) findViewById(R.id.notification)).setText(roomName);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SlidingUpPanelLayout mLayout = findViewById(R.id.sliding_layout);
        mLayout.setOverlayed(true);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        IndoorwayMapFragment.Config config = new IndoorwayMapFragment.Config();
        config.setLocationButtonVisible(false);
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

        IndoorwayLocationSdk.instance()
            .customProximityEvents()
            .onEvent()
            .register(proximityListener);

        timer = new Timer();
        timer.schedule(new UpdateVisitorIcons(), 0, 1000); // TODO: 2000
        timer.schedule(new UpdatePeopleCount(), 0, 1000); // TODO: 10000
    }

    @Override
    protected void onStop() {
        timer.cancel();

        IndoorwayLocationSdk.instance()
            .customProximityEvents()
            .onEvent()
            .unregister(proximityListener);

        IndoorwayLocationSdk.instance()
            .position()
            .onChange()
            .unregister(positionListener);

        super.onStop();
    }

    private class UpdateVisitorIcons extends TimerTask {
        @Override
        public void run() {
            if (myLayer == null)
                return;

            IndoorwaySdk.instance()
                .visitors()
                .locations()
                .setOnCompletedListener(new Action1<List<VisitorLocation>>() {
                    @Override
                    public void onAction(List<VisitorLocation> visitorLocations) {
//                        Log.i(IndoorwayConstants.LOG_TAG, "WORKING");
                        IndoorwayMap indoorwayMap = mapFragment.getCurrentMap();
                        assert indoorwayMap != null;
                        for (VisitorLocation visitor : visitorLocations) {
                            if (visitor.getLat() == null || visitor.getLon() == null || visitor.getTimestamp() == null || visitor.getBuildingUuid() == null || visitor.getMapUuid() == null)
                                continue; // DLACZEGO TE NULLE

                            if (!visitor.getBuildingUuid().equals(indoorwayMap.getBuildingUuid()) || !visitor.getMapUuid().equals(indoorwayMap.getMapUuid()))
                                continue; // złe piętro

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
                    }
                })
                .setOnFailedListener(new Action1<IndoorwayTask.ProcessingException>() {
                    @Override
                    public void onAction(IndoorwayTask.ProcessingException e) {
                        Log.e(IndoorwayConstants.LOG_TAG, "lel");
                    }
                })
                .execute();
        }
    }

    private class UpdatePeopleCount extends TimerTask {
        @Override
        public void run() {
            try {
                peopleCount = MyApplication.getRestClient().location().getPeopleCount().execute().body();

                if (mapFragment.getCurrentMap() == null || myLayer == null) {
                    return;
                }

                for(IndoorwayObjectParameters room : mapFragment.getCurrentMap().getObjects()) {
                    if (!IndoorwayConstants.isRoom(room))
                        continue;

                    Log.i(IndoorwayConstants.LOG_TAG, "Room "+room+": "+getPeopleCount(room.getId()));

                    int green = Color.argb(60, 0, 255, 0);
                    int red = Color.argb(60, 255, 0, 0);

                    int amount = getPeopleCount(room.getId());
                    setRoomColor(room.getId(), (amount > 0 ? red : green));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMapFragmentReady(@NotNull final MapFragment mapFragment) {
        this.mapFragment = mapFragment;
        mapFragment.getMapView().setOnMapLoadCompletedListener(new Action1<IndoorwayMap>() {
            @Override
            public void onAction(IndoorwayMap indoorwayMap) {
                myLayer = mapFragment.getMapView().getMarker().addLayer(1.0f);
            }
        });

        mapFragment.getMapView().getSelection()
            .setOnObjectSelectedListener(new OnObjectSelectedListener() {
                @Override
                public boolean canObjectBeSelected(IndoorwayObjectParameters parameters) {
                    Log.i(IndoorwayConstants.LOG_TAG, parameters.getId());
                    return IndoorwayConstants.isRoom(parameters);
                }

                @Override
                public void onObjectSelected(IndoorwayObjectParameters parameters) {
                    Log.i(IndoorwayConstants.LOG_TAG, "SELECT " + parameters.getName() + " " + parameters.getId());

                    final SlidingUpPanelLayout mLayout = findViewById(R.id.sliding_layout);
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

                    if (parameters.getId().equals(selectedObject)) {
                        mLayout.setPanelState(
                                mLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN ?
                                        SlidingUpPanelLayout.PanelState.HIDDEN : SlidingUpPanelLayout.PanelState.COLLAPSED);
                    }

                    selectedObject = parameters.getId();
                    updateRoomDetails();
                    if (currentPosition != null) {
                        mapFragment.getMapView().getNavigation().start(currentPosition, selectedObject);
                    }

//                    setRoomColor(selectedObject, red);
                }

                @Override
                public void onSelectionCleared() {
                    Log.i(IndoorwayConstants.LOG_TAG, "DESELECT");
                    selectedObject = null;
                    mapFragment.getMapView().getNavigation().stop();

                    final SlidingUpPanelLayout mLayout = findViewById(R.id.sliding_layout);
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                }
            });
    }

    public void updateRoomDetails() {
        TextView peopleAmount = findViewById(R.id.usersAmount);
        peopleAmount.setText(String.valueOf(getPeopleCount(selectedObject)));
    }

    private int getPeopleCount(String roomId) {
        int amount = 0;
        if (peopleCount.containsKey(roomId)) {
            amount = peopleCount.get(roomId);
        } else {
            Log.w(IndoorwayConstants.LOG_TAG, "No data for room: "+roomId);
        }
        return amount;
    }

    public void setRoomColor(String roomId, int color) {
        IndoorwayObjectParameters originalRoom = null;
        for (IndoorwayObjectParameters parameters : mapFragment.getCurrentMap().getObjects()) {
            if (parameters.getId().equals(roomId)) {
                originalRoom = parameters;
            }

            if (parameters.getId().equals(roomId+"Overlay")) { //TODO: ???
                myLayer.remove(roomId+"Overlay");
            }
        }

        if (originalRoom != null) {
            myLayer.add(new DrawablePolygon(roomId+"Overlay", originalRoom.getCoordinates(), color));
        }

    }
}
