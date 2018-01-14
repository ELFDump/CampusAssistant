package net.elfdump.campusassistant;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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
import com.indoorway.android.location.sdk.background.BackgroundNotificationBuilder;
import com.indoorway.android.location.sdk.background.StandardBackgroundNotificationBuilder;
import com.indoorway.android.location.sdk.listeners.OnProximityEventListener;

import net.elfdump.campusassistant.api.RestClient;
import net.elfdump.campusassistant.api.model.UserPlaceEvent;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class MyApplication extends Application {
    private static final String RUNNING_NOTIFICATION_CHANNEL_ID = "CampusAssistant-running";
    private static final String NOTIFICATION_CHANNEL_ID = "CampusAssistant-notification";
    private static final int ENTRY_EXIT_NOTIFICATION_ID = 1234;

    private static class SendUserPlaceEvent extends AsyncTask<UserPlaceEvent, Void, Void> {
        @Override
        protected Void doInBackground(UserPlaceEvent... userPlaceEvents) {
            for(UserPlaceEvent userPlaceEvent : userPlaceEvents) {
                try {
                    getRestClient().location().roomChange(userPlaceEvent).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    OnProximityEventListener proximityEventListener = new OnProximityEventListener() {
        @Override
        public void onEvent(IndoorwayProximityEvent proximityEvent, IndoorwayProximityEvent.Source source) {
            Log.w(IndoorwayConstants.LOG_TAG, proximityEvent.toString());
//            Toast.makeText(MyApplication.this, proximityEvent.toString(), Toast.LENGTH_LONG).show();

            IndoorwayProximityEvent.Trigger trigger = proximityEvent.getTrigger();
            String roomId = proximityEvent.getIdentifier().split("\\+")[0];

            PendingIntent contentIntent = PendingIntent.getActivity(MyApplication.this, 0,
                new Intent(MyApplication.this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(MyApplication.this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Proximity event triggered!")
                .setContentText(roomId + " " + trigger.toString())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(proximityEvent.toString()))
                .setContentIntent(contentIntent)
                .build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
            notificationManager.notify(ENTRY_EXIT_NOTIFICATION_ID, notification);

            UserPlaceEvent restEvent = new UserPlaceEvent();
            restEvent.setUserId(Preferences.getUserUUID(MyApplication.this));
            restEvent.setPlaceId(roomId);
            restEvent.setAction(trigger == IndoorwayProximityEvent.Trigger.ENTER ? UserPlaceEvent.PlaceAction.ENTER : UserPlaceEvent.PlaceAction.LEAVE);
            restEvent.setTimeInMilliseconds(new Date().getTime());
            new SendUserPlaceEvent().execute(restEvent);
        }
    };

    public class MyBackgroundNotificationBuilder implements BackgroundNotificationBuilder {
        @NotNull
        @Override
        public Notification buildNotificationForService(Context context) {
            PendingIntent contentIntent = PendingIntent.getActivity(MyApplication.this, 0,
                new Intent(MyApplication.this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            return new NotificationCompat.Builder(MyApplication.this, RUNNING_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("CampusAssistant")
                .setContentText("CampusAssistant jest aktywny!")
                .setContentIntent(contentIntent)
                .build();
        }
    }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "CampusAssistant: notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Powiadomienia o wchodzeniu/wychodzeniu [DEBUG]");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{ 100, 100, 100, 100, 100, 500 });
            notificationManager.createNotificationChannel(channel);

            NotificationChannel channel2 = new NotificationChannel(RUNNING_NOTIFICATION_CHANNEL_ID, "CampusAssistant: activity", NotificationManager.IMPORTANCE_MIN);
            channel2.setDescription("Powiadomienia o działaniu aplikacji");
            channel2.enableLights(false);
            channel2.enableVibration(false);
            notificationManager.createNotificationChannel(channel2);
        }

        StandardBackgroundNotificationBuilder notificationBuilder = new StandardBackgroundNotificationBuilder(
            RUNNING_NOTIFICATION_CHANNEL_ID,
            "CampusAssistant: running",
            "CampusAssistant",
            "CampusAssistant is running!",
            R.drawable.ic_launcher_foreground
        );
        IndoorwayLocationSdk.background().enable(new MyBackgroundNotificationBuilder());

        IndoorwayLocationSdk.background().setCustomProximityEventListener(proximityEventListener);

        // For each floor in the building...
        IndoorwaySdk.instance()
            .building()
            .listMaps(IndoorwayConstants.BUILDING_UUID)
            .setOnCompletedListener(new Action1<List<IndoorwayObjectId>>() {
                @Override
                public void onAction(List<IndoorwayObjectId> floors) {
                    for (final IndoorwayObjectId floor : floors) {
                        // ... retrieve the map ...
                        IndoorwaySdk.instance()
                            .map()
                            .details(IndoorwayConstants.BUILDING_UUID, floor.getUuid())
                            .setOnCompletedListener(new Action1<IndoorwayMap>() {
                                @Override
                                public void onAction(IndoorwayMap indoorwayMap) {
                                    Log.i(IndoorwayConstants.LOG_TAG, "Mamy mapę dla " + floor.getName());

                                    for (IndoorwayObjectParameters obj : indoorwayMap.getObjects()) {
                                        Log.i(IndoorwayConstants.LOG_TAG, obj.getId() + ";" + obj.getName() + ";" + obj.getType() + ";" + obj.getCenterPoint().toString());
                                    }

                                    // ... and for each room ...
                                    for (IndoorwayObjectParameters room : indoorwayMap.getObjects()) {
                                        if (!IndoorwayConstants.isRoom(room))
                                            continue; // I said, for each ROOM

                                        // ... register the proximity events

                                        IndoorwayLocationSdk.instance().customProximityEvents()
                                            .add(new IndoorwayProximityEvent(
                                                room.getId() + "+enter",
                                                IndoorwayProximityEvent.Trigger.ENTER,
                                                new IndoorwayProximityEventShape.Polygon(room.getCoordinates()),
                                                IndoorwayConstants.BUILDING_UUID,
                                                floor.getUuid()
                                            ));

                                        IndoorwayLocationSdk.instance().customProximityEvents()
                                            .add(new IndoorwayProximityEvent(
                                                room.getId() + "+exit",
                                                IndoorwayProximityEvent.Trigger.EXIT,
                                                new IndoorwayProximityEventShape.Polygon(room.getCoordinates()),
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


    private static RestClient restClient = null;

    public static RestClient getRestClient() {
        if (restClient == null) {
            restClient = new RestClient();
        }

        return restClient;
    }
}
