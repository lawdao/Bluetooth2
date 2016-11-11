package example.fussen.bluetooth2.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import example.fussen.bluetooth2.R;
import example.fussen.bluetooth2.adapter.BluetoothListAdapter;
import example.fussen.bluetooth2.base.BaseActivity;
import example.fussen.bluetooth2.bean.MyBleDevice;
import example.fussen.bluetooth2.service.BleService;
import example.fussen.bluetooth2.utils.BleUtils;
import example.fussen.bluetooth2.utils.LogUtil;
import example.fussen.bluetooth2.utils.ToastUtil;

public class MainActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener {


    private ProgressDialog progressDialog;
    private BluetoothListAdapter bluetoothListAdapter;

    private List<MyBleDevice> mData = new ArrayList<>();

    private boolean isDeviceConnection;

    @Override
    public int getContentView() {
        return R.layout.activity_main;
    }


    @Override
    public void initView() {
        Button btn_startService = (Button) findViewById(R.id.bt_start_service);
        Button open_bluetooth = (Button) findViewById(R.id.open_bluetooth);
        ListView listView = (ListView) findViewById(R.id.listview);

        Button changeName = (Button) findViewById(R.id.change);

        btn_startService.setOnClickListener(this);
        open_bluetooth.setOnClickListener(this);
        changeName.setOnClickListener(this);
        listView.setOnItemClickListener(this);

        startService(mServiceIntent);
        bindBleService();

        bluetoothListAdapter = new BluetoothListAdapter(mData, this);
        listView.setAdapter(bluetoothListAdapter);
        progressDialog = new ProgressDialog(this);

    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.open_bluetooth:
                //开启蓝牙
                openBluetooth();
                break;
            case R.id.bt_start_service:
                //搜索附近蓝牙设备
                startScan();
                break;
            case R.id.change:
                //更改设备名
                if (isDeviceConnection) {
                    changeDeviceName("这是设备名");
                } else {
                    ToastUtil.showToast("请先连接设备 好吗");
                }
                break;
        }
    }


    private void openBluetooth() {

        if (BleUtils.isOpenBle(this)) {
            ToastUtil.showToast("蓝牙已开启!");
            return;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter blueToothAdapter = bluetoothManager.getAdapter();
        if (!blueToothAdapter.isEnabled()) {
            blueToothAdapter.enable();
        }

    }


    /**
     * 向服务发送消息，开始扫描蓝牙
     */
    private void startScan() {
        if (!BleUtils.isOpenBle(this)) {
            ToastUtil.showToast("蓝牙未开启，请先开启蓝牙");
            return;
        }

        progressDialog.setMessage("搜索中...");
        progressDialog.show();
        Message msg = Message.obtain(null, BleService.MSG_START_SCAN);
        sendMessage(msg);
    }


    /**
     * 处理搜索结果
     *
     * @param result       结果码， 8代表 搜到蓝牙设备 9代表 没有搜到任何设备
     * @param myBleDevices 扫描到的结果集合
     */
    @Override
    protected void scanBleResult(int result, ArrayList<MyBleDevice> myBleDevices) {
        LogUtil.fussenLog().d("1008611" + "=======scanBleResult==========" + result);

        progressDialog.dismiss();

        switch (result) {
            case BleService.MSG_SCAN_SUCCESS://已搜到设备
                mData.clear();
                mData.addAll(myBleDevices);
                bluetoothListAdapter.notifyDataSetChanged();
                LogUtil.fussenLog().d("1008611" + "=======myBleDevices.size()==========" + myBleDevices.size());
                break;
            case BleService.MSG_SCAN_FAIL://未搜到设备
                ToastUtil.showToast("没有搜到设备,请重新搜索");
                break;
        }
    }


    /**
     * 设备连接状态回调
     *
     * @param state
     */
    @Override
    protected void deviceConnectionState(boolean state) {
        isDeviceConnection = state;
        if (state) {
            ToastUtil.showToast("连接成功");
        } else {
            ToastUtil.showToast("连接失败");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //连接被点击的设备，通知服务连接，将设备的信息也带过去
        MyBleDevice myBleDevice = mData.get(position);
        Message msg = Message.obtain(null, BleService.CONNECT_BLE_DEVICE);
        msg.obj = myBleDevice;
        sendMessage(msg);
    }
}
