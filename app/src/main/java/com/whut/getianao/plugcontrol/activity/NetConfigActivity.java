package com.whut.getianao.plugcontrol.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.api.GizWifiSDK;
import com.gizwits.gizwifisdk.enumration.GizWifiConfigureMode;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.gizwifisdk.enumration.GizWifiGAgentType;
import com.whut.getianao.plugcontrol.R;
import com.whut.getianao.plugcontrol.SharePreUtil;

import java.util.ArrayList;
import java.util.List;

public class NetConfigActivity extends BaseActivity {

    private Toolbar mToolbar;
    private EditText etPas;
    private EditText etSSID;
    private Button btnSearch;
    private ProgressDialog mProgressDialog;
    private String uid;
    private String token;

    /************************ GizWifiSDKListener **************************/
    //配网回调
    @Override
    public void didSetDeviceOnboarding(GizWifiErrorCode result, GizWifiDevice device) {
        super.didSetDeviceOnboarding(result, device);
        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
            // 配置成功
            mHandler.sendEmptyMessage(105);
            //设备配置成功后自动订阅，将自动绑定和自动登录
            device.setSubscribe(true);
        } else if (result == GizWifiErrorCode.GIZ_SDK_DEVICE_CONFIG_IS_RUNNING) {
            // 正在配网
        } else if (result == GizWifiErrorCode.GIZ_SDK_ONBOARDING_STOPPED) {
            // 配网终止
            mHandler.sendEmptyMessage(106);
        } else {
            // 配置失败
            mHandler.sendEmptyMessage(107);
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

    //倒计时线程
    class ClockThread extends Thread {
        int initial = 0;//初始下载进度
        volatile boolean stop = false;// 线程中断信号量
        @Override
        public void run() {
            super.run();
            while (initial < 80) {//设置循环条件
                if (stop==true) {
                    return;
                }
                mProgressDialog.setProgress(initial += 1);
                try {
                    Thread.sleep(1000);//main进程
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ClockThread thread;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 105:
                    mProgressDialog.dismiss();
                    final AlertDialog.Builder builder1 = new AlertDialog.Builder(NetConfigActivity.this);
                    builder1.setMessage("配网成功");
                    builder1.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();//返回主界面
                        }
                    });
                    builder1.create().show();
                    break;
                case 106:
                    mProgressDialog.dismiss();
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(NetConfigActivity.this);
                    builder2.setMessage("配网终止");
                    builder2.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder2.create().show();
                    break;
                case 107:
                    mProgressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(NetConfigActivity.this);
                    builder.setMessage("配网失败");
                    builder.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.create().show();
                    break;
                default:
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_config);
        init();
    }

    //初始化
    private void init() {
        //获取用户名
        uid=SharePreUtil.getString(this,"_uid",null);
        token=SharePreUtil.getString(this,"_token",null);
        initView();
    }

    //视图初始化
    private void initView() {
        //toolebar
        mToolbar = findViewById(R.id.tool_bar_net_config);
        mToolbar.setTitle("添加设备");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //editText
        etPas = findViewById(R.id.edit_password);
        etSSID = findViewById(R.id.edit_wifi);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        etSSID.setText(wifiInfo.getSSID().replace("\"", ""));
        etSSID.setEnabled(false);
        //button
        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //wifi名
                String SSID = etSSID.getText().toString();
                //wifi密码
                String password = etPas.getText().toString();
                if (!SSID.isEmpty() && !password.isEmpty()) {
                    mProgressDialog = new ProgressDialog(NetConfigActivity.this);
                    mProgressDialog.setMessage("正在配网...");
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mProgressDialog.setMax(90);
                    mProgressDialog.setProgressNumberFormat(" ");
                    //倒计时开始
                    thread = new ClockThread();
                    thread.start();
                    mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //停止设备配网
                                    GizWifiSDK.sharedInstance().stopDeviceOnboarding();
                                    //倒计时结束
                                    thread.stop=true;
                                }
                            });
                    mProgressDialog.show();
                    startAirlink(SSID, password);
                }
            }
        });
    }

    //开始配网
    private void startAirlink(final String ssid, final String password) {
        //乐鑫模组
        final List<GizWifiGAgentType> types = new ArrayList<GizWifiGAgentType>();
        types.add(GizWifiGAgentType.GizGAgentESP);
        GizWifiSDK.sharedInstance().setDeviceOnboardingDeploy(ssid, password, GizWifiConfigureMode.GizWifiAirLink,
                null, 90, types, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}
