package net.elfdump.campusassistant;

import android.support.annotation.NonNull;
import android.util.Log;

import com.indoorway.android.common.sdk.model.IndoorwayObjectParameters;

public final class IndoorwayConstants {
    public static final String API_KEY = "72e6a109-b1d5-41cd-832c-3847a6716152";
    public static final String BUILDING_UUID = "CScrSxCVhQg";
    public static final String FLOOR0_UUID = "7-QLYjkafkE";
    public static final String FLOOR1_UUID = "gVI7XXuBFCQ";
    public static final String FLOOR2_UUID = "3-_M01M3r5w";
    public static final String[] FLOOR_UUIDS = {FLOOR0_UUID, FLOOR1_UUID, FLOOR2_UUID};

    public static final String ROOM_216_UUID = "3-_M01M3r5w_ca808"; // POKÓJ Z JEDZENIEM
    public static final String ROOM_213_UUID = "3-_M01M3r5w_fe9c8"; // Nasz pokój

    public static final String LOG_TAG = "CampusAssistant";

    public static boolean isRoom(@NonNull IndoorwayObjectParameters room) {
        Log.e(LOG_TAG, room.getId() + " " + room.getName() + " " + room.getType());
        assert room.getType() != null;
        return room.getName() != null && room.getType().equals("room") && !room.getName().contains("Corridor") && !room.getName().contains("Floor") && !room.getName().contains("Parter"); //TODO
    }
}
