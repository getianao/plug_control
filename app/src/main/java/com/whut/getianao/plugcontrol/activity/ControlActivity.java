package com.whut.getianao.plugcontrol.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.whut.getianao.plugcontrol.DeviceAdapter;
import com.whut.getianao.plugcontrol.R;
import com.whut.getianao.plugcontrol.SharePreUtil;

import java.util.concurrent.ConcurrentHashMap;

public class ControlActivity extends BaseActivity {
    private Switch switchOnoff;
    private Switch switchTimer;
    private EditText editHour;
    private EditText editMin;
    private TextView textTimer;
    private int deviceIndex;
    private GizWifiDevice mDevice;
    private boolean isOnoff;
    private boolean isTimerOnoff;
    private long now;//保存当前时间
    private long last;//保存定时时间
    private TimerThread thread;

    /************************ GizWifiDeviceListener **************************/
    @Override
    protected void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
                                  ConcurrentHashMap<String, Object> dataMap, int sn) {
        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
            switch (sn) {
                case 5: //命令序号相符开灯指令执行成功
                    break;
                case 6://定时命令启动
                    Toast.makeText(ControlActivity.this, "定时功能开启", Toast.LENGTH_SHORT).show();
                    //封印
                    switchOnoff.setEnabled(false);
                    //倒计时开始
                    thread = new TimerThread();
                    thread.start();
                    break;
                case 7://定时命令解除
                    Toast.makeText(ControlActivity.this, "定时功能关闭", Toast.LENGTH_SHORT).show();
                    //封印
                    switchOnoff.setEnabled(false);
                    //定时时间清零
                    last = 0;
                    SharePreUtil.putLong(ControlActivity.this, "timer", last);
                    break;
                case 0://状态的主动上报
                    if (dataMap.get("data") != null) {
                        ConcurrentHashMap<String, Object> map = (ConcurrentHashMap<String, Object>) dataMap.get("data");
                        if ((Boolean) map.get("on_off") == false) {
                            switchTimer.setText("定时启动");
                        } else {
                            switchTimer.setText("定时关闭");
                        }
                        if ((Boolean) map.get("T_on_off") == true) {
                            editHour.setText((int) map.get("time_h") + "");
                            editMin.setText((int) map.get("time_m") + "");
                        }
                        switchOnoff.setChecked((Boolean) map.get("on_off"));
                        switchTimer.setChecked((Boolean) map.get("T_on_off"));
                        //定时命令停止，解除封印
                        if (!switchTimer.isChecked()) {
                            //保存时间清零
                            last = 0;
                            SharePreUtil.putLong(ControlActivity.this, "timer", last);
                            switchOnoff.setEnabled(true);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textTimer.setText("");
                                }
                            });
                        }

                    }
                    break;
                default:
            }
        } else {
            // 操作失败
        }
    }

    //倒计时线程
    class TimerThread extends Thread {
        volatile boolean stop = false;// 线程中断信号量

        @Override
        public void run() {
            super.run();
            //当前时间
            now = System.currentTimeMillis();
            int initial = (int) (last - now) / 1000;//初始下载进度
            while (initial > 0) {//设置循环条件
                if (stop == true) {
                    return;
                }
                final int finalInitial = initial;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textTimer.setText("倒计时：" + finalInitial + "秒");
                    }
                });
                initial--;
                try {
                    Thread.sleep(1000);//main进程
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        init();
    }

    private void init() {

        //获得设备
        deviceIndex = getIntent().getIntExtra("index", -1);
        if (deviceIndex >= 0 && DeviceAdapter.mDeviceList != null) {
            mDevice = DeviceAdapter.mDeviceList.get(deviceIndex);
            mDevice.setListener(mDeviceListener);
            initView();
        }
    }

    private void initView() {
        editHour = findViewById(R.id.edit_onoff_hour);
        editMin = findViewById(R.id.edit_onoff_min);
        textTimer = findViewById(R.id.text_onoff_timer);
        //switch
        mDevice.getDeviceStatus(null);//主动上报，初始化swtich
        switchOnoff = findViewById(R.id.switch_onoff);
        switchOnoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 订阅设备并变为可控状态后，执行开灯动作
                isOnoff = isChecked;
                int sn = 5;
                ConcurrentHashMap<String, Object> command = new ConcurrentHashMap<>();
                command.put("on_off", isChecked);
                mDevice.write(command, sn);
            }
        });
        switchTimer = findViewById(R.id.switch_time_onoff);
        switchTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTimerOnoff = switchTimer.isChecked();
                if (editHour.getText().toString().isEmpty() || editMin.getText().toString().isEmpty()) {
                    //存在空项
                    Toast.makeText(ControlActivity.this, "输入不合法", Toast.LENGTH_SHORT).show();
                    switchTimer.setChecked(false);
                    return;
                }
                if (isTimerOnoff) {
                    //开始定时
                    int hour = Integer.parseInt(editHour.getText().toString());
                    int min = Integer.parseInt(editMin.getText().toString());
                    if (hour >= 0 && min >= 0 && hour <= 24 && min <= 60) {//输入合法
                        if (hour == 0 && min == 0) {
                            Toast.makeText(ControlActivity.this, "输入不合法", Toast.LENGTH_SHORT).show();
                            switchTimer.setChecked(false);
                        } else {
                            //定时开启
                            int sn = 6;
                            ConcurrentHashMap<String, Object> command = new ConcurrentHashMap<>();
                            command.put("T_on_off", isTimerOnoff);
                            command.put("time_h", hour);
                            command.put("time_m", min);
                            mDevice.write(command, sn);
                            //保存定时时间
                            last = System.currentTimeMillis() + (hour * 60 + min) * 60 * 1000;
                            SharePreUtil.putLong(ControlActivity.this, "timer", last);
                        }
                    } else {//输入不合法
                        Toast.makeText(ControlActivity.this, "输入不合法", Toast.LENGTH_SHORT).show();
                        switchTimer.setChecked(false);
                    }
                } else {
                    //终止定时
                    int sn = 7;
                    ConcurrentHashMap<String, Object> command = new ConcurrentHashMap<>();
                    command.put("T_on_off", isTimerOnoff);
                    mDevice.write(command, sn);
                }
            }
        });
        // 弃用，不然设置状态时会触发监听
        // switchTimer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

        //倒计时
        now = System.currentTimeMillis();
        last = SharePreUtil.getLong(ControlActivity.this, "timer", 0);
        if (now < last) {
            //倒计时开始
            thread = new TimerThread();
            thread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        thread.stop=true;
    }
}
