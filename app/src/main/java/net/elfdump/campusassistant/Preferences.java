package net.elfdump.campusassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.UUID;

public class Preferences {
    private static String userUUID = null;

    public static String getUserUUID(Context ctx) {
        if (userUUID == null) {
            SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getString(R.string.user_session_key), Context.MODE_PRIVATE);
            if (!sharedPref.contains(ctx.getString(R.string.user_uuid))) {
                userUUID = UUID.randomUUID().toString();
                Log.w(IndoorwayConstants.LOG_TAG, "New user UUID: " + userUUID);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(ctx.getString(R.string.user_uuid), userUUID);
                editor.apply();
            } else {
                userUUID = sharedPref.getString(ctx.getString(R.string.user_uuid), null);
            }
        }

        return userUUID;
    }
}
