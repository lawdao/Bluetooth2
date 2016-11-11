package example.fussen.bluetooth2.base;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import example.fussen.bluetooth2.bean.MyBleDevice;
import example.fussen.bluetooth2.service.BleService;
import example.fussen.bluetooth2.utils.LogUtil;
import example.fussen.bluetooth2.utils.ToastUtil;

public abstract class BaseActivity extends AppCompatActivity {

    private Messenger mService = null;
    private Messenger mMessenger;
    private static final int MSG_BIND_SUCCESS = 1;
    protected Intent mServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //设置页面布局
        setContentView(getContentView());

        //创建服务intent
        mServiceIntent = new Intent(this, BleService.class);

        //创建activity中的Messenger
        mMessenger = new Messenger(new IncomingHandler(this));

        initView();
    }

    //子类必须实现此方法
    public abstract int getContentView();

    public abstract void initView();


    @Override
    protected void onResume() {
        super.onResume();

        //绑定服务
//        bindBleService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterMessage();
    }

    //绑定服务
    protected void bindBleService() {
        bindService(mServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    //与服务的连接回调
    private ServiceConnection serviceConnection = new ServiceConnection() {

        // 当与service的连接建立后被调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            LogUtil.fussenLog().d("1008611" + "=========onServiceConnected===============");

            // 客户端(activity) 与 服务 不在同一个进程中的话，是不可以进行显示强制类型转换的
            // 使用从Service返回的IBinder来生成一个Messenger
            mService = new Messenger(service);

            // 生成一个Message
            Message msg = Message.obtain();
            if (msg != null) {
                msg.what = MSG_BIND_SUCCESS;
                msg.replyTo = mMessenger;
                // 向Service 发送Message
                sendMessage(msg);
            } else {
                mService = null;
            }

            //activity与service绑定成功

        }

        // 当与service的连接意外断开时被调用
        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.fussenLog().d("1008611" + "=========onServiceDisconnected===============");
            mService = null;
        }
    };


    // client(activity端) 端 Handler 的实现
    private class IncomingHandler extends Handler {

        private final WeakReference<BaseActivity> mActivity;

        public IncomingHandler(BaseActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        /*
         * 处理从Service发送至该Activity的消息
         */
        @Override
        public void handleMessage(Message msg) {
            BaseActivity activity = mActivity.get();

            if (activity != null) {
                switch (msg.what) {
                    case BleService.MSG_BLUETOOTH_OFF://检测到蓝牙已关闭
                        activity.stateBluetoothOFF();
                        break;
                    case BleService.MSG_BLUETOOTH_ON://检测到蓝牙已开启
                        ToastUtil.showToast("检测到蓝牙已打开");
                        break;
                    case BleService.SCAN_BLE_DEVICE_RESULT://搜索蓝牙结果
                        int result = msg.arg1;
                        ArrayList<MyBleDevice> myBleDevices = (ArrayList<MyBleDevice>) msg.obj;
                        activity.scanBleResult(result, myBleDevices);
                        break;
                    case BleService.MSG_DEVICE_CONNECT_STATE://设备是否连接成功
                        activity.deviceConnectionState((boolean) msg.obj);
                        break;

                    case BleService.MSG_SEND_DATA_TIME_OUT://发送指令超时 超时次数为3次 每次3秒
                        activity.sendDataTimeOut();
                        break;
                    case BleService.MSG_BLE_WRITE_NAME_SUCCEED://更改设备名称成功
                        activity.msgBtChangeName((byte[]) msg.obj);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }

        }
    }

    /**
     * 更改设备名称成功返回的信息
     *
     * @param data
     */
    protected void msgBtChangeName(byte[] data) {
    }


    /**
     * 更改设备名称
     *
     * @param name 更改的名称
     */
    protected void changeDeviceName(String name) {
        Message msg = Message.obtain(null, BleService.MSG_BLE_WRITE_NAME);
        msg.obj = name;
        sendMessage(msg);
    }

    /**
     * 发送指令超时
     */
    protected void sendDataTimeOut() {
    }

    /**
     * Activity连接设备后请求设备连接状态的返回 和断开连接后接收到的通知
     *
     * @param isConnection
     */
    protected void deviceConnectionState(boolean isConnection) {

    }

    /**
     * 蓝牙关闭时回调
     */
    protected void stateBluetoothOFF() {
        ToastUtil.showToast("检测到蓝牙已关闭");
    }

    /**
     * 扫描蓝牙后的结果 子类可以选择实现
     *
     * @param result       结果码， 8代表 搜到蓝牙设备 9代表 没有搜到任何设备
     * @param myBleDevices 扫描到的结果集合
     */
    protected void scanBleResult(int result, ArrayList<MyBleDevice> myBleDevices) {
    }


    /**
     * ********************************* Activity与Service的交互 ************************************
     */

    protected synchronized void sendMessage(Message msg) {
        if (msg != null && mService != null) {
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                unbindService(serviceConnection);
            }
        }
    }


    /**
     * 向服务发送消息，activity已销毁，服务应该做收尾工作
     */
    protected void unRegisterMessage() {
        Message msg = Message.obtain(null, BleService.MSG_UNREGISTER);
        if (msg != null) {
            msg.replyTo = mMessenger;
            sendMessage(msg);
        }
    }
}
