package com.bluetooth.test;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 * Author:qingxia
 * Created:2017/8/17 18:03
 * Version:
 */
public class ScanAdapter extends BaseAdapter {
    private Context mContext;
    private List<ScanData> mlist_macs = new ArrayList<>();

    public ScanAdapter(Context context) {
            this.mContext = context;
    }

    @Override
    public int getCount() {
        return mlist_macs == null ? 0 : mlist_macs.size();
    }

    @Override
    public Object getItem(int i) {
        return mlist_macs == null ? null : mlist_macs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.simple_list_item_1, null);
            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) view.findViewById(R.id.txt_mac_imei);
            viewHolder.rssi = view.findViewById(R.id.txt_rssi);
            viewHolder.time = view.findViewById(R.id.txt_time);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        ScanData data = mlist_macs.get(i);
        viewHolder.textView.setText("name:"+data.getImei());
        viewHolder.time.setText("mac:"+data.getMac());
        viewHolder.rssi.setText("rssi:"+data.getSsid());
        return view;
    }

    public interface Listener{
        void postion(int pos);
    }


    public void addData(List<ScanData> mac) {
        if(mac == null){
            return;
        }
        mlist_macs.clear();
        mlist_macs.addAll(mac);
        notifyDataSetChanged();

    }

    class ViewHolder {
        TextView textView;
        TextView rssi;
        TextView time;
    }
}
