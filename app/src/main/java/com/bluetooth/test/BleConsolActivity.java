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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressLint("NewApi")
public class BleConsolActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback,AdapterView.OnItemClickListener{

    private UUID bltServerUUID = UUID.fromString("0000fe00-0000-1000-8000-00805f9b34fb");
    private UUID readDataUUID = UUID.fromString("0000fe02-0000-1000-8000-00805f9b34fb");
    private UUID writeDataUUID = UUID.fromString("0000fe01-0000-1000-8000-00805f9b34fb");
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

    private Button mStartScan,mLvL,mFind,mScanComm,mShutdown,mStartTime;

    private Button mStopScan;

    private TextView mLogData;

    private LinearLayout mControlLayout;
    private LinearLayout mControlLayout2;


    boolean mScanning = false;

    private List<ScanData> datas = new ArrayList<>();

    private final int REQUEST_ENABLE_BT = 0;

    private Map<String ,String> tempDatas = new HashMap<>();

    boolean isStop = false;

    BluetoothGatt mBluetoothGatt;

    private TextView mCount;

    Handler handler = new Handler(){
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
     * */
    private void initView(){
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

        mStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                byte[] temp = {0x74,0x77,0x01,0x01};
                handlerMsg(bytesToHexString(temp));
                if(mBluetoothGatt==null){
                    Toast.makeText(BleConsolActivity.this,"请重新进行连接!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(writeCharacteristic == null){
                    Toast.makeText(BleConsolActivity.this," write Characteristic获取失败!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
//                byte[] bTemp = hexStringToBytes("74770101");


                sendData(" 发送获取电量指令：",temp);
            }
        });

        mFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBluetoothGatt==null){
                    Toast.makeText(BleConsolActivity.this,"请重新进行连接!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(writeCharacteristic == null){
                    Toast.makeText(BleConsolActivity.this," write Characteristic获取失败!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
//                byte[] bTemp = hexStringToBytes("72770201");
                byte[] temp = {0x74,0x77,0x02,0x01};
                sendData(" 发送一键寻牛指令：",temp);
            }
        });
        mScanComm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBluetoothGatt==null){
                    Toast.makeText(BleConsolActivity.this,"请重新进行连接!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(writeCharacteristic == null){
                    Toast.makeText(BleConsolActivity.this," write Characteristic获取失败!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] temp = {0x74,0x77,0x03,0x01};
                sendData(" 发送立即扫描指令：",temp);
            }
        });

        mShutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBluetoothGatt==null){
                    Toast.makeText(BleConsolActivity.this,"请重新进行连接!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(writeCharacteristic == null){
                    Toast.makeText(BleConsolActivity.this," write Characteristic获取失败!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] temp = {0x74,0x77,0x04,0x01};
                sendData(" 发送关机指令：",temp);
            }
        });

        mStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBluetoothGatt==null){
                    Toast.makeText(BleConsolActivity.this,"请重新进行连接!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(writeCharacteristic == null){
                    Toast.makeText(BleConsolActivity.this," write Characteristic获取失败!!!",Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] temp = {0x74,77,05,01};
                sendData(" 发送开机机指令：",temp);
            }
        });
    }


    /**
     * 扫描蓝牙
     * */
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
        if(tempDatas.containsKey(imei)){
            return;
        }
        value++;
        mCount.setText("扫描到设备数量:"+value);
        mControlLayout.setVisibility(View.VISIBLE);
        mControlLayout2.setVisibility(View.VISIBLE);
        tempDatas.put(imei,imei);
        datas.add(sd);
        scanAdapter.addData(datas);
    }




    BluetoothDevice bluetoothDevice;

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

//      bluetoothDevice  =   mBluetoothAdapter.getRemoteDevice("00:E0:12:34:56:78");
        Log.e("TAG",i+"...！！！！");
        Handler handler1 = new Handler(BleConsolActivity.this.getMainLooper());
        sb.delete(0,sb.length());
        ScanData data = datas.get(i);
        handlerMsg(data.getImei()+"正在连接...");
        bluetoothDevice = data.getDevice();

        handler1.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt =  bluetoothDevice.connectGatt(BleConsolActivity.this,true,mGattCallback);
            }
        });
//        bluetoothGatt.getService(UUID.fromString("fe01"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBluetoothGatt!=null){
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }

    /**
     * 发送数据
     * */
    private void sendData(String tag,byte[] data ){
        if (data != null && writeCharacteristic!=null) {
            writeCharacteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            String hexString = bytesToHexString(data);
            handlerMsg(tag+hexString);
        }
    }

    StringBuffer sb = new StringBuffer();

    /**
     * 刷新view
     * */
    private void handlerMsg(String vMsg){
        sb.append(vMsg+"\n");
        Message msg = new Message();
        msg.obj =sb.toString();
        handler.sendMessage(msg);
    }


    /**
     * 蓝牙连接状态回调
    * */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.e("TAG", "onCharacteristicRead"+status);
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // 获取蓝牙ble设备返回给你的信息
            Log.e("TAG", "onCharacteristicChanged");

            byte[] values = characteristic.getValue();
            StringBuffer sb = new StringBuffer();
            for(byte b: values){
                sb.append(b+",");
            }
            handlerMsg("收到蓝牙返回byte数据:"+sb.toString());
            if(values.length>3){
                if(((int) values[0] & 0xff) == 0x74  && ((int) values[1] & 0xff) == 0x78){
                    if(((int) values[2] & 0xff) == 0x01){//电量返回
                        StringBuffer stringBuffer= new StringBuffer();
                        int v = values[3] & 0xFF;
                        String hv = Integer.toHexString(v);
                        if (hv.length() < 2) {
                            stringBuffer.append(0);
                        }else{
                            stringBuffer.append(hv);
                        }
                        handlerMsg("获取电量成功:"+stringBuffer.toString());
                    }else if(((int) values[2] & 0xff) == 0x02){//寻牛返回
                        if(((int) values[3] & 0xff) == 0x00){
                            handlerMsg("寻牛成功:");
                        }else{
                            handlerMsg("寻牛失败:");
                        }

                    }else if(((int) values[2] & 0xff) == 0x03){//立即扫描
                        if(((int) values[3] & 0xff) == 0x00){
                            handlerMsg("扫描成功:");
                        }else{
                            handlerMsg("扫描失败:");
                        }

                    }else if(((int) values[2] & 0xff) == 0x04){//GSM立即关机
                        if(((int) values[3] & 0xff) == 0x00){
                            handlerMsg("关机成功:");
                        }else{
                            handlerMsg("关机失败:");
                        }

                    }else if(((int) values[2] & 0xff) == 0x05){//GSM立即开机
                        if(((int) values[3] & 0xff) == 0x00){
                            handlerMsg("开机成功:");
                        }else{
                            handlerMsg("开机失败:");
                        }

                    }

                }
            }


        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.e("TAG","....");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                handlerMsg("连接状态：STATE_CONNECTED");
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handlerMsg("连接断开：STATE_DISCONNECTED");
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                mBluetoothGatt =  bluetoothDevice.connectGatt(BleConsolActivity.this,false,mGattCallback,BluetoothDevice.TRANSPORT_LE);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // 当发现服务的时候

            if (status == BluetoothGatt.GATT_SUCCESS) {
                  handlerMsg("服务状态：GATT_SUCCESS");
                BluetoothGattService service = gatt.getService(bltServerUUID);
                if(service!=null){
                    handlerMsg("设置service uuid :"+bltServerUUID.toString());
                    readCharacteristic = service.getCharacteristic(readDataUUID);
                    handlerMsg("设置读特征值 :"+readDataUUID.toString());
                    writeCharacteristic = service.getCharacteristic(writeDataUUID);
                    handlerMsg("设置写特征值 :"+writeDataUUID.toString());
                    writeNotifyCharacteristic = service.getCharacteristic(writeDataNotifyUUID);
                    gatt.setCharacteristicNotification(readCharacteristic, true);

                    boolean isEnablewrite =  gatt.setCharacteristicNotification(writeCharacteristic,true);
                    boolean isEnableNotification= gatt.setCharacteristicNotification(writeNotifyCharacteristic,true);

                    if(isEnableNotification) {
                        List<BluetoothGattDescriptor> descriptorList = readCharacteristic.getDescriptors();
                        if(descriptorList != null && descriptorList.size() > 0) {
                            for(BluetoothGattDescriptor descriptor : descriptorList) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                Log.e("TAG","......");
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }else{
                    handlerMsg("BluetoothGattService 获取失败 service uuid:"+bltServerUUID);
                }



            }else{
                handlerMsg("服务状态：onServicesDiscovered failer"+status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.e("TAG","write"+status);
        }
    };

    /**
     * 解析数据
     */
    public String spliteScanData(byte[] scanData) {

        int startPos = getStartPostion(scanData);
        StringBuffer sb1 = new StringBuffer();
        for(byte b:scanData){
            sb1.append(b);
        }

        if (startPos >= 2) {

            Log.e("---------TAG",sb1.toString());
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
            System.arraycopy(scanData, startPos+6, proximityImeiBytes, 0, 8);
            String hexString = bytesToHexString(proximityImeiBytes);
            StringBuilder sb = new StringBuilder();
            sb.append(hexString.substring(1, 4));
            sb.append(hexString.substring(4, 8));
            sb.append(hexString.substring(8, 12));
            sb.append(hexString.substring(12, 16));
            Log.e("---------TAG", sb.toString() + ".." + sb.length());
            if(smac.toString().equals("b4:a8:08:0f:e0:8b")){
                Log.e("",".....");
            }

            byte[] type = new byte[2];

            System.arraycopy(scanData, startPos + 14, type, 0, 2);
            String typeString = bytesToHexString(type);

            sb.append(" |牛群识别码:"+typeString);

            Log.e("---------TAG",typeString);
            byte[] num = new byte[4];

            System.arraycopy(scanData, startPos + 16, num, 0, 4);
            String numString = bytesToHexString(num);

            Log.e("---------TAG",numString);

            sb.append(" |终端号:"+numString);

            return sb.toString();
        }
        return null;
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



}