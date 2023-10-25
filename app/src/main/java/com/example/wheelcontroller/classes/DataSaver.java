package com.example.wheelcontroller.classes;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class DataSaver {

    private static SharedPreferences sp;
    private static DataSaver dataSaver = null;

    public static DataSaver getInstance(@NonNull Activity activity){
        if(dataSaver == null){
            dataSaver = new DataSaver(activity);
        }
        return dataSaver;
    }

    public boolean isIDPassNotSet(){
        return !( sp.contains("my_id") && sp.contains("my_pass") );
    }

    public String getMyPass(){
        return sp.getString("my_pass",null);
    }
    public String getId(){
        return sp.getString("my_id",null);
    }
    public String getName(){
        return sp.getString("my_name",null);
    }

    public void clearIdPass(){
        SharedPreferences.Editor editor = sp.edit();
        editor.remove("my_id");
        editor.remove("my_pass");
        editor.remove("my_name");
        editor.apply();
    }
    public void saveIdPass(String id, String pass,String name){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("my_id",id);
        editor.putString("my_pass",pass);
        editor.putString("my_name",name);
        editor.apply();
    }

    private DataSaver(Activity activity){
        sp = activity.getPreferences(MODE_PRIVATE);
    }

}
