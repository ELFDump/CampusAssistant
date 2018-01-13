package net.elfdump.campusassistant;

import android.app.Application;
import android.app.NotificationManager;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.indoorway.android.common.sdk.IndoorwaySdk;
import com.indoorway.android.common.sdk.listeners.generic.Action1;
import com.indoorway.android.common.sdk.model.IndoorwayMap;
import com.indoorway.android.common.sdk.model.IndoorwayObjectId;
import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;
import com.indoorway.android.common.sdk.model.Visitor;
import com.indoorway.android.common.sdk.model.proximity.IndoorwayProximityEvent;
import com.indoorway.android.common.sdk.model.proximity.IndoorwayProximityEventShape;
import com.indoorway.android.common.sdk.task.IndoorwayTask;
import com.indoorway.android.location.sdk.IndoorwayLocationSdk;
import com.indoorway.android.location.sdk.background.StandardBackgroundNotificationBuilder;
import com.indoorway.android.location.sdk.listeners.OnProximityEventListener;

import java.util.List;

public class MyApplication extends Application {
    OnProximityEventListener proximityEventListener = new OnProximityEventListener() {
        @Override
        public void onEvent(IndoorwayProximityEvent proximityEvent, IndoorwayProximityEvent.Source source) {
            // show notification using event title, description, url etc.
            // track conversion using proximityEvent.getUuid()
            Log.w(IndoorwayConstants.LOG_TAG, proximityEvent.toString());
            Toast.makeText(MyApplication.this, proximityEvent.toString(), Toast.LENGTH_LONG).show();

            IndoorwayProximityEvent.Trigger trigger = proximityEvent.getTrigger();
            String roomId = proximityEvent.getIdentifier().split("\\+")[0];

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MyApplication.this);
            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground));
            builder.setContentTitle("Proximity event triggered!");
            builder.setContentText(proximityEvent.toString());
            builder.setSubText(roomId+" "+trigger.toString());
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
            notificationManager.notify(1234, builder.build());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // init application context on each Application start
        IndoorwaySdk.initContext(this);

        // it's up to you when to initialize IndoorwaySdk, once initialized it will work forever!
        IndoorwaySdk.configure(IndoorwayConstants.API_KEY);

        Visitor visitor = new Visitor();
        visitor.setName(Preferences.getUserUUID(this));
        visitor.setShareLocation(true);
        IndoorwaySdk.instance().visitor().setup(visitor);
        Log.i(IndoorwayConstants.LOG_TAG, "App started, visitor: " + visitor.toString());

        StandardBackgroundNotificationBuilder notificationBuilder = new StandardBackgroundNotificationBuilder(
            "CampusAssistant-running",
            "CampusAssistant: running",
            "CampusAssistant",
            "CampusAssistant is running!",
            R.drawable.ic_launcher_foreground
        );
        IndoorwayLocationSdk.background().enable(notificationBuilder);

        IndoorwayLocationSdk.background().setCustomProximityEventListener(proximityEventListener);

        // For each floor in the building...
        IndoorwaySdk.instance()
            .building()
            .listMaps(IndoorwayConstants.BUILDING_UUID)
            .setOnCompletedListener(new Action1<List<IndoorwayObjectId>>() {
                @Override
                public void onAction(List<IndoorwayObjectId> floors) {
                    for(final IndoorwayObjectId floor : floors) {
                        // ... retrieve the map ...
                        IndoorwaySdk.instance()
                            .map()
                            .details(IndoorwayConstants.BUILDING_UUID, floor.getUuid())
                            .setOnCompletedListener(new Action1<IndoorwayMap>() {
                                @Override
                                public void onAction(IndoorwayMap indoorwayMap) {
                                    Log.i(IndoorwayConstants.LOG_TAG, "Mamy mapę dla "+floor.getName());

                                    for (IndoorwayObjectParameters obj : indoorwayMap.getObjects()) {
                                        Log.i(IndoorwayConstants.LOG_TAG, obj.getId() + ";" + obj.getName() + ";" + obj.getType() + ";" + obj.getCenterPoint().toString());
                                    }

                                    // ... and for each room on the selectable list ...
                                    for (String room : IndoorwayConstants.SELECTABLE_ROOMS) {
                                        IndoorwayObjectParameters roomParams = indoorwayMap.objectWithId(room);
                                        if (roomParams == null) {
                                            // Probably on a different floor
                                            return;
                                        }

                                        // ... register the proximity events

                                        IndoorwayLocationSdk.instance().customProximityEvents()
                                            .add(new IndoorwayProximityEvent(
                                                room+"+enter",
                                                IndoorwayProximityEvent.Trigger.ENTER,
                                                new IndoorwayProximityEventShape.Polygon(roomParams.getCoordinates()),
                                                IndoorwayConstants.BUILDING_UUID,
                                                floor.getUuid()
                                            ));

                                        IndoorwayLocationSdk.instance().customProximityEvents()
                                            .add(new IndoorwayProximityEvent(
                                                room+"+exit",
                                                IndoorwayProximityEvent.Trigger.EXIT,
                                                new IndoorwayProximityEventShape.Polygon(roomParams.getCoordinates()),
                                                IndoorwayConstants.BUILDING_UUID,
                                                floor.getUuid()
                                            ));
                                    }
                                }
                            })
                            .setOnFailedListener(new Action1<IndoorwayTask.ProcessingException>() {
                                @Override
                                public void onAction(IndoorwayTask.ProcessingException e) {
                                    // handle error, original exception is given on e.getCause()
                                    Log.e(IndoorwayConstants.LOG_TAG, "lol no jak to " + e.toString());
                                }
                            })
                            .execute();
                    }
                }
            })
            .setOnFailedListener(new Action1<IndoorwayTask.ProcessingException>() {
                @Override
                public void onAction(IndoorwayTask.ProcessingException e) {
                    // handle error, original exception is given on e.getCause()
                    Log.e(IndoorwayConstants.LOG_TAG, "looooooool " + e.toString());
                }
            })
            .execute();
    }
}
