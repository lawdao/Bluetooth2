package example.fussen.bluetooth2.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.SparseArray;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙工具
 */
public class BleUtils {
    
    public static boolean isCMDReadResponse(byte[] data){
        if(data[0] == (byte) 0xB0){
            return true;
        }
        return false;
    }

    public static boolean isCMDReadError(byte[] data, byte payloadLength){
        if(data[3] == (byte) 0xFF || data[3] != payloadLength){
            return true;
        }
        return false;
    }

    public static boolean isCMDWriteResponse(byte[] data){
        if(data[0] == (byte) 0xA0){
            return true;
        }
        return false;
    }

    public static boolean isCMDWriteError(byte[] data){
        if(data[3] == (byte) 0xFF){
            return true;
        }
        return false;
    }
    
    public static boolean isSTMReadResponse(byte[] data){
        if(data[0] == (byte) 0xFC && data[1] == (byte)0xFD){
            return true;
        }
        return false;
    }
    
    public static boolean isSTMReadError(byte[] data, byte payloadLength){
        if(data[4] == (byte)0xFF || data[4] != payloadLength){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isSTMWriteResponse(byte[] data){
        if(data[0] == (byte) 0xFA && data[1] == (byte)0xFB){
            return true;
        }
        return false;
    }

    public static boolean isSTMWriteError(byte[] data){
        if(data[4] == (byte) 0xFF || data[5] != (byte)0){
            return true;
        }
        return false;
    }
    


    public static boolean isSTMWriteHeadDataResponse(byte[] data){
        if(data[0] == (byte) 0xFE && data[1] == (byte)0xFF){
            return true;
        }
        return false;
    }


    public static boolean isSTMWriteDataResponse(byte[] data){
        if(data[0] == (byte) 0xEE && data[1] == (byte)0xEF){
            return true;
        }
        return false;
    }
    
    //判断当前的数据是否属于commandId
    public static boolean isCurrentCommandData(byte[] data, short commandId){
        byte[] b = DataUtils.shortToByteArray(commandId);
        if(data[1] == b[1] && data[2] == b[0]){
            return true;
        }
        return false;
    }


    //STM 判断当前的数据是否属于commandId
    public static boolean isSTMCurrentCommandData(byte[] data, short commandId){
        byte[] b = DataUtils.shortToByteArray(commandId);
        if(data[2] == b[1] && data[3] == b[0]){
            return true;
        }
        return false;
    }
    
    
    

    public static boolean isCMDReadResponse(byte[] data, short commandId){
        byte[] b = DataUtils.shortToByteArray(commandId);
        if(data[0] == (byte) 0xB0 && data[1] == b[1] && data[2] == b[0]){
            return true;
        }
        return false;
    }

    public static boolean isCMDWriteResponse(byte[] data, short commandId){
        byte[] b = DataUtils.shortToByteArray(commandId);
        if(data[0] == (byte) 0xA0 && data[1] == b[1] && data[2] == b[0]){
            return true;
        }
        return false;
    }

    public static boolean isSTM32ReadResponse(byte[] data, short commandId){
        byte[] b = DataUtils.shortToByteArray(commandId);
        if(data[0] == (byte) 0xFC && data[1] == (byte)0xFD && data[2] == b[1] && data[3] == b[0]){
            return true;
        }
        return false;
    }

    public static boolean isSTM32WriteResponse(byte[] data, short commandId){
        byte[] b = DataUtils.shortToByteArray(commandId);
        if(data[0] == (byte) 0xFA && data[1] == (byte)0xFB && data[2] == b[1] && data[3] == b[0]){
            return true;
        }
        return false;
    }

    public static boolean isSTM32WriteHeadResponse(byte[] data, short index){
        byte[] b = DataUtils.shortToByteArray(index);
        if(data[0] == (byte) 0xFE && data[1] == (byte)0xFF && data[2] == b[1] && data[3] == b[0]){
            return true;
        }
        return false;
    }

    public static boolean isSTM32WriteDataResponse(byte[] data, short index){
        byte[] b = DataUtils.shortToByteArray(index);
        if(data[0] == (byte) 0xEE && data[1] == (byte)0xEF && data[2] == b[1] && data[3] == b[0]){
            return true;
        }
        return false;
    }
    
    public static boolean isDfuCMDError(byte[] data){
        if(data[4] == (byte)0xFF){
            return true;
        }else{
            return false;
        }
    }

    //根据SN号生成硬件连接需要的验证码
    public static byte[] generateSNPassWord(byte[] sn) {
        byte[] out = new byte[8];
        byte[] str = "BodyPlus".getBytes();
        
        for (int i = 0; i < 8; i++){
            out[i] = (byte) (sn[i + 2] ^ str[i] ^ sn[i%2]);
        }
        return out;
    }
    
    /**
     * 拆分文件
     *
     * @param fileName 待拆分的完整文件名
     * @param byteSize 按多少字节大小拆分
     * @throws IOException
     */
    public static List<byte[]> splitBySize(String fileName, int byteSize) throws IOException {
        List<byte[]> splitByteArray = new ArrayList<>();
        File file = new File(fileName);
        int count = (int) Math.ceil(file.length() / (double) byteSize);

        for (int i = 0; i < count; i++) {
            RandomAccessFile rFile;
            try {
                rFile = new RandomAccessFile(file, "r");
                rFile.seek(i * byteSize);// 移动指针到每“段”开头
                int length = byteSize;
                if(rFile.length() - (i* byteSize) < byteSize){
                    length = (int) (rFile.length() - (i* byteSize));
                }
                byte[] b = new byte[length];
                rFile.read(b);
                splitByteArray.add(b);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return splitByteArray;
    }
    

    //获取ble广播内容 SN码
    public static SparseArray<byte[]> parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        int currentPos = 0;
        SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos ++] & 0xFF;
                if (length == 0) {
                    break;
                }
                int dataLength = length - 1;
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case 0xFF:
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8) + (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2, dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        break;
                }
                currentPos += dataLength;
            }

            return  manufacturerData;
           
        } catch (Exception e) {
            return manufacturerData;
        }
       
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }

    //过滤出自己的服务UUID
    public static boolean isFilterMyUUID(byte[] scanRecord){
        UUID cmdService = UUID.fromString("00000001-0000-1000-8000-00805f9b34fb");
        UUID dataService = UUID.fromString("00000005-0000-1000-8000-00805f9b34fb");
        UUID stmService = UUID.fromString("00000009-0000-1000-8000-00805f9b34fb");
        UUID[] bleUUIDs = new UUID[]{cmdService, dataService, stmService};
        int index = 0;
        List<UUID> scanUUID = BleUtils.parseUuids(scanRecord);
        for(int i = 0; i < bleUUIDs.length; i ++){
            UUID uuid = bleUUIDs[i];
            for(int j = 0; j < scanUUID.size(); j ++){
                if(uuid.equals(scanUUID.get(j))){
                    index ++;
                    break;
                }
            }
        }
        if(index == 3){
            return true;
        }
        return false;
    }

    //获取ble广播内容 服务UUID
    public static List<UUID> parseUuids(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;
    }
    
    public static String generateVersion(String versionInfo){
        short w = Short.parseShort(versionInfo);

        byte h = (byte) ((w >> 8) & 0x00FF);
        byte l = (byte) (w & 0x00FF);

        byte a = (byte) ((h >> 4) & 0x0F);
        byte aa = (byte) (h & 0x0F);
        String aaa = String.valueOf(a).equals("0") ? String.valueOf(aa) : String.valueOf(a) + String.valueOf(aa);

        byte b = (byte) ((l >> 4) & 0x0F);
        byte bb = (byte) (l & 0x0F);
        String bbb = String.valueOf(b) + String.valueOf(bb);

        return aaa + "." + bbb;
    }


    public static String byteToChar(byte[] ucPtr){
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < ucPtr.length; ++i){
            char c = (char) ucPtr[i];
            sb.append(String.valueOf(c));
        }
        return sb.toString();
    }

    public static String logBytes(byte[] ucPtr){
        String s;
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < ucPtr.length; ++i){
            if(0 == i){
                s = "";
            }else{
                s = ", ";
            }
            s += String.valueOf(ucPtr[i]);
            sb.append(s);
        }
        
        return sb.toString();
    }

    public static String dumpBytes(byte[] ucPtr){
        String s;
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < ucPtr.length; ++i){
            if(0 == i){
                s = "";
            }else{
                s = ", ";
            }
            s += String.format("0x%02X", ucPtr[i]);//输出十六进制保留两位有效数字
            sb.append(s);
        }
        return sb.toString();
    }

    public static float getFloat(byte[] bytes)
    {
        return Float.intBitsToFloat(getInt(bytes));
    }

    public static int getInt(byte[] bytes)
    {
        return (0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)) | (0xff0000 & (bytes[2] << 16)) | (0xff000000 & (bytes[3] << 24));
    }

    public static byte[] float2byte(float f) {

        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        // 翻转数组
        int len = b.length;
        // 建立一个与源数组元素类型相同的数组
        byte[] dest = new byte[len];
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;

    }




    /**
     * 判断手机蓝牙手否打开
     * @param context
     * @return
     */
    public static boolean isOpenBle(Context context){
        BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()){
            return true;
        }
        return false;
    }
}
