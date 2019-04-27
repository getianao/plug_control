package com.whut.getianao.plugcontrol.base;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;


public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initApp();
    }

    //应用初始化
    private void initApp() {

    }
}


