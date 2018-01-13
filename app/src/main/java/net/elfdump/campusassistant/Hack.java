package net.elfdump.campusassistant;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Hack {

    public static void setFinal(Object owner, String fieldName, Object value) {

        try {
            for (Field f : owner.getClass().getDeclaredFields()) {
                Log.e("asd", f.getName());
            }

            Field field = owner.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            Field modifierField = field.getClass().getDeclaredField("accessFlags");
            modifierField.setAccessible(true);

            field.set(owner, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logField(Object owner, String fieldName)
    {
        try {
            Field field = owner.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Log.e("dfnojfjsnfoj", field.get(owner).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
