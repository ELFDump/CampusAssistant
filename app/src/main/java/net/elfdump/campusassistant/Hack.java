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

//
//            field.set(owner, value);

//            int modifiers = field.getModifiers();
            Field modifierField = field.getClass().getDeclaredField("accessFlags");
//            modifiers = modifiers & ~Modifier.FINAL;
            modifierField.setAccessible(true);
            modifierField.setInt(field, 2);

            field.set(owner, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
