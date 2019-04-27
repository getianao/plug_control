package com.whut.getianao.plugcontrol.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.api.GizWifiSDK;
import com.gizwits.gizwifisdk.enumration.GizEventType;
import com.gizwits.gizwifisdk.enumration.GizUserAccountType;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.gizwifisdk.listener.GizWifiDeviceListener;
import com.gizwits.gizwifisdk.listener.GizWifiSDKListener;
import com.whut.getianao.plugcontrol.DeviceAdapter;
import com.whut.getianao.plugcontrol.R;
import com.whut.getianao.plugcontrol.SharePreUtil;
import com.whut.getianao.plugcontrol.base.ActivityCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends BaseActivity {

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private List<GizWifiDevice> mList;
    private String uid;
    private String token;
    private DeviceAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean isLogin = false;

    /************************ GizWifiSDKListener **************************/
    // 实现系统事件通知回调
    @Override
    public void didNotifyEvent(GizEventType eventType, Object eventSource, GizWifiErrorCode eventID, String eventMessage) {
        if (eventType == GizEventType.GizEventSDK) {
            // SDK发生异常的通知
            Log.i("GizWifiSDK", "SDK event happened: " + eventID + ", " + eventMessage);
            Toast.makeText(MainActivity.this, "SDK初始化成功", Toast.LENGTH_SHORT).show();
            isLogin = true;
            GizWifiSDK.sharedInstance().userLoginAnonymous();
        } else if (eventType == GizEventType.GizEventDevice) {
            // 设备连接断开时可能产生的通知
            GizWifiDevice mDevice = (GizWifiDevice) eventSource;
            Log.i("GizWifiSDK", "device mac: " + mDevice.getMacAddress() +
                    " disconnect caused by eventID: " + eventID + ", eventMessage: " + eventMessage);
        } else if (eventType == GizEventType.GizEventM2MService) {
            // M2M服务返回的异常通知
            Log.i("GizWifiSDK", "M2M domain " + (String) eventSource +
                    " exception happened, eventID: " + eventID + ", eventMessage: " + eventMessage);
        } else if (eventType == GizEventType.GizEventToken) {
            // token失效通知
            Log.i("GizWifiSDK", "token " + (String) eventSource + " expired: " + eventMessage);
        }
    }

    //登陆回调
    @Override
    public void didUserLogin(GizWifiErrorCode result, String _uid, String _token) {
        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
            //登陆成功,保存消息
            uid = _uid;
            token = _token;
            SharePreUtil.putString(MainActivity.this, "_uid", _uid);
            SharePreUtil.putString(MainActivity.this, "_token", _token);
            Toast.makeText(MainActivity.this, "登陆成功", Toast.LENGTH_SHORT).show();
        } else {
            //登陆失败
            Toast.makeText(MainActivity.this, "登陆失败：" + result.name(), Toast.LENGTH_SHORT).show();
        }
    }

    //发现设备回调
    @Override
    protected void didDiscovered(GizWifiErrorCode result, List<GizWifiDevice> deviceList) {
        mSwipeRefreshLayout.setRefreshing(false);
        if (result != GizWifiErrorCode.GIZ_SDK_SUCCESS) {
            Log.d("", "result: " + result.name());
            Toast.makeText(MainActivity.this, "获取设备列表失败：" + result.name(), Toast.LENGTH_SHORT).show();
        } else {//成功获取列表
            if (deviceList.size() == 0) {//列表为空
                Toast.makeText(MainActivity.this, "设备列表为空，快去添加你的的第一个设备吧！", Toast.LENGTH_SHORT).show();
                mList.clear();
                //更新ui
                mHandler.sendEmptyMessage(108);
            } else {//列表不为空
                Toast.makeText(MainActivity.this, "获取设备列表成功", Toast.LENGTH_SHORT).show();
                mList.clear();
                mList.addAll(deviceList);//不能直接赋值
                //因为退出app后所有设备将会自动退订，所以这里将所有绑定了设备自动订阅
                for (int i = 0; i < deviceList.size(); i++) {
                    if (deviceList.get(i).isBind() == true && deviceList.get(i).isSubscribed() == false) {
                        deviceList.get(i).setSubscribe(true);
                    }
                }
                //更新ui
                mHandler.sendEmptyMessage(108);
            }
        }
    }

    @Override
    public void didUnbindDevice(GizWifiErrorCode result, String did) {
        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
            // 解绑成功
            Toast.makeText(this, "解绑成功", Toast.LENGTH_SHORT).show();
        } else {
            // 解绑失败
            Toast.makeText(this, "解绑失败:" + result, Toast.LENGTH_SHORT).show();
        }
    }

    /************************ GizWifiDeviceListener **************************/
    @Override
    protected void didSetSubscribe(GizWifiErrorCode result, GizWifiDevice device, boolean isSubscribed) {
        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
            //订阅或解除订阅成功,离线也可以订阅
        } else {
            //订阅或解除订阅失败
        }
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //设备列表更新
                case 108:
                    adapter.notifyDataSetChanged();
                    break;
                default:
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onResume() {
        //AlertDialog，toast都不会onResume
        super.onResume();
        if (isLogin == true) {
            //Resume时自动刷新
            mSwipeRefreshLayout.setRefreshing(true);
            initDeviceList();
        }
    }

    //初始化
    private void init() {
        initSDK();
        checkAndroidPermission();
        initView();
    }

    private void initSDK() {
        // 设置 AppInfo
        final ConcurrentHashMap<String, String> appInfo = new ConcurrentHashMap<>();
        appInfo.put("appId", getResources().getString(R.string.appId));
        appInfo.put("appSecret", getResources().getString(R.string.appSecret));
        //初始化Sdk
        GizWifiSDK.sharedInstance().startWithAppInfo(getApplicationContext(), appInfo,
                null, null, false);
    }


    //视图初始化
    @SuppressLint("ResourceAsColor")
    private void initView() {
        //toolbar
        mToolbar = findViewById(R.id.tool_bar_main);
        mToolbar.setTitle("我的插头");
        setSupportActionBar(mToolbar);
        //SwipeRefreshLayout
        mSwipeRefreshLayout = findViewById(R.id.swipe_view);
        mSwipeRefreshLayout.setColorSchemeColors(R.color.colorAccent);
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
                if (mList.size() == 0) {
                    Toast.makeText(MainActivity.this, "设备列表为空，快去添加你的的第一个设备吧！", Toast.LENGTH_SHORT).show();
                }
            }
        }, 3000);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            //下拉更新列表
            @Override
            public void onRefresh() {
                initDeviceList();
            }
        });

        //RecyclerView
        mRecyclerView = findViewById(R.id.recycle_view);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(manager);
        mList = GizWifiSDK.sharedInstance().getDeviceList();// 使用缓存的设备列表刷新UI,并检查更新
        adapter = new DeviceAdapter(mList, MainActivity.this);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL));//分割线
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toobar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_plug:
                startActivity(new Intent(MainActivity.this, NetConfigActivity.class));
                break;
            default:
        }
        return true;
    }

    //运行时权限
    private void checkAndroidPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestRunPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.READ_PHONE_STATE});
        }
    }

    //检查权限
    private void requestRunPermission(String[] strings) {
        for (String permission : strings) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, strings, 1);
            }
        }
    }

    //请求权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    List<String> denyPermission = new ArrayList<>();
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            denyPermission.add(permissions[i]);
                        }
                    }
                    //存在没有通过的权限则退出
                    if (!denyPermission.isEmpty()) {
                        Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_LONG).show();
                        ActivityCollector.finishAll();
                    }
                }
        }
    }

    //返回已绑定的设备列表
    public void initDeviceList() {
        GizWifiSDK.sharedInstance().getBoundDevices(uid, token);
    }
}
