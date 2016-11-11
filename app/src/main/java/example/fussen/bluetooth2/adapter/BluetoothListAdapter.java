package example.fussen.bluetooth2.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import example.fussen.bluetooth2.R;
import example.fussen.bluetooth2.bean.MyBleDevice;

/**
 * Created by Fussen on 2016/11/8.
 */

public class BluetoothListAdapter extends BaseAdapter {
    private List<MyBleDevice> mData;

    private Context context;
    public BluetoothListAdapter(List<MyBleDevice> data, Context context) {
        this.mData = data;
        this.context=context;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public void setData(List<MyBleDevice> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        View v = View.inflate(context, R.layout.item, null);

        TextView name = (TextView) v.findViewById(R.id.name);
        TextView address = (TextView) v.findViewById(R.id.adress);
        name.setText(mData.get(i).deviceName);
        address.setText(mData.get(i).macAddress);
        return v;
    }
}
