package com.whut.getianao.plugcontrol.base;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityCollector {
    public static List<Activity> sActivities=new ArrayList<>();

    public static void addActivitiy(Activity activity){
        sActivities.add(activity);
    }

    public static void removeActivitiy(Activity activity){
        sActivities.remove(activity);
    }
    public static void finishAll(){
        for(Activity activity :sActivities){
            if(!activity.isFinishing()){
                activity.finish();
            }
        }
    }
}

