package net.elfdump.campusassistant;

import android.app.Application;
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

public class MyApplication extends Application {
    OnProximityEventListener proximityEventListener = new OnProximityEventListener() {
        @Override
        public void onEvent(IndoorwayProximityEvent proximityEvent, IndoorwayProximityEvent.Source source) {
            // show notification using event title, description, url etc.
            // track conversion using proximityEvent.getUuid()
            Log.w(IndoorwayConstants.LOG_TAG, proximityEvent.toString());
            Toast.makeText(MyApplication.this, proximityEvent.toString(), Toast.LENGTH_LONG).show();
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
        visitor.setShareLocation(true);
        IndoorwaySdk.instance().visitor().setup(visitor);
        Log.i(IndoorwayConstants.LOG_TAG, "App started");

        StandardBackgroundNotificationBuilder notificationBuilder = new StandardBackgroundNotificationBuilder(
            "channel-id",
            "channel-name",
            "title",
            "description",
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

                    for(IndoorwayObjectParameters obj : indoorwayMap.getObjects()) {
                        Log.i(IndoorwayConstants.LOG_TAG, obj.getId()+";"+obj.getName()+";"+obj.getType()+";"+obj.getCenterPoint().toString());
                    }

                    IndoorwayLocationSdk.instance().customProximityEvents()
                        .add(new IndoorwayProximityEvent(
                            "proximity-event-id-enter", // identifier
                            IndoorwayProximityEvent.Trigger.ENTER, // trigger on enter or on exit?
                            new IndoorwayProximityEventShape.Circle(
                                indoorwayMap.objectWithId(IndoorwayConstants.ROOM_216_UUID).getCenterPoint(),
                                5.0
                            ),
                            IndoorwayConstants.BUILDING_UUID,
                            IndoorwayConstants.FLOOR2_UUID,
                            0L,
                            new IndoorwayNotificationInfo("title", "description", "url", "image")
                        ));

                    IndoorwayLocationSdk.instance().customProximityEvents()
                        .add(new IndoorwayProximityEvent(
                            "proximity-event-id-exit", // identifier
                            IndoorwayProximityEvent.Trigger.EXIT, // trigger on enter or on exit?
                            new IndoorwayProximityEventShape.Circle(
                                indoorwayMap.objectWithId(IndoorwayConstants.ROOM_216_UUID).getCenterPoint(),
                                5.0
                            ),
                            IndoorwayConstants.BUILDING_UUID,
                            IndoorwayConstants.FLOOR2_UUID,
                            0L,
                            new IndoorwayNotificationInfo("title", "description", "url", "image")
                        ));
                }
            })
            .setOnFailedListener(new Action1<IndoorwayTask.ProcessingException>() {
                @Override
                public void onAction(IndoorwayTask.ProcessingException e) {
                    // handle error, original exception is given on e.getCause()
                    Log.e(IndoorwayConstants.LOG_TAG, "lol no jak to "+e.toString());
                }
            })
            .execute();
    }
}
