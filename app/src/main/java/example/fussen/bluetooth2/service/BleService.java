package example.fussen.bluetooth2.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import example.fussen.bluetooth2.bean.MyBleDevice;
import example.fussen.bluetooth2.utils.BleUtils;
import example.fussen.bluetooth2.utils.LogUtil;
import example.fussen.bluetooth2.utils.TLUtil;

/**
 * Created by Fussen on 2016/11/7.
 */

public class BleService extends Service {
    public static final String TAG = "BleService";
    public static final int MSG_REGISTER = 1;
    public static final int MSG_UNREGISTER = 2;
    public static final int MSG_BLUETOOTH_OFF = 3; // 手机蓝牙关闭
    public static final int MSG_BLUETOOTH_ON = 4; // 手机蓝牙打开
    public static final int SCAN_BLE_DEVICE_RESULT = 5; //搜索所有蓝牙设备后的结果
    public static final int MSG_OPEN_BLUETOOTH = 6; //打开蓝牙
    public static final int MSG_START_SCAN = 7; //开始搜索附近蓝牙设备
    public static final int MSG_SCAN_SUCCESS = 8; //搜索到蓝牙设备
    public static final int MSG_SCAN_FAIL = 9; //没有搜索到蓝牙设备
    public static final int CONNECT_BLE_DEVICE = 10; //连接蓝牙设备
    public static final int MSG_DEVICE_CONNECT_STATE = 11; //连接状态
    public static final int MSG_SEND_DATA_TIME_OUT = 12; //发送数据超时
    public static final int MSG_BLE_WRITE_NAME_SUCCEED = 13; //更改蓝牙设备名称成功
    public static final int MSG_CRC8_ERROR = 14; //CRC8验证失败
    public static final int MSG_BLE_WRITE_NAME = 15; //更改设备的名称

    private List<MyBleDevice> mLeDevices = new ArrayList<>(); // 扫描到所有蓝牙设备的集合
    private List<String> mDeviceAddress = new ArrayList<>(); // 扫描到所有设备的物理地址的集合 可以用来过滤掉重复的设备

    private IncomingHandler mHandler;
    private Messenger mMessenger;
    private BluetoothAdapter mBlueToothAdapter;

    private BluetoothDevice mBluetoothDevice;//需要连接的设备device

    private BluetoothGatt mBluetoothGatt;

    private boolean isDeviceConnection = false;//设备连接状态

    private BluetoothGattService mDeviceService;//设备里的服务

    private BluetoothGattCharacteristic cmdRespondCharacter, cmdWriteCharacter, btWriteCharacter, btRespondCharacter;//设备服务里的Characteristic

    //存放客户端的Messenger
    private final List<Messenger> mClients = new LinkedList<Messenger>();

    private static final Queue<BleWriteData> mWriteQueue = new ConcurrentLinkedQueue<BleWriteData>();//数据队列

    private static boolean isWriting = false;//正在写入数据

    private BleWriteData latSendData = new BleWriteData();
    private BleWriteData reSendData = new BleWriteData();//重新发送的数据

    private TimeOutThread timeOutThread; //超时线程
    private int timeOutNum = 0;//超时次数

    private boolean timeOutThread_Start = false;//超时线程是否开始

    private int timeOutTime = 0; //超时时间标记

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.fussenLog().d("1008611" + "=========服务第一次被创建===onCreate======");

        //注册蓝牙开关状态的广播
        registerReceiver(blueStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        //蓝牙初始化
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBlueToothAdapter = bluetoothManager.getAdapter();

        //创建service中的Messenger 用来和activity通信
        mHandler = new IncomingHandler(this);
        mMessenger = new Messenger(mHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.fussenLog().d("1008611" + "=========onStartCommand======");
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.fussenLog().d("1008611" + "=========onBind======");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.fussenLog().d("1008611" + "=========onUnbind======");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtil.fussenLog().d("1008611" + "=========onDestroy======");
        //注销蓝牙监听广播
        unregisterReceiver(blueStateBroadcastReceiver);
    }


    // service 端 Handler 的实现
    private static class IncomingHandler extends Handler {
        private final WeakReference<BleService> mService;

        //使用弱引用进行优化
        public IncomingHandler(BleService service) {
            mService = new WeakReference<>(service);
        }

        //用来处理activity发来的消息
        @Override
        public synchronized void handleMessage(Message msg) {

            //拿到service对象
            BleService service = mService.get();
            if (service != null) {
                switch (msg.what) {
                    case MSG_REGISTER:
                        service.mClients.add(msg.replyTo);//添加订阅者
                        LogUtil.fussenLog().d("1008611" + "======注册service=====");
                        break;

                    case MSG_UNREGISTER:
                        service.mClients.remove(msg.replyTo);//移除此消息
                        LogUtil.fussenLog().d("1008611" + "======注销service=====");
                        break;
                    case MSG_START_SCAN://收到activity的消息 开始扫描蓝牙
                        LogUtil.fussenLog().d("1008611" + "========开始扫描蓝牙设备=============");
                        service.startScanBleDevice();
                        break;
                    case CONNECT_BLE_DEVICE://收到activity的连接蓝牙设备指令 开始连接蓝牙

                        if (service.mBlueToothAdapter == null) {
                            BluetoothManager bluetoothManager = (BluetoothManager) service.getSystemService(Context.BLUETOOTH_SERVICE);
                            service.mBlueToothAdapter = bluetoothManager.getAdapter();
                        }
                        MyBleDevice myBleDevice = (MyBleDevice) msg.obj;//拿到activity传过来的设备信息

                        //通过mac地址连接设备
                        BluetoothDevice bluetoothDevice = service.mBlueToothAdapter.getRemoteDevice(myBleDevice.macAddress);

                        service.connectDevice(bluetoothDevice);
                        break;
                    case MSG_BLE_WRITE_NAME: //收到界面发来更改设备名称的指令
                        service.btWriteCmd((String) msg.obj);
                        break;

                    default:
                        super.handleMessage(msg);

                }
            }
        }
    }


    /**
     * 更改设备名称的指令
     *
     * @param values
     */
    private void btWriteCmd(String values) {

        //各个厂家的指令都不一样，只是举个例子 这里将values转化了下 然后写出去 具体转化格式 根据厂家来定

        byte[] cmdByte1 = "NAM".getBytes();
        byte[] cmdByte2 = values.getBytes();
        byte[] cmdByte = new byte[cmdByte1.length + cmdByte2.length + 1];

        System.arraycopy(cmdByte1, 0, cmdByte, 0, cmdByte1.length);
        cmdByte[cmdByte1.length] = (byte) cmdByte2.length;
        System.arraycopy(cmdByte2, 0, cmdByte, cmdByte1.length + 1, cmdByte2.length);

        if (btWriteCharacter != null && mBluetoothGatt != null) {

            //将指令放置进特征中
            btWriteCharacter.setValue(cmdByte);
            //设置回复形式WRITE_TYPE_NO_RESPONSE 这样速度会快
            btWriteCharacter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            //写出去
            mBluetoothGatt.writeCharacteristic(btWriteCharacter);
        }
    }

    /**
     * 连接设备
     *
     * @param device
     */
    private void connectDevice(BluetoothDevice device) {

        if (device == null) {
            return;
        }
        mBluetoothDevice = device;
        //开启异步连接任务
        new ConnectDeviceTask().execute();

    }

    //异步去连接设备
    private class ConnectDeviceTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                if (mBluetoothDevice != null) {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(BleService.this, false, mBluetoothCallback);
                }
                return "";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }


    /**
     * 蓝牙连接回调
     */
    private BluetoothGattCallback mBluetoothCallback = new BluetoothGattCallback() {


        //连接状态回调方法
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {//连接成功
                LogUtil.fussenLog().d("1008611" + "=======设备连接连接成功======");

                //去发现该设备服务
                mBluetoothGatt.discoverServices();
                isDeviceConnection = true;

            } else {//连接失败
                LogUtil.fussenLog().d("1008611" + "=======设备连接连接失败======");
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                if (isDeviceConnection) {
                    isDeviceConnection = false;
                }
            }
            sendDeviceConnectionState();//通知页面
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {//已发现该设备的服务

                //寻找服务之后，我们就可以和设备进行通信，比如下发配置值，获取设备电量什么的
                LogUtil.fussenLog().d("1008611" + "=======onServicesDiscovered======");

                //通过UUID拿到设备里的服务service
                mDeviceService = mBluetoothGatt.getService(UUIDUtils.CMD_SERVICE);
                //通过UUID拿到设备里的Characteristic
                cmdRespondCharacter = mDeviceService.getCharacteristic(UUIDUtils.CMD_READ_CHARACTERISTIC);
                cmdWriteCharacter = mDeviceService.getCharacteristic(UUIDUtils.CMD_WRITE_CHARACTERISTIC);
                btWriteCharacter = mDeviceService.getCharacteristic(UUIDUtils.CMD_BT_WRITE_CHARACTERISTIC);
                btRespondCharacter = mDeviceService.getCharacteristic(UUIDUtils.CMD_BT_READ_CHARACTERISTIC);

                //开启订阅事件后 设备可以主动的发送数据给你
                //通知后相应的Characteristic数据会在 onCharacteristicChanged()方法中返回
                //这里我订阅了cmdRespondCharacter，btRespondCharacter
                enableNotification(true, mBluetoothGatt, cmdRespondCharacter);
                enableNotification(true, mBluetoothGatt, btRespondCharacter);

                //写入数据
                nextWrite();

            } else {//未发现该设备的服务
                //这里我暂时没有做处理
            }
        }


        /**
         * 当你调用了readCharacteristic()方法后
         * 此方法会执行，在这里返回你想要的Characteristic数据
         * 也就是说 你是主动的获取Characteristic值，而不是等着设备给你返回
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
            }

        }

        /**
         * 如果该方法被调用, 此时characteristics的返回值由设备决定. APP应当将该值与
         * 要求的值相比较, 如果两者不相等, 则APP将会做相应的操作.也就是检查发送的命令是否成功
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

        /**被订阅的Characteristic的值要是改变 调用此方法
         * 执行此方法的前提就是要被订阅
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            //拿到characteristic的值
            byte[] data = characteristic.getValue();

            // 通过characteristic.getUUID()来判断是谁发送值给你 来执行相应的操作
            if (characteristic.getUuid().equals(UUIDUtils.CMD_BT_READ_CHARACTERISTIC)) { // BT
                handleBTDataAvailable(data);//拿到数据后解析 并发送给页面
            } else { // CMD命令的返回
                if (TLUtil.validateCRC8(data)) {//进行CRC8码验证 验证成功后 解析数据
                    LogUtil.fussenLog().e("1008611" + "========BLE CMD命令的返回=========");
                    handleCharacteristicData(data);
                } else {
                    //CRC8码验证失败 通知界面
                    Message msg = Message.obtain(null, MSG_CRC8_ERROR);
                    sendMessage(msg);
                    LogUtil.fussenLog().d("1008611" + " ==========CRC8_ERROR==========");
                }
            }
        }


        /**
         * 写入完Descriptor之后调用此方法 应在此检查数据是否发送完毕
         * @param gatt
         * @param descriptor
         * @param status
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            if (mWriteQueue.isEmpty()) {
                LogUtil.fussenLog().i("1008611" + "==========BLE onDescriptorWrite…写入… 完成");
                isWriting = false;

                //可以在此处通知页面或者做其他工作
            } else {
                LogUtil.fussenLog().i("1008611" + "==========BLEonDescriptorWrite…=========…写下一个");
                //如果队列中还有数据，继续写入
                isWriting = false;
                nextWrite();
            }
        }

        /**
         * 读取设备信号后的回调方法
         *
         * @param gatt
         * @param rssi
         * @param status
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

        }
    };

    /**
     * 解析通知返回数据
     *
     * @param data
     */
    private void handleCharacteristicData(byte[] data) {

        LogUtil.fussenLog().d(TAG + " handleCharacteristicData : " + BleUtils.logBytes(data));

        //收到指令 应该解析指令 这里暂时不做处理  具体得看 通信协议
        succeedReceiveCmd();

    }

    private void succeedReceiveCmd() {
        LogUtil.fussenLog().i("========BLE succeedReceiveCmd 指令返回============");
        isWriting = false;
        timeOutTime = 0;
        nextWrite();
    }


    /**
     * 解析蓝牙设备返回的数据
     *
     * @param data
     */
    private void handleBTDataAvailable(byte[] data) {

        //在这里我只 我们只看操作设备名称是否成功 不做其他操作
        if (data.length == 2) {
            if (data[0] == 79 && data[1] == 75) {   // 改设备名的返回(OK)
                Message msg = Message.obtain(null, MSG_BLE_WRITE_NAME_SUCCEED);
                msg.obj = data;
                sendMessage(msg);
            }
        } else {
            //更改失败 暂不做处理
        }
    }

    private void enableNotification(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == null || characteristic == null)
            return;

        //这一步必须要有 否则收不到通知
        gatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUIDUtils.CCC);
        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        //准备数据
        BleWriteData bData = new BleWriteData();
        bData.write_type = BleWriteData.DESCRIP_WRITE;//数据种类
        bData.object = clientConfig;
        //将数据加入队列
        mWriteQueue.add(bData);
    }


    /**
     * 写入数据，方法是同步的
     */
    private synchronized void nextWrite() {

        LogUtil.fussenLog().i("1008611" + " nextWrite ------ isEmpty()：" + mWriteQueue.isEmpty() + "    isWriting :" + isWriting);
        if (!mWriteQueue.isEmpty() && !isWriting) {
            doWrite(mWriteQueue.poll());//从数据队列里取出数据
        }
    }

    private void doWrite(BleWriteData data) {

        if (mBluetoothGatt == null) {
            return;
        }
        if (data.write_type == BleWriteData.CMD) { // cmd write
            if (cmdWriteCharacter != null) {
                cmdWriteCharacter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);//设置为WRITE_TYPE_NO_RESPONSE，这样速度会快
                cmdWriteCharacter.setValue(data.write_data);
                mBluetoothGatt.writeCharacteristic(cmdWriteCharacter);
                LogUtil.fussenLog().w("1008611" + " CMD 写数据…… ： " + " cmdID: " + data.write_data[1] + " " + data.write_data[2]);
                isWriting = true;
                latSendData = data;
                if (timeOutThread == null) {
                    //开启超时判断线程
                    initTimeOutThread();
                }
            }
        } else if (data.write_type == BleWriteData.DESCRIP_WRITE) { //  BluetoothGattDescriptor
            isWriting = true;
            mBluetoothGatt.writeDescriptor((BluetoothGattDescriptor) data.object);
        } else {
            nextWrite();
        }

    }


    /**
     * 开启超时判断线程
     */
    private void initTimeOutThread() {
        if (timeOutThread == null) {
            timeOutThread_Start = true;
            timeOutThread = new TimeOutThread();
            timeOutThread.start();
        }
    }


    /**
     * 指令超时判断的线程
     */
    class TimeOutThread extends Thread {
        @Override
        public void run() {
            while (timeOutThread_Start) {
                if (isWriting) {
                    if (timeOutTime == 6) {
                        // 超时三秒啦 重发
                        if (timeOutNum == 3) {
                            // 超时3次 放弃当前这条指令 发送下一条
                            isWriting = false;
                            mWriteQueue.clear();
                            timeOutNum = 0;
                            timeOutTime = 0;
                            //发送超时信息
                            sendTimeOutMessage();
                            nextWrite();
                            continue;
                        }
                        if (reSendData != null && latSendData != null && reSendData.write_type == latSendData.write_type && Arrays.toString(reSendData.write_data).equals(Arrays.toString(latSendData.write_data))) {
                            timeOutNum++;
                        } else {
                            timeOutNum = 1;
                        }
                        if (latSendData != null) {
                            doWrite(latSendData);
                            reSendData = latSendData;
                        }
                        timeOutTime = 0;
                    } else {
                        int num = 0;
                        while (isWriting) {
                            SystemClock.sleep(10);
                            num++;
                            if (num == 50) {
                                break;
                            }
                        }
                        timeOutTime++;
                    }
                } else {
                    int num = 0;
                    while (!isWriting) {
                        SystemClock.sleep(10);
                        num++;
                        if (num == 1000) {
                            break;
                        }
                    }
                    LogUtil.fussenLog().w("1008611" + "设备指令超时判断的线程 ----Runing--");
                }
            }
        }
    }


    /**
     * 发送超时信息
     */
    private void sendTimeOutMessage() {
        Message message = Message.obtain(null, MSG_SEND_DATA_TIME_OUT);
        sendMessage(message);
    }


    public class BleWriteData {
        public static final int CMD = 1;
        //        public static final int CMD_WRITE = 2;
        public static final int STM = 3;
        //        public static final int STM_WRITE = 4;
        public static final int DESCRIP_WRITE = 5;

        private int write_type; //对应的特性
        private byte[] write_data; // 设置的数据
        private Object object;
    }


    private void sendDeviceConnectionState() {
        Message msg = Message.obtain(null, MSG_DEVICE_CONNECT_STATE);
        msg.obj = isDeviceConnection;
        sendMessage(msg);
    }


    /**
     * 服务开始扫描所有蓝牙设备
     */
    private void startScanBleDevice() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBlueToothAdapter.stopLeScan(mLeScanCallback); // 5秒后结束扫描

                // 通知界面扫描结束 并传递数据
                if (mLeDevices != null && mLeDevices.size() > 0) {
                    Message msg = Message.obtain(null, SCAN_BLE_DEVICE_RESULT);
                    msg.arg1 = MSG_SCAN_SUCCESS;
                    msg.obj = mLeDevices;//将扫描到的蓝牙设备集合传递到activity去
                    sendMessage(msg);
                } else {
                    // 通知没有扫描到
                    Message msg = Message.obtain(null, SCAN_BLE_DEVICE_RESULT);
                    msg.arg1 = MSG_SCAN_FAIL;
                    sendMessage(msg);
                }
            }
        }, 5 * 1000);

        if (mLeDevices != null && mLeDevices.size() > 0) {
            mLeDevices.clear(); // 清空当前的数据。
        }
        if (mDeviceAddress != null && mDeviceAddress.size() > 0) {
            mDeviceAddress.clear();
        }
        mBlueToothAdapter.startLeScan(mLeScanCallback);
    }


    /**
     * 扫描蓝牙后的回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device != null && device.getName() != null) {
                if (mDeviceAddress.size() > 0 && mDeviceAddress.contains(device.getAddress())) {
                    mDeviceAddress.clear();
                }

                MyBleDevice bleDevice = new MyBleDevice();
                bleDevice.macAddress = device.getAddress();//设备的mac地址
                bleDevice.rssi = rssi;//设备的信号强度
                bleDevice.deviceName = device.getName();//设备的名称
                mLeDevices.add(bleDevice);
                mDeviceAddress.add(device.getAddress());

//                 过滤出自己的设备,目前展示所有扫描到的设备
//                ParcelUuid[] uuids = device.getUuids();
//                if (BleUtils.isFilterMyUUID(scanRecord)) {//过滤出自己的服务UUID
//
//                    // 获取广播里面的信息
//                    SparseArray<byte[]> recodeArray = BleUtils.parseFromBytes(scanRecord);
//
//                    //根据厂商ID获取对应的信息（0xffff 默认厂商ID  信息为SN码）
//                    byte[] b = recodeArray.get(0xffff);
//                    if (b != null && b.length > 0) {
//                        String deviceSn = BleUtils.byteToChar(b);
//                        MyBleDevice myBleDevice = new MyBleDevice();
//                        myBleDevice.macAddress = device.getAddress();//设备的mac地址
//                        myBleDevice.deviceSn = deviceSn;//设备sn码
//                        myBleDevice.rssi = rssi;//设备的信号强度
//                        myBleDevice.deviceName = device.getName();//设备的名称
//                        mLeDevices.add(myBleDevice);
//                        mDeviceAddress.add(device.getAddress());
//                    }
//
//                }
            }
        }
    };


    /**
     * 蓝牙开关的监听器
     */
    protected BroadcastReceiver blueStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //通过intent获得蓝牙的状态
            int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            switch (blueState) {
                case BluetoothAdapter.STATE_OFF://蓝牙关闭

                    //通知activity 蓝牙已关闭
                    Message msg = Message.obtain(null, MSG_BLUETOOTH_OFF);
                    msg.arg1 = MSG_BLUETOOTH_OFF;
                    sendMessage(msg);

                    //应该在此处释放蓝牙

                    break;
                case BluetoothAdapter.STATE_ON://蓝牙打开

                    //在这里要不要自动连接蓝牙  就看 你的了，这里暂时只是通知activity
                    //通知activity 蓝牙已关闭
                    Message msg1 = Message.obtain(null, MSG_BLUETOOTH_ON);
                    msg1.arg1 = MSG_BLUETOOTH_ON;
                    sendMessage(msg1);
                    break;
            }
        }
    };


    /**
     * 扫描蓝牙设备的结果
     *
     * @param list
     */
    protected void scanBleResult(int result, ArrayList<MyBleDevice> list) {
    }


    /**
     * 可以向所有绑定服务的activity发送消息
     *
     * @param msg 消息主体
     */
    private void sendMessage(Message msg) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            Message message = new Message();
            message.what = msg.what;
            message.arg1 = msg.arg1;
            message.arg2 = msg.arg2;
            message.obj = msg.obj;
            Messenger messenger = mClients.get(i);
            sendMessage(messenger, message);
        }
    }


    /**
     * 发送消息
     *
     * @param messenger
     * @param msg
     * @return
     */
    private boolean sendMessage(Messenger messenger, Message msg) {
        boolean success = true;
        try {
            messenger.send(msg);
        } catch (Exception e) {
            LogUtil.fussenLog().d(TAG + " Lost connection to client" + e);
            success = false;
        }
        return success;
    }
}
