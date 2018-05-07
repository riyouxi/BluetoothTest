package com.bluetooth.test;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@SuppressLint("NewApi")
public class BleConsolActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback, AdapterView.OnItemClickListener {

    private UUID bltServerUUID = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");
    private UUID readDataUUID = UUID.fromString("000036f6-0000-1000-8000-00805f9b34fb");
    private UUID writeDataUUID = UUID.fromString("000036f5-0000-1000-8000-00805f9b34fb");
    private UUID writeDataNotifyUUID = UUID.fromString("0000feff-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;
    private BluetoothGattCharacteristic writeNotifyCharacteristic;

    //是否需要检测权限
    public static boolean isRequireCheck = true;

    private BluetoothAdapter mBluetoothAdapter;

    // 所需的全部权限
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private PermissionUtil mPermissionUtil;

    private ListView mListView;

    private ScanAdapter scanAdapter;

    private Button mStartScan, mLvL, mFind, mScanComm, mShutdown, mStartTime, mRing;

    private Button mStopScan;

    private TextView mLogData;

    private LinearLayout mControlLayout;
    private LinearLayout mControlLayout2;


    boolean mScanning = false;

    private List<ScanData> datas = new ArrayList<>();

    private final int REQUEST_ENABLE_BT = 0;

    private Map<String, String> tempDatas = new HashMap<>();

    boolean isStop = false;

    BluetoothGatt mBluetoothGatt;

    private TextView mCount;

    byte[] key = {0x20, 0x57, 0x2f, 0x52, 0x36, 0x4b, 0x3f, 0x47, 0x30, 0x50, 0x41, 0x58, 0x11, 0x63, 0x2d, 0x2b};

    byte[] token = new byte[4];

    byte[] gettoken = {0x06, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            String info = (String) msg.obj;
            mLogData.setText(info);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initView();
        mPermissionUtil = new PermissionUtil(this, this);
        if (isRequireCheck) {
            if (mPermissionUtil.lacksPermissions(PERMISSIONS)) {
                mPermissionUtil.requestPermissions(PERMISSIONS); // 请求权限
                isRequireCheck = true;
            } else {
                isRequireCheck = false;
            }
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your device does not support bluetooth 4.0", Toast.LENGTH_SHORT).show();
        }


//        byte[] temp = new byte[]{19,19,19,-1,0,0,125,126,-104,89,4,40,-88,-76,0,0,0,0,0,0,0,0,3,3,1,0,-96,3,25,0,0};
//       String s =  "FE01FE02FE03FE0DFE0EFE0FFEEF";
//       Log.e("TAG",s);
//       byte[] t = hexStringToBytes(s);
//       StringBuffer sb = new StringBuffer();
//       for(byte b:t){
//           sb.append(b+",");
//       }
//       Log.e("TAG",sb.toString());
    }

    /**
     * 初始化view
     */
    private void initView() {
        mLvL = findViewById(R.id.lvl);
        mFind = findViewById(R.id.find);
        mScanComm = findViewById(R.id.scan_commd);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mListView = findViewById(R.id.listview);
        scanAdapter = new ScanAdapter(this);

        mLogData = findViewById(R.id.log_data);
        mStartScan = findViewById(R.id.start_scan);
        mStopScan = findViewById(R.id.stop_scan);
        mControlLayout = findViewById(R.id.control_layout);
        mControlLayout2 = findViewById(R.id.control_layout2);
        mShutdown = findViewById(R.id.shutdown);
        mStartTime = findViewById(R.id.starttime);
        scanAdapter.addData(datas);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(scanAdapter);
        mCount = findViewById(R.id.cout);
        mRing = findViewById(R.id.scan_ring);
        mStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopGatt();
                isStop = false;
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    return;
                }
                value = 0;
                scan(true);
            }
        });

        mStopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStop = true;
                value = 0;
                scan(false);
            }
        });

        mLvL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                byte[] temp = {0x74,0x77,0x01,0x01};
                byte[] downLock = {0x02, 0x01, 0x01, 0x01, token[0], token[1], token[2], token[3], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                handlerMsg(bytesToHexString(downLock));
                if (mBluetoothGatt == null) {
                    Toast.makeText(BleConsolActivity.this, "请重新进行连接!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (writeCharacteristic == null) {
                    Toast.makeText(BleConsolActivity.this, " write Characteristic获取失败!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
//                byte[] bTemp = hexStringToBytes("74770101");
                handlerMsg("发送获取电量指令：");
                SendData(downLock);
//                sendData(" 发送获取电量指令：",temp);
            }
        });

        mFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothGatt == null) {
                    Toast.makeText(BleConsolActivity.this, "请重新进行连接!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (writeCharacteristic == null) {
                    Toast.makeText(BleConsolActivity.this, " write Characteristic获取失败!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] downLock = {0x05, 0x01, 0x06, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, token[0], token[1], token[2], token[3], 0x00, 0x00, 0x00};
//                byte[] bTemp = hexStringToBytes("72770201");
                handlerMsg("发送一键寻牛指令:");
                SendData(downLock);
            }
        });
        mScanComm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothGatt == null) {
                    Toast.makeText(BleConsolActivity.this, "请重新进行连接!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (writeCharacteristic == null) {
                    Toast.makeText(BleConsolActivity.this, " write Characteristic获取失败!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
//                byte[] temp = {0x74, 0x77, 0x03, 0x01};
//                sendData(" 发送立即扫描指令：", temp);
                byte[] bTemp = hexStringToBytes("FA0301WM");
                Log.e("TAG", "------bTemp:" + Arrays.toString(bTemp));
                byte[] downLock = {bTemp[0], bTemp[1], bTemp[2], bTemp[3], token[0], token[1], token[2], token[3], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                handlerMsg("设置工作模式指令:");
                SendData(downLock);
            }
        });

        mShutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothGatt == null) {
                    Toast.makeText(BleConsolActivity.this, "请重新进行连接!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (writeCharacteristic == null) {
                    Toast.makeText(BleConsolActivity.this, " write Characteristic获取失败!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] temp = {0x74, 0x77, 0x04, 0x01};
                sendData(" 发送关机指令：", temp);
            }
        });

        mStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothGatt == null) {
                    Toast.makeText(BleConsolActivity.this, "请重新进行连接!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (writeCharacteristic == null) {
                    Toast.makeText(BleConsolActivity.this, " write Characteristic获取失败!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] temp = {0x74, 77, 05, 01};
                sendData(" 发送开机机指令：", temp);
            }
        });

        mRing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothGatt == null) {
                    Toast.makeText(BleConsolActivity.this, "请重新进行连接!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (writeCharacteristic == null) {
                    Toast.makeText(BleConsolActivity.this, " write Characteristic获取失败!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] bTemp = hexStringToBytes("FA050101");
                Log.e("TAG", "------bTemp:" + Arrays.toString(bTemp));
                byte[] downLock = {bTemp[0], bTemp[1], bTemp[2], bTemp[3], token[0], token[1], token[2], token[3], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                handlerMsg("发送蜂鸣指令:");
                SendData(downLock);

//                byte[] bTemp1 = hexStringToBytes("FA070101");
//                Log.e("TAG", "------bTemp1:" + Arrays.toString(bTemp1));
//                byte[] downLock1 = {bTemp1[0], bTemp1[1], bTemp1[2], bTemp1[3], token[0], token[1], token[2], token[3], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
//                handlerMsg("发送闪光指令:");
//                SendData(downLock1);
            }
        });
    }

    public byte[] IntToByteArray2(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 扫描蓝牙
     */
    private void scan(final boolean enable) {
        if (enable) {
            mScanning = true;
            datas.clear();
            tempDatas.clear();
            scanAdapter.addData(datas);
            mBluetoothAdapter.startLeScan(this);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == mPermissionUtil.PERMISSION_REQUEST_CODE && mPermissionUtil.hasAllPermissionsGranted(grantResults)) {
            isRequireCheck = false;
        } else {
            isRequireCheck = true;
            mPermissionUtil.showMissingPermissionDialog();
        }
    }


    private int value;
    StringBuffer stringBuffer = new StringBuffer();

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
//        byte[] temp = new byte[]{19,19,19,-1,0,0,125,126,-104,89,4,40,-88,-76,0,0,0,0,0,0,0,0,3,3,1,0,-96,3,25,0,0};
            String imei = spliteScanData(scanRecord);
            if(TextUtils.isEmpty(imei)){
                return;
            }
            ScanData sd = new ScanData();
            sd.setDevice(bluetoothDevice);
            sd.setImei(imei);
            sd.setMac(bluetoothDevice.getAddress());
            sd.setSsid(String.valueOf(rssi));
            if (tempDatas.containsKey(imei)) {
                return;
            }
            value++;
            mCount.setText("扫描到设备数量:" + value);
            mControlLayout.setVisibility(View.VISIBLE);
            mControlLayout2.setVisibility(View.VISIBLE);
            tempDatas.put(imei, imei);
            datas.add(sd);
            scanAdapter.addData(datas);
        }


    BluetoothDevice bluetoothDevice;

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

//      bluetoothDevice  =   mBluetoothAdapter.getRemoteDevice("00:E0:12:34:56:78");
        Log.e("TAG", i + "...！！！！");
        stopGatt();
        Handler handler1 = new Handler(BleConsolActivity.this.getMainLooper());
        sb.delete(0, sb.length());
        ScanData data = datas.get(i);
        handlerMsg((data.getImei() == null || data.getImei().equals("") ? data.getDevice() : data.getImei()) + "正在连接...");
        bluetoothDevice = data.getDevice();

        handler1.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt = bluetoothDevice.connectGatt(BleConsolActivity.this, true, mGattCallback);
            }
        });

    }

    /**
     * 断开连接BluetoothGatt
     */
    public void stopGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGatt();
    }

    /**
     * 发送数据
     */
    private void sendData(String tag, byte[] data) {
        if (data != null && writeCharacteristic != null) {
            writeCharacteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            String hexString = bytesToHexString(data);
            handlerMsg(tag + hexString);
        }
    }

    StringBuffer sb = new StringBuffer();

    /**
     * 刷新view
     */
    private void handlerMsg(String vMsg) {
        sb.append(vMsg + "\n");
        Message msg = new Message();
        msg.obj = sb.toString();
        handler.sendMessage(msg);
    }

    public void SendData(byte[] data) {
        byte miwen[] = Encrypt(data, key);
        if (miwen != null) {
            writeCharacteristic.setValue(miwen);
            mBluetoothGatt.writeCharacteristic(writeCharacteristic);

            String hexString = bytesToHexString(miwen);
            handlerMsg(hexString);
            Log.e("TAG", "Encrypt" + hexString);
        }
    }


    // encryption
    public byte[] Encrypt(byte[] sSrc, byte[] sKey) {

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//"Algorithm/mode/complement mode"
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(sSrc);

            return encrypted;//BASE64 is used for transcoding, and it can be used to encrypt two times.
        } catch (Exception ex) {
            return null;
        }
    }

    // decryption
    public byte[] Decrypt(byte[] sSrc, byte[] sKey) {

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            ;
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] dncrypted = cipher.doFinal(sSrc);
            return dncrypted;

        } catch (Exception ex) {
            return null;
        }
    }


    /**
     * 蓝牙连接状态回调
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.e("TAG", "onCharacteristicRead" + characteristic.getUuid());
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // 获取蓝牙ble设备返回给你的信息
            Log.e("TAG", "onCharacteristicChanged");

            byte[] values = characteristic.getValue();
            StringBuffer sb = new StringBuffer();
            for (byte b : values) {
                sb.append(b + ",");
            }

            byte[] x = new byte[16];
            System.arraycopy(values, 0, x, 0, 16);
            byte mingwen[] = Decrypt(x, key);
            handlerMsg("收到蓝牙返回byte数据:" + sb.toString());

            if (mingwen != null && mingwen.length == 16) {
                if (mingwen[0] == 0x06 && mingwen[1] == 0x02) {
                    token[0] = mingwen[3];
                    token[1] = mingwen[4];
                    token[2] = mingwen[5];
                    token[3] = mingwen[6];
                    handlerMsg("收到token:");
                } else if (mingwen[0] == 0x02 && mingwen[1] == 0x02) {
                    StringBuffer stringBuffer = new StringBuffer();
                    int v = mingwen[3] & 0xFF;
                    String hv = Integer.toHexString(v);
                    if (hv.length() < 2) {
                        stringBuffer.append(0);
                    } else {
                        stringBuffer.append(hv);
                    }
                    handlerMsg("获取电量成功:" + stringBuffer.toString());
                } else if (mingwen[0] == 0x05 && mingwen[1] == 0x02) {
                    if (((int) mingwen[3] & 0xff) == 0x00) {
                        handlerMsg("寻牛成功:");
                    } else {
                        handlerMsg("寻牛失败:");
                    }
                } else if (mingwen[0] == 0xFA && mingwen[1] == 0x06) {
                    if (((int) mingwen[3] & 0xff) == 0x00) {
                        handlerMsg("蜂鸣成功:");
                    } else {
                        handlerMsg("蜂鸣失败:");
                    }
                } else if (mingwen[0] == 0xFA && mingwen[1] == 0x08) {
                    if (((int) mingwen[3] & 0xff) == 0x00) {
                        handlerMsg("闪光成功:");
                    } else {
                        handlerMsg("闪光失败:");
                    }
                }

            }
            if (mingwen.length > 3) {
//                if(((int) values[0] & 0xff) == 0x74  && ((int) values[1] & 0xff) == 0x78){
//                    if(((int) values[2] & 0xff) == 0x01){//电量返回
//                        StringBuffer stringBuffer= new StringBuffer();
//                        int v = values[3] & 0xFF;
//                        String hv = Integer.toHexString(v);
//                        if (hv.length() < 2) {
//                            stringBuffer.append(0);
//                        }else{
//                            stringBuffer.append(hv);
//                        }
//                        handlerMsg("获取电量成功:"+stringBuffer.toString());
//                    }else if(((int) values[2] & 0xff) == 0x02){//寻牛返回
//                        if(((int) values[3] & 0xff) == 0x00){
//                            handlerMsg("寻牛成功:");
//                        }else{
//                            handlerMsg("寻牛失败:");
//                        }
//
//                    }else if(((int) values[2] & 0xff) == 0x03){//立即扫描
//                        if(((int) values[3] & 0xff) == 0x00){
//                            handlerMsg("扫描成功:");
//                        }else{
//                            handlerMsg("扫描失败:");
//                        }
//
//                    }else if(((int) values[2] & 0xff) == 0x04){//GSM立即关机
//                        if(((int) values[3] & 0xff) == 0x00){
//                            handlerMsg("关机成功:");
//                        }else{
//                            handlerMsg("关机失败:");
//                        }
//
//                    }else if(((int) values[2] & 0xff) == 0x05){//GSM立即开机
//                        if(((int) values[3] & 0xff) == 0x00){
//                            handlerMsg("开机成功:");
//                        }else{
//                            handlerMsg("开机失败:");
//                        }
//
//                    }
//
//                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.e("TAG", "....");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                handlerMsg("连接状态：STATE_CONNECTED");
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handlerMsg("连接断开：STATE_DISCONNECTED");
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                mBluetoothGatt = bluetoothDevice.connectGatt(BleConsolActivity.this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // 当发现服务的时候

            if (status == BluetoothGatt.GATT_SUCCESS) {
                handlerMsg("服务状态：GATT_SUCCESS");
                List<BluetoothGattService> data = gatt.getServices();
                for (BluetoothGattService service : data) {
                    Log.e("uuid", service.getUuid().toString());
                }
                BluetoothGattService service = gatt.getService(bltServerUUID);
                if (service != null) {
                    handlerMsg("设置service uuid :" + bltServerUUID.toString());
                    readCharacteristic = service.getCharacteristic(readDataUUID);
                    handlerMsg("设置读特征值 :" + readDataUUID.toString());
                    writeCharacteristic = service.getCharacteristic(writeDataUUID);
                    handlerMsg("设置写特征值 :" + writeDataUUID.toString());
                    writeNotifyCharacteristic = service.getCharacteristic(writeDataNotifyUUID);
                    gatt.setCharacteristicNotification(readCharacteristic, true);

                    boolean isEnablewrite = gatt.setCharacteristicNotification(writeCharacteristic, true);
                    boolean isEnableNotification = gatt.setCharacteristicNotification(writeNotifyCharacteristic, true);

                    if (isEnableNotification) {
                        List<BluetoothGattDescriptor> descriptorList = readCharacteristic.getDescriptors();
                        if (descriptorList != null && descriptorList.size() > 0) {
                            for (BluetoothGattDescriptor descriptor : descriptorList) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                Log.e("TAG", "......");
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                } else {
                    handlerMsg("BluetoothGattService 获取失败 service uuid:" + bltServerUUID);
                }


            } else {
                handlerMsg("服务状态：onServicesDiscovered failer" + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.e("TAG", "write" + characteristic.getUuid());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            SendData(gettoken);
        }
    };

    /**
     * 解析数据
     */
    public String spliteScanData(byte[] scanData) {

        String hexMacx = bytesToHexString2(scanData);
        Log.e("---------TAG", hexMacx.toString());

        int startPos = getStartPostion(scanData);
        StringBuffer sb1 = new StringBuffer();
        for (byte b : scanData) {
            sb1.append(b);
        }


        if (startPos >= 2) {
            String hexMac1 = bytesToHexString2(scanData);
            Log.e("---------TAG", hexMac1.toString());


            byte[] nameBytes = new byte[6];
            System.arraycopy(scanData, startPos, nameBytes, 0, 6);
            String hexMac = bytesToHexString(nameBytes);
            StringBuilder smac = new StringBuilder();

            smac.append(hexMac.substring(0, 2).length() == 1 ? "0" + hexMac.substring(0, 2) : hexMac.substring(0, 2));
            smac.append(":");
            smac.append(hexMac.substring(2, 4).length() == 1 ? "0" + hexMac.substring(2, 4) : hexMac.substring(2, 4));
            smac.append(":");
            smac.append(hexMac.substring(5, 6).length() == 1 ? "0" + hexMac.substring(5, 6) : hexMac.substring(5, 6));
            smac.append(":");
            smac.append(hexMac.substring(6, 8).length() == 1 ? "0" + hexMac.substring(6, 8) : hexMac.substring(6, 8));
            smac.append(":");
            smac.append(hexMac.substring(8, 10).length() == 1 ? "0" + hexMac.substring(8, 10) : hexMac.substring(8, 10));
            smac.append(":");
            smac.append(hexMac.substring(10, 12).length() == 1 ? "0" + hexMac.substring(10, 12) : hexMac.substring(10, 12));
            Log.e("---------TAG", smac.toString());

            byte[] proximityImeiBytes = new byte[8];
            System.arraycopy(scanData, startPos + 6, proximityImeiBytes, 0, 8);
            String hexString = bytesToHexString(proximityImeiBytes);
            StringBuilder sb = new StringBuilder();

            sb.append(hexString.substring(1, 4));
            sb.append(hexString.substring(4, 8));
            sb.append(hexString.substring(8, 12));
            sb.append(hexString.substring(12, 16));
            Log.e("---------TAG", sb.toString() + ".." + sb.length());

            byte[] type = new byte[2];

            System.arraycopy(scanData, startPos + 14, type, 0, 2);
            String typeString = bytesToHexString(type);

            byte[] b1 = {type[0]};
            byte[] b2 = {type[1]};
            byte a = ByteBuffer.wrap(b1).order(ByteOrder.BIG_ENDIAN).get();
            byte b = ByteBuffer.wrap(b2).order(ByteOrder.BIG_ENDIAN).get();
            Log.e("---------TAG", typeString);
            sb.append(" |牛群识别码:" + bytesToInt(type));


            byte[] num = new byte[4];

            System.arraycopy(scanData, startPos + 16, num, 0, 4);
            String numString = bytesToHexString(num);

            Log.e("---------TAG", numString);
            int number = ByteBuffer.wrap(num).order(ByteOrder.BIG_ENDIAN).getInt();

            sb.append(" |终端号:" + number);

            sb.append("\n" + "收到的广播数据:" + hexMac1);

            return sb.toString();
        }
        return null;
    }

    public static int bytesToInt(byte[] bytes) {
        int addr = 0;
        addr = bytes[0] & 0xFF;
        addr |= ((bytes[1] << 8) & 0xFF00);
        return addr;
    }

    /**
     * 以小端模式将byte[]转成int
     */
    public static int bytesToIntLittle(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8));
        return value;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));

        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    /**
     * 获取数据开始的位置
     */
    private int getStartPostion(byte[] data) {
        int startByte = 0;
        while (startByte + 1 <= data.length) {
            if (((int) data[startByte] & 0xff) == 0x7D &&
                    ((int) data[startByte + 1] & 0xff) == 0x7E) {
                return startByte + 2;
            }
            startByte++;
        }
        return -1;
    }

    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private String bytesToHexString2(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv + ",");
        }
        return stringBuilder.toString();
    }


}
