package com.bluetooth.test;

import android.bluetooth.BluetoothDevice;

/**
 * Created by shanshan on 2017/12/27.
 */

public class ScanData {


    private BluetoothDevice device = null;

    private String mac;

    private String imei;

    private String ssid;

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }
}
