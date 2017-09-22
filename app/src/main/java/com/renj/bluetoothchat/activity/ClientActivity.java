package com.renj.bluetoothchat.activity;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.renj.bluetoothchat.R;
import com.renj.bluetoothchat.bluetooth.BluetoothClient;
import com.renj.bluetoothchat.common.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * <p>
 * 创建时间：2017-09-21   17:23
 * <p>
 * 描述：客户端页面
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class ClientActivity extends Activity {
    private Button btOpen, btSearch;
    private ListView listView;

    private BluetoothClient bluetoothClient;
    private MyAdapter myAdapter;
    private List<BluetoothDevice> devices = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        btOpen = (Button) findViewById(R.id.bt_open);
        btSearch = (Button) findViewById(R.id.bt_search);
        listView = (ListView) findViewById(R.id.listview);
        myAdapter = new MyAdapter();
        listView.setAdapter(myAdapter);

        bluetoothClient = BluetoothClient.newInstance(getApplicationContext());

        // 打开蓝牙
        btOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothClient.openBluetooth();
            }
        });

        // 搜索设备
        btSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devices.clear(); // 先清除原来的所有设备
                myAdapter.notifyDataSetChanged();
                bluetoothClient.startSearch();
            }
        });

        // 设置蓝牙操作的相关监听
        bluetoothClient
                .setOnBluetoothFindDeviceListener(new BluetoothClient.BluetoothFindDeviceListener() {
                    @Override
                    public void onFindDevice(BluetoothDevice device) {
                        devices.add(device);
                        myAdapter.notifyDataSetChanged();
                    }
                })
                .setOnBluetoothSearchFinishedListener(new BluetoothClient.BluetoothSearchFinishedListener() {
                    @Override
                    public void onFinishedSearch(List<BluetoothDevice> devices) {
                        LogUtil.i("一共找到设备：" + devices.size());
                        Toast.makeText(ClientActivity.this, "搜索完成，共找到" + devices.size() + "设备", Toast.LENGTH_SHORT).show();
                    }
                });

        // 点击条目直接进行配对和连接操作，连接成功之后进行跳转
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice itemAtPosition = (BluetoothDevice) parent.getItemAtPosition(position);
                Intent intent = new Intent(ClientActivity.this, ClientChatActivity.class);
                intent.putExtra("bluetoothdevice", itemAtPosition);
                startActivity(intent);
            }
        });
    }


    // 适配器
    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View inflate = LayoutInflater.from(ClientActivity.this).inflate(R.layout.item_devices, null);
            TextView textview = (TextView) inflate.findViewById(R.id.textview);
            BluetoothDevice bluetoothDevice = devices.get(position);
            int bondState = bluetoothDevice.getBondState();
            String isBoud = "--";
            if (BluetoothDevice.BOND_NONE == bondState) {
                isBoud = "未配对";
            } else if (BluetoothDevice.BOND_BONDING == bondState) {
                isBoud = "正在配对";
            } else if (BluetoothDevice.BOND_BONDED == bondState) {
                isBoud = "已配对";
            }
            textview.setText(isBoud + " - " + bluetoothDevice.getName() + " : " + bluetoothDevice.getAddress());
            return inflate;
        }
    }
}
