# Bluetooth2
蓝牙4.0进阶篇
##闲话中心
这几天最大的事可能就是美国总统的上任，双十一，还有乐视股价了，乍一看，好像和我们没什么关系，其实肯定是有的了，要不然他也成不了新闻啊，有一点我们得改变，就是我们必须要希望我们自己国家的企业能过强大，我们必须支持他们，哪怕他做的不够好，这个问题其实就像一个国家一样，我们都知道许多政策是不合理的，或者说有很多制度是坑人的，但是我们不能因为这些而不爱我们的国家，那么企业也是一样，就拿乐视来说，股价跌了，公司遇到资金问题了，你看看这些媒体都在报道什么，全是负面消息，马上倒闭了，或者说是撑不住了，没有一家媒体是支持乐视，都是在等着看笑话，恨不得把人家祖宗十八代都挖出来，凉一凉，这些都让我看到这些新闻都不想在点进去，还有就是锤子，你看看国外的品牌对国人的腐蚀度，国内的品牌居然远不及他，人家一个做手机的，好歹也是国产品牌，我们不想着让他走出国门，而是想着看他的笑话，媒体恨不得他明天早上就能倒闭，这倒还是一个头条新闻，其实，要是企业都走出去了，国家也就变得强大了，这个时候，你到别的国家旅游的时候人家就不会另眼相看了，人家在嘲笑你的时候，你是不是还挺高兴的，我们不能这么恬不知耻，我们要支持我们国家的每一个企业，即使他做的不好，我们也不能嘲笑每一个企业，因为他有可能就是你以后出去引以为豪的企业，是你自己国家的。
##效果预览
![](http://ww2.sinaimg.cn/large/65e4f1e6gw1f9ny8i8uf0g208k0f4kjm.gif)

这是最终效果，也是通过蓝牙进行数据传输，实时传输，可见蓝牙传输数据之强大
##步骤说明
1. 今天我们要完成的就是框架的搭建，做什么事首先得有个框架，这样会事半功倍的
2. 做蓝牙模块，一般情况下，你的蓝牙代码不可能全写在activity中吧，这时候我们需要他到后台去做事情，同时把采集到的数据返回到页面来
3. 这时候就是service和activity的通信了，首先得解决掉这个问题，才能进行下一步，也许这很简单，但你也要坚持看完
4. 有关于蓝牙传输数据的问题很多，最重要的也就是在他发送数据的时候，一定要有顺序，不能乱，千万不能乱，也就是不能使用多线程来处理数据，这样很危险，要么数据没有发送出去，要么发送的顺序乱了，影响实在太多，所以，保证了发送数据的顺序显得尤为重要
5. 还有发送数据时候的超时问题，这都是我们要考虑的，在这一块处理问题的方式可能和蓝牙协议有些关系，所以得自行处理
6. 这就是我说的所谓的框架，等这些事都做完了，你会发现，其实很简单

## service和activity之间的通信
**首先要说的就是我的处理方式，我是把service和activity之间的通讯当成两个应用来处理的，为什么要这样做呢？其实是为了有些时候我们会把蓝牙在后台的service设置成了一个单独的进程，设置成单独的进程的好处也是有的，可以单独的运作，系统会单独的给他开辟一块内存出来保证他的运行，因为这个时候service已经相当于一个单独的应用了，所以说，这个时候我们要想和service通信，那就是跨进程了，而不是线程了，这个时候大家都会想到aidl，他是专门来处理进程之间的通讯问题的，别逗了，好么，谁他么傻，就一个服务，我还得去写一个aidl，要是这样的话，我也就不会写这篇文章了，列位，看大招！**

## Messenger出场
### 一. 官方介绍：
它引用了一个Handler对象，以便others能够向它发送消息。该类允许跨进程间基于Message的通信(即两个进程间可以通过Message进行通信)，在服务端使用Handler创建一个 Messenger，客户端持有这个Messenger就可以与服务端通信了

### 二. 为什么要使用Messenger
和直接使用AIDL不同的是Messenger利用了Handler处理通信，所以它是线程安全的（不支持并发处理）；而我们平时用的AIDL是非线程安全的（支持并发处理）。所以大多数时候我们应用中是不需要处理夸进程并发处理通信的，而且最重要的是蓝牙喜欢并发的发送数据，而且还要很有顺序，所以这时选择Messenger会比AIDL更加容易操作。其实Messenger最终还是aidl的实现方式，是不是屌爆了。

### 三. Messenger使用步骤

① service 内部需要有一个 Handler 的实现，它被用来处理从每一个 client(activity) 发送过的来请求

② 通过这个 Handler ，来生成一个 Messenger

③ 在 service 的onBind() 方法中，需要向 client(activity) 返回由该 Messenger 生成的一个 IBinder 实例

④ client 使用从 service 返回的 IBinder 实例来初始化一个 Messenger， 然后使用该 Messenger 与 service 进行通信

⑤ service 通过它自身内部的 Handler 实现(Handler的handleMessage() 方法中)来处理从 client 发送过来的请求

### 四. 具体步骤及代码

1、创建activity中的Messenger	

```
//创建activity中的Messenger
mMessenger = new Messenger(new IncomingHandler(this));
```

2、实现client(activity)端的handler

		

```
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
	                    
	                    case BleService.MSG_BLUETOOTH_ON://检测到蓝牙已开启
	                        ToastUtil.showToast("检测到蓝牙已打开");
	                        break;
	                    default:
	                        super.handleMessage(msg);
	                }
	            }
	
	        }
	    }
```


3、client 端 ServiceConnection 的实现
**服务和activity绑定，必须实现ServiceConnection**

```
	//与服务的连接回调
    private ServiceConnection serviceConnection = new ServiceConnection() {

        // 当与service的连接建立后被调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

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
            mService = null;
        }
    };
```

4、client 端 向service发送消息

```
protected synchronized void sendMessage(Message msg) {
        if (msg != null && mService != null) {
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                unbindService(serviceConnection);
            }
        }
    }
```

5、service端创建Messenger和handler

① 创建Messenger

```
 //创建service中的Messenger 用来和activity通信
 mHandler = new IncomingHandler(this);
 mMessenger = new Messenger(mHandler);
```

② 创建handler

```
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
                switch (msg.what) {//根据发送过来消息的种类来处理消息
                    case MSG_REGISTER:
                        service.mClients.add(msg.replyTo);//添加订阅者
                        LogUtil.fussenLog().d("1008611" + "======注册service=====");
                        break;

                    case MSG_UNREGISTER:
                        service.mClients.remove(msg.replyTo);//移除此消息
                        LogUtil.fussenLog().d("1008611" + "======注销service=====");
                        break;
                    default:
                        super.handleMessage(msg);

                }
            }
        }
    }
```

③ 拿到IBinder对象

```
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
```

做到这，activity与service的通信框架就基本搭建完成了，这个时候不管service是一个单独的进程还是服务，那么都可以和activity就行无缝的通信了，记住不是并发而是有序的

##蓝牙流程细节处理(重点)
在 [Android蓝牙4.0之玩爆智能穿戴、家具(一)](http://blog.csdn.net/fussenyu/article/details/53054637) 一文中，我们已经讲述过了一遍蓝牙基本的开发流程，下面我们就来说下具体的流程和方法，还有处理方式，列位，不管你是高手还是未来的高手，请听一听我的方法，我们都需要共同进步，这篇文章要是说的有理，或者说对你有很大的启发，那就赶紧关注我的微信公共号，要不然这篇文章的价值体现何在？好吧，可以开始了

1、开启蓝牙，就不说了，现在说扫描蓝牙，首先是activity向service发送一个消息，告知service，让service在后台处理，扫描成功，向activity发送消息，将扫描到的数据一并带过去，扫描失败，告知activity自行处理，不管怎样给出一个扫描时间，不能一直扫描，我的处理方式，就是5秒后关闭扫描，这时候也有各个手机的兼容问题，所以时间不能太长，浪费资源(耗电)，并且会有不能预测的问题出现
① activity向service发消息，扫描蓝牙

```
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
```

② service收到消息，开始扫描工作


```
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
```

2、实现BluetoothAdapter.LeScanCallback，在上文中我们在这个回调里拿到了附近所有的设备信息，但是在实战中，我们谁都不会去这么做，其实呢，我们在扫描的阶段就应该过滤出自己的设备，不将其他设备展示出来，这样才是我们真正的需求呀，如何过滤呢，Demo源码中已经有很好的方案了，可下载源码查看，这里就不讲了，有可能，每个设备的过滤方法都不一样，但是一般情况下都是通过UUID来过滤的，我们采取的是使用UUID和广播双层过滤

3、如果说没有问题的话，你的设备应该已经显示在你的手机上了，下一步就是连接了，然后又和上面一样activity发送连接消息向service，然后service收到消息进行处理，代码不再贴出，现在就是service处理消息的问题了，那么service收到消息该怎么处理才好呢，此时应该开启异步连接任务，让他去连接，最好这样做，要不然很有可能会出现问题，目前各个手机蓝牙都已经很少了，偶尔会冒出个毛病来，但是不影响大局

```
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
```


4、实现BluetoothGattCallback，这个回调是最主要的啦，刚才是连接，那么我们暂时只关心BluetoothGattCallback回调中的onConnectionStateChange，那么在这里我们究竟要做些什么呢？其实这个方法执行了也就意味着蓝牙设备和手机连接已经成功了，我们应该先通知activity蓝牙设备连接成功，可是并不是这样的，一般情况下，我们暂时是不通知的，其实通知也是可以的，这不是说了么，为了安全么，所以处理的方式是一旦连接失败，立即通知，那么成功呢，别急，我们是和硬件打交道的，我们肯定会和硬件约定一个彼此认识的唯一标示吧，你不能说你拿着你的手机什么设备都连呀，那恐怕不行吧，这里我只是抛砖引玉，具体怎么做，还得看你们怎么约定的，Demo中是直接通知页面了

 

```
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
```

5、这个时候，我们要做的就是等这个方法返回成功之后，调用蓝牙发现服务的方法，先让他去寻找这个设备的服务，这个时候让我们把目光都投向BluetoothGattCallback回调中的onServicesDiscovered方法中来，能不能发现设备中的service的结果都会在这里出现，在这里我们要做的事情就是拿到自己设备中的服务，还有像有的数据是要进行订阅的，在这里就得订阅了，这个时候我们就得和设备进行通信了，比如下发配置值，获取设备电量什么的，都在这里执行

```
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
```

6、订阅事件：就是当设备需要主动的向你发送数据时，这时候，你应该先订阅，否则onCharacteristicChanged方法是不会执行的

```
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
```

7、写入数据：这个时候我们写的数据就是为了能获得设备的信息，或者发送一些指令，那要怎么写呢？这里就得注意了

① 写数据的时候必须是有序的，不能并发，这个时候我们就得用到队列了，一定不能乱，首先创建一个数据的队列，并且必须是先进先出

**Queue<BleWriteData> mWriteQueue = new ConcurrentLinkedQueue<BleWriteData>();//数据队列**

ConcurrentLinkedQueue介绍：

ConcurrentLinkedQueue是一个基于链接节点的无界线程安全队列，它采用先进先出的规则对节点进行排序，当我们添加一个元素的时候，它会添加到队列的尾部，当我们获取一个元素时，它会返回队列头部的元素。

② 在第6步的时候我们已经准备过数据了，这个时候就开始准备发送数据了，现在给出发送数据的代码

```
    /**
     * 写入数据，方法是同步的
     */
    private synchronized void nextWrite() {
        if (!mWriteQueue.isEmpty() && !isWriting) {
            doWrite(mWriteQueue.poll());//从数据队列里取出数据
        }
    }
```

③ 正式写入数据：这个时候要注意的就是为了保证在数据很多的情况下都能够发送成功，必须要有一个超时的判定，一旦超时，要么重新发送，要么直接丢掉数据，继续发送，具体情况得和硬件沟通，发送完之后，就得到onCharacteristicWrite方法中检查是否写入成功，但是我第一次调用的是mBluetoothGatt.writeDescriptor(),所以我要在onDescriptorWrite()方法中检查我发送的数据是否发送完毕

```
 private void doWrite(BleWriteData data) {

        if (mBluetoothGatt == null) {
            return;
        }
        if (data.write_type == BleWriteData.CMD) { // cmd write
            if (cmdWriteCharacter != null) {
                cmdWriteCharacter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);//设置为WRITE_TYPE_NO_RESPONSE，这样速度会快
                cmdWriteCharacter.setValue(data.write_data);
                mBluetoothGatt.writeCharacteristic(cmdWriteCharacter);
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
```

④ 检查数据是否发送完毕，我在这里 检查的是我定义的数据队列一旦数据队列中没有数据，那么我认为数据已经发送完毕，具体还得看硬件有没有其他要求，我是这样做的

```
		/**
         * 写入完Descriptor之后调用此方法 应在此检查数据是否发送完毕
         * @param gatt
         * @param descriptor
         * @param status
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            if (mWriteQueue.isEmpty()) {
                isWriting = false;
                
                //数据写入完毕 可以在此处通知页面或者做其他工作
            } else {
               
                //如果队列中还有数据，继续写入
                isWriting = false;
                nextWrite();
            }
        }
```

⑤ 超时判断：在这里采用的是轮询方式，但是逻辑的话各个设备都是不一样的，这里只是简单的说下

```
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
                }
            }
        }
    }
```

8、onCharacteristicChanged方法
我们的业务最主要的就是设备采取数据，然后发送数据到手机上来，所以，关注的数据一般都是在onCharacteristicChanged方法里，因为我们需要设备主动发送数据过来，然后验证和解析，处理的方式就是对于相对重要的数据要进行CRC8校验，一旦失败，就得通知页面，可能你所关心的和我所关心的不一样，但是道理都是相通的，下面贴出代码

```
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
                }
            }
        }
```

##最后说明
1. 源码中也有许多你应该可以借鉴的地方，但是不能太依赖于它，我们要明白的就是它的过程，和顺序，到底我们在什么地方干什么，剩下的就是数据的解析了，这一点也是和硬件相关的，所以说，你怎么验证你的数据，应该是提前约定好的，而不是你在那瞎验证，另外一个问题就是蓝牙的兼容性，可以说，只要你按照大众的流程来做，逻辑不能乱搞，兼容性这个问题就没有多大的问题，就算有问题，你只能说是避免，你是改不掉的，因为那是手机厂商的事
2. 有关于demo源码，demo不是让你运行的，就算你能运行，也看不到效果，只能暂时把周围的蓝牙设备搜到，然后展示出来，要看的就是里面的逻辑，还有处理方式，这些才是最终要的
3. 写到这了也算是写的差不多了，挺辛苦的
4. [博客](http://blog.csdn.net/fussenyu)
5. 关注我们，微信公共号：AppCode，扫描下面二维码即可关注

![](http://ww1.sinaimg.cn/large/65e4f1e6gw1f9btkbltksj2076076aaj.jpg)
