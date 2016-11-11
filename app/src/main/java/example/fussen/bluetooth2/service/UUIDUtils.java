package example.fussen.bluetooth2.service;

import java.util.UUID;

public class UUIDUtils {
    //标准关闭或打开通知的UUID。
    public final static UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public final static String BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    public final static String BATTERY_CHAR_UUID = "00002a19-0000-1000-8000-00805f9b34fb";

    public final static String DEVICE_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public final static String DEVICE_CHAR_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    public final static String WRITE_SERVICE_UUID = "00001805-0000-1000-8000-00805f9b34fb";
    public final static String WRITE_CHAR_UUID_2A08 = "00002a08-0000-1000-8000-00805f9b34fb";
    public final static String WRITE_CHAR_UUID_2A09 = "00002a09-0000-1000-8000-00805f9b34fb";


    public final static String OAD_SERVICE_UUID = "f000ffc0-0451-4000-b000-000000000000";

    public final static String NAME_SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";
    public final static String NAME_CHAR_UUID = "00002a28-0000-1000-8000-00805f9b34fb";

    public final static String KEY_SERVICE_UUID = "0000c800-0000-1000-8000-00805f9b34fb";
    public final static String KEY_CHAR_UUID = "00002902-0000-1000-8000-00805f9b34fb";


    // 心率GATT服务
    public static final UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");

    // 心率GATT传感器位置特征
    public static final UUID HR_SENSOR_LOCATION_CHARACTERISTIC_UUID = UUID
            .fromString("00002A38-0000-1000-8000-00805f9b34fb");

    // 心率GATT特征
    public static final UUID HR_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_KEY_DATA =  UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");



    //电池服务
    public static final UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    // 电池特征
    public static final UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    //CMD profile
    public static final UUID CMD_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CMD_WRITE_CHARACTERISTIC = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CMD_READ_CHARACTERISTIC = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CMD_BT_WRITE_CHARACTERISTIC = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CMD_BT_READ_CHARACTERISTIC = UUID.fromString("6e400012-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CMD_LOG_READ_CHARACTERISTIC = UUID.fromString("6e400008-b5a3-f393-e0a9-e50e24dcca9e");

    //Data profile
    public static final UUID DATA_SERVICE = UUID.fromString("6e400005-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID DATA_WRITE_CHARACTERISTIC = UUID.fromString("6e400006-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID DATA_READ_CHARACTERISTIC = UUID.fromString("6e400007-b5a3-f393-e0a9-e50e24dcca9e");

    //STM profile
    public static final UUID STM_SERVICE = UUID.fromString("6e400009-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID STM_WRITE_CHARACTERISTIC = UUID.fromString("6e400010-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID STM_READ_CHARACTERISTIC = UUID.fromString("6e400011-b5a3-f393-e0a9-e50e24dcca9e");
}
