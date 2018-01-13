package net.elfdump.campusassistant;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.indoorway.android.common.sdk.IndoorwaySdk;
import com.indoorway.android.common.sdk.listeners.generic.Action1;
import com.indoorway.android.common.sdk.model.IndoorwayMap;
import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;
import com.indoorway.android.common.sdk.model.Visitor;
import com.indoorway.android.common.sdk.model.proximity.IndoorwayNotificationInfo;
import com.indoorway.android.common.sdk.model.proximity.IndoorwayProximityEvent;
import com.indoorway.android.common.sdk.model.proximity.IndoorwayProximityEventShape;
import com.indoorway.android.common.sdk.task.IndoorwayTask;
import com.indoorway.android.location.sdk.IndoorwayLocationSdk;
import com.indoorway.android.location.sdk.background.StandardBackgroundNotificationBuilder;
import com.indoorway.android.location.sdk.listeners.OnProximityEventListener;

import java.util.UUID;

public class MyApplication extends Application {
    OnProximityEventListener proximityEventListener = new OnProximityEventListener() {
        @Override
        public void onEvent(IndoorwayProximityEvent proximityEvent, IndoorwayProximityEvent.Source source) {
            // show notification using event title, description, url etc.
            // track conversion using proximityEvent.getUuid()
            Log.w(IndoorwayConstants.LOG_TAG, proximityEvent.toString());
            Toast.makeText(MyApplication.this, proximityEvent.toString(), Toast.LENGTH_LONG).show();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MyApplication.this);
            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground));
            builder.setContentTitle("Proximity event triggered!");
            builder.setContentText(proximityEvent.toString());
            builder.setSubText(proximityEvent.toString());
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
        Log.i(IndoorwayConstants.LOG_TAG, "App started, visitor: "+visitor.toString());

        StandardBackgroundNotificationBuilder notificationBuilder = new StandardBackgroundNotificationBuilder(
            "CampusAssistant-running",
            "CampusAssistant: running",
            "CampusAssistant",
            "CampusAssistant is running!",
            R.drawable.ic_launcher_foreground
        );
        IndoorwayLocationSdk.background().enable(notificationBuilder);

        IndoorwayLocationSdk.background().setCustomProximityEventListener(proximityEventListener);

        IndoorwaySdk.instance()
            .map()
            .details(IndoorwayConstants.BUILDING_UUID, IndoorwayConstants.FLOOR2_UUID)
            .setOnCompletedListener(new Action1<IndoorwayMap>() {
                @Override
                public void onAction(IndoorwayMap indoorwayMap) {
                    Log.i(IndoorwayConstants.LOG_TAG, "Mamy mapę");

                    for (IndoorwayObjectParameters obj : indoorwayMap.getObjects()) {
                        Log.i(IndoorwayConstants.LOG_TAG, obj.getId() + ";" + obj.getName() + ";" + obj.getType() + ";" + obj.getCenterPoint().toString());
                    }

                    IndoorwayObjectParameters room216 = indoorwayMap.objectWithId(IndoorwayConstants.ROOM_216_UUID);
                    assert room216 != null;

                    IndoorwayLocationSdk.instance().customProximityEvents()
                        .add(new IndoorwayProximityEvent(
                            "proximity-event-id-enter",
                            IndoorwayProximityEvent.Trigger.ENTER,
                            new IndoorwayProximityEventShape.Polygon(room216.getCoordinates()),
                            IndoorwayConstants.BUILDING_UUID,
                            IndoorwayConstants.FLOOR2_UUID
                        ));

                    IndoorwayLocationSdk.instance().customProximityEvents()
                        .add(new IndoorwayProximityEvent(
                            "proximity-event-id-exit",
                            IndoorwayProximityEvent.Trigger.EXIT,
                            new IndoorwayProximityEventShape.Polygon(room216.getCoordinates()),
                            IndoorwayConstants.BUILDING_UUID,
                            IndoorwayConstants.FLOOR2_UUID
                        ));
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
