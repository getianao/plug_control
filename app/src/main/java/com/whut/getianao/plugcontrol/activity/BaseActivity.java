package com.whut.getianao.plugcontrol.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.api.GizWifiSDK;

import com.gizwits.gizwifisdk.enumration.GizEventType;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.gizwifisdk.listener.GizWifiDeviceListener;
import com.gizwits.gizwifisdk.listener.GizWifiSDKListener;
import com.whut.getianao.plugcontrol.base.ActivityCollector;


import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BaseActivity extends AppCompatActivity {

    /************************ GizWifiSDKListener **************************/
    protected GizWifiSDKListener mListener = new GizWifiSDKListener() {

        @Override
        public void didUnbindDevice(GizWifiErrorCode result, String did) {
            BaseActivity.this.didUnbindDevice(result, did);
        }

        @Override
        public void didNotifyEvent(GizEventType eventType, Object eventSource, GizWifiErrorCode eventID, String eventMessage) {
            BaseActivity.this.didNotifyEvent(eventType, eventSource, eventID, eventMessage);
        }

        @Override
        public void didDiscovered(GizWifiErrorCode result, List<GizWifiDevice> deviceList) {
            BaseActivity.this.didDiscovered(result, deviceList);
        }

        @Override
        public void didUserLogin(GizWifiErrorCode result, String uid, String token) {
            BaseActivity.this.didUserLogin(result, uid, token);
        }

        @Override
        public void didSetDeviceOnboarding(GizWifiErrorCode result, GizWifiDevice device) {
            BaseActivity.this.didSetDeviceOnboarding(result, device);
        }

    };


    protected void didUnbindDevice(GizWifiErrorCode result, String did) {
    }

    protected void didNotifyEvent(GizEventType eventType, Object eventSource, GizWifiErrorCode eventID, String eventMessage) {
    }

    protected void didUserLogin(GizWifiErrorCode result, String _uid, String _token) {
    }

    protected void didDiscovered(GizWifiErrorCode result, List<GizWifiDevice> deviceList) {
    }

    protected void didSetDeviceOnboarding(GizWifiErrorCode result, GizWifiDevice device) {
    }

    /************************ GizWifiDeviceListener **************************/
    protected GizWifiDeviceListener mDeviceListener = new GizWifiDeviceListener() {
        //设备订阅回调
        @Override
        public void didSetSubscribe(GizWifiErrorCode result, GizWifiDevice device, boolean isSubscribed) {
            BaseActivity.this.didSetSubscribe(result, device, isSubscribed);
        }

        //控制动作回调
        @Override
        public void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
                                   ConcurrentHashMap<String, Object> dataMap, int sn) {
            BaseActivity.this.didReceiveData(result, device, dataMap, sn);
        }
    };

    protected void didSetSubscribe(GizWifiErrorCode result, GizWifiDevice device, boolean isSubscribed) {
    }

    protected void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
                                  ConcurrentHashMap<String, Object> dataMap, int sn) {
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivitiy(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //每次启动activity都要注册一次sdk监听器，保证sdk状态能正确回调！！！！！！！！！
        //每个activity对应一个mListener，Resume要设置回来
        GizWifiSDK.sharedInstance().setListener(mListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivitiy(this);
    }
}
