package net.elfdump.campusassistant;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
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
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableCircle;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawablePolygon;
import com.indoorway.android.map.sdk.view.drawable.figures.DrawableText;
import com.indoorway.android.map.sdk.view.drawable.layers.MarkersLayer;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/*
    HIGH QUALITY LEGIT CODE
 */

public class MainActivity extends AppCompatActivity implements IndoorwayMapFragment.OnMapFragmentReadyListener {
    private MapFragment mapFragment;
    private MarkersLayer markersLayer;
    private MarkersLayer roomsLayer;

    private IndoorwayPosition currentPosition;
    private String selectedObject;

    private Timer timer;

    private Map<String, Integer> peopleCount;

    private int currentFloor = 0;

    Action1<IndoorwayPosition> positionListener = new Action1<IndoorwayPosition>() {
        @Override
        public void onAction(IndoorwayPosition position) {
            if (currentPosition == null || !currentPosition.getMapUuid().equals(position.getMapUuid())) {
                mapFragment.getMapView().load(position.getBuildingUuid(), position.getMapUuid());
            }

            currentPosition = position;

            if (selectedObject != null && mapFragment.getCurrentMap() != null && currentPosition.getMapUuid().equals(mapFragment.getCurrentMap().getMapUuid()))
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
            if (indoorwayMap == null || !indoorwayProximityEvent.isForBuildingAndMap(indoorwayMap.getBuildingUuid(), indoorwayMap.getMapUuid()))
                return;

            String roomId = indoorwayProximityEvent.getIdentifier().split("\\+")[0];
            IndoorwayObjectParameters room = indoorwayMap.objectWithId(roomId);
            assert room != null;
            String roomName = room.getName();

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
        config.setReloadMapOnPositionChange(false);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        IndoorwayMapFragment fragment = IndoorwayMapFragment.newInstance(this, config);
        fragmentTransaction.add(R.id.fragment_container, fragment, IndoorwayMapFragment.class.getSimpleName());
        fragmentTransaction.commit();

        findViewById(R.id.level_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapFragment.getMapView().load(IndoorwayConstants.BUILDING_UUID, IndoorwayConstants.FLOOR_UUIDS[currentFloor + 1]);
                changeFloorText(currentFloor + 1);
            }
        });

        findViewById(R.id.level_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapFragment.getMapView().load(IndoorwayConstants.BUILDING_UUID, IndoorwayConstants.FLOOR_UUIDS[currentFloor - 1]);
                changeFloorText(currentFloor - 1);
            }
        });
    }

    void changeFloorText(int floor) {
        TextView textView = findViewById(R.id.floorName);

        if (floor == 0) {
            textView.setText("Parter");
        } else {
            textView.setText(String.format("%d piętro", floor));
        }

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
            if (markersLayer == null)
                return;

            IndoorwaySdk.instance()
                .visitors()
                .locations()
                .setOnCompletedListener(new Action1<List<VisitorLocation>>() {
                    @Override
                    public void onAction(List<VisitorLocation> visitorLocations) {
                        IndoorwayMap indoorwayMap = mapFragment.getCurrentMap();
                        assert indoorwayMap != null;
                        for (VisitorLocation visitor : visitorLocations) {
                            if (visitor.getLat() == null || visitor.getLon() == null || visitor.getTimestamp() == null || visitor.getBuildingUuid() == null || visitor.getMapUuid() == null)
                                continue; // DLACZEGO TE NULLE

                            if (!visitor.getBuildingUuid().equals(indoorwayMap.getBuildingUuid()) || !visitor.getMapUuid().equals(indoorwayMap.getMapUuid()))
                                continue; // złe piętro

                            markersLayer.remove(visitor.getVisitorUuid());

                            if (new Date().getTime() - visitor.getTimestamp().getTime() > 10000)
                                continue; // za stare

                            markersLayer.add(
                                new DrawableCircle(
                                    visitor.getVisitorUuid(),
                                    .5f, // radius in meters, eg. 0.4f
                                    Color.RED, // circle background color, eg. Color.RED
                                    0, // color of outline, eg. Color.BLUE
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

                if (mapFragment.getCurrentMap() == null || roomsLayer == null) {
                    return;
                }

                for (IndoorwayObjectParameters room : mapFragment.getCurrentMap().getObjects()) {
                    if (!IndoorwayConstants.isRoom(room))
                        continue;

                    Log.i(IndoorwayConstants.LOG_TAG, "Room " + room + ": " + getPeopleCount(room.getId()));

                    int amount = getPeopleCount(room.getId());
                    int color = Color.argb(60, 50*amount, 255-50*amount, 0);
                    setRoomColor(room.getId(), color);
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
                markersLayer = mapFragment.getMapView().getMarker().addLayer(100.0f);
                roomsLayer = mapFragment.getMapView().getMarker().addLayer(1.0f);
                for (int i = 0; i < IndoorwayConstants.FLOOR_UUIDS.length; i++)
                    if (IndoorwayConstants.FLOOR_UUIDS[i].equals(indoorwayMap.getMapUuid()))
                        currentFloor = i;
                Log.e(IndoorwayConstants.LOG_TAG, "Current floor is " + currentFloor);
                findViewById(R.id.level_up).setEnabled(currentFloor < IndoorwayConstants.FLOOR_UUIDS.length - 1);
                findViewById(R.id.level_down).setEnabled(currentFloor > 0);
                changeFloorText(currentFloor);
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
                    if (currentPosition != null && mapFragment.getCurrentMap() != null && currentPosition.getMapUuid().equals(mapFragment.getCurrentMap().getMapUuid())) {
                        mapFragment.getMapView().getNavigation().start(currentPosition, selectedObject);
                    }
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
        TextView averageTime = findViewById(R.id.averageTime);
        BarChart chart = findViewById(R.id.chart);
        TextView roomName = findViewById(R.id.room_name);

        Random rand = new Random(selectedObject.hashCode());
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 6; i < 22; i++) {
            entries.add(new BarEntry(i, 0));
        }
        for (int i = 0; i < 250; i++) {
            int x = (int)(rand.nextGaussian()*(entries.size()/8)+(entries.size()/2));
            x = Math.min(Math.max(x, 0), entries.size()-1);
            BarEntry e = entries.get(x);
            e.setY(e.getY()+1);
        }

        averageTime.setText((5+rand.nextInt(40))+" minut");

        BarDataSet set = new BarDataSet(entries, "BarDataSet");
        set.setColor(Color.WHITE);

        BarData data = new BarData(set);
        data.setValueTextColor(Color.WHITE);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getXAxis().setLabelCount(12);

        data.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return String.valueOf((int)value);
            }
        });

        chart.setDescription(null);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

//        data.setBarWidth(0.6f); // set custom bar width
        chart.setData(data);
        chart.setFitBars(true); // make the x-axis fit exactly all bars
        chart.invalidate(); // refresh

        peopleAmount.setText(String.valueOf(getPeopleCount(selectedObject)));
        roomName.setText(mapFragment.getCurrentMap().objectWithId(selectedObject).getName());
    }

    private int getPeopleCount(String roomId) {
        int amount = 0;
        if (peopleCount.containsKey(roomId)) {
            amount = peopleCount.get(roomId);
        } else {
            Log.w(IndoorwayConstants.LOG_TAG, "No data for room: " + roomId);
        }
        return amount;
    }

    public void setRoomColor(String roomId, int color) {
        assert mapFragment.getCurrentMap() != null;

        IndoorwayObjectParameters originalRoom = mapFragment.getCurrentMap().objectWithId(roomId);

        if (originalRoom != null) {
            roomsLayer.add(new DrawablePolygon(roomId + "Overlay", originalRoom.getCoordinates(), color));
        }
    }
}
