package com.whitecane;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by barno on 18/3/17.
 */

public class DataHelper {
    static SharedPreferences sharedPreferences;

    public static void setSharedPref(SharedPreferences sp) {
        sharedPreferences = sp;
    }

    public static void saveSharedPrefStr(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getSharedPrefStr(String key) {
        return sharedPreferences.getString(key, "null");
    }
}
