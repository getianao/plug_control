package com.whut.getianao.plugcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.api.GizWifiSDK;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.gizwifisdk.listener.GizWifiDeviceListener;
import com.gizwits.gizwifisdk.listener.GizWifiSDKListener;
import com.whut.getianao.plugcontrol.activity.ControlActivity;
import com.whut.getianao.plugcontrol.activity.MainActivity;
import com.whut.getianao.plugcontrol.activity.NetConfigActivity;

import java.util.List;

import static com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus.GizDeviceControlled;

public class DeviceAdapter extends RecyclerView.Adapter {
    public static List<GizWifiDevice> mDeviceList;
    private Context mContext;
    private Dialog dialog;
    private String uid;
    private String token;

    /************************ GizWifiDeviceListener **************************/
    private GizWifiDeviceListener mGizWifiDeviceListener = new GizWifiDeviceListener() {
        //修改设备别名回调
        @Override
        public void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                // 修改成功
                Toast.makeText(mContext, "修改成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                // 修改失败
                Toast.makeText(mContext, "修改失败:" + result, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceName;
        private TextView deviceStatus;
        private static View deviceView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            //绑定item组件
            deviceName = itemView.findViewById(R.id.text_device_name);
            deviceStatus = itemView.findViewById(R.id.text_device_status);
            deviceView = itemView;
        }
    }

    public DeviceAdapter(List<GizWifiDevice> deviceList, Context context) {
        mDeviceList = deviceList;
        mContext = context;
        uid = SharePreUtil.getString(context, "_uid", null);
        token = SharePreUtil.getString(context, "_token", null);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_device, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);
        //item短按控制
        viewHolder.deviceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ControlActivity.class);
                intent.putExtra("index", i);
                if (mDeviceList.get(i).getNetStatus() == GizDeviceControlled) {
                    mContext.startActivity(intent);
                } else {
                    Toast.makeText(mContext, "请在可控状态下操作", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //item长按配置
        viewHolder.deviceView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showOnLongClickDialog(mDeviceList.get(i));
                return true;
            }
        });
        return viewHolder;
    }

    //长按弹窗
    private void showOnLongClickDialog(final GizWifiDevice gizWifiDevice) {
        gizWifiDevice.setListener(mGizWifiDeviceListener);
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewHolder.deviceView.getContext());
        LayoutInflater inflater = LayoutInflater.from(ViewHolder.deviceView.getContext());
        View v = inflater.inflate(R.layout.edit_device_info, null);
        final EditText rename = v.findViewById(R.id.edit_rename);
        //有别名自动填充别名
        rename.setText(gizWifiDevice.getAlias());
        Button btnConfirm = v.findViewById(R.id.btn_device_info_confirm);
        Button btnUnbind = v.findViewById(R.id.btn_device_info_unbind);
        Button btnCancel = v.findViewById(R.id.btn_device_info_cancel);
        //弹出软键盘
        builder.setView(((Activity) ViewHolder.deviceView.getContext()).getLayoutInflater().inflate(
                R.layout.edit_device_info, null));
        //builer.setView(v);//这里如果使用builer.setView(v)，自定义布局只会覆盖title和button之间的那部分
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getWindow().setContentView(v);//自定义布局应该在这里添加，要在dialog.show()的后面
        //修改别名
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String newName = rename.getText().toString();
                gizWifiDevice.setCustomInfo(null, newName);
            }
        });
        //解绑
        btnUnbind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //官网说解绑时解除订阅，但实际未必
                GizWifiSDK.sharedInstance().unbindDevice(uid, token, gizWifiDevice.getDid());
                gizWifiDevice.setSubscribe(false);
                //解绑后将从列表中移除
                dialog.dismiss();
            }
        });
        //取消返回
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        GizWifiDevice device = mDeviceList.get(i);
        //按照设备状态显示颜色
        /*状态解释：
         *离线:设备没有连接至局域网和互联网
         *在线:绑定但没订阅，目前不存在这种情况
         *已解绑：未绑定且未订阅（用户在绑定设备后手动解绑，同时将解除订阅）
         *        在用户和设备在同一局域网下显示已解绑，不再同一局域网时不显示
         *可控：绑定且订阅，不管是否在同于局域网下都会显示
         **/

        /*状态测试
         * 1.设备已配网，关闭app重新打开，状态依次显示已解绑、在线、可控
         * 3.设备已配网，将设备连接局域网及互联网，马上显示离线、在线、可控
         * 2.将设备断开局域网及互联网，1分钟后网关显示设备掉线，2分钟后显示离线状态（环境：校园网+windows热点）
         * */
        switch (device.getNetStatus()) {
            case GizDeviceOffline:
                ((ViewHolder) viewHolder).deviceStatus.setText("离线");//强制类型转化
                ((ViewHolder) viewHolder).deviceStatus.setTextColor(
                        ((ViewHolder) viewHolder).deviceView.getContext().getResources().getColor(R.color.grey));
                ((ViewHolder) viewHolder).deviceName.setTextColor(
                        ((ViewHolder) viewHolder).deviceView.getContext().getResources().getColor(R.color.grey));
                break;

            case GizDeviceOnline:
                if (device.isBind() == true) {
                    ((ViewHolder) viewHolder).deviceStatus.setText("在线");
                } else {
                    ((ViewHolder) viewHolder).deviceStatus.setText("已解绑");
                }
                ((ViewHolder) viewHolder).deviceStatus.setTextColor(
                        ((ViewHolder) viewHolder).deviceView.getContext().getResources().getColor(R.color.black));
                ((ViewHolder) viewHolder).deviceName.setTextColor(
                        ((ViewHolder) viewHolder).deviceView.getContext().getResources().getColor(R.color.black));
                break;
            case GizDeviceControlled:
                ((ViewHolder) viewHolder).deviceStatus.setText("可控");
                ((ViewHolder) viewHolder).deviceStatus.setTextColor(
                        ((ViewHolder) viewHolder).deviceView.getContext().getResources().getColor(R.color.colorPrimary));
                ((ViewHolder) viewHolder).deviceName.setTextColor(
                        ((ViewHolder) viewHolder).deviceView.getContext().getResources().getColor(R.color.colorPrimary));
                break;
        }
        if (!device.getAlias().isEmpty()) {//优先使用别名
            ((ViewHolder) viewHolder).deviceName.setText(device.getAlias());
        } else {//否则使用设备名
            ((ViewHolder) viewHolder).deviceName.setText(device.getProductName());
        }

    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }
}
