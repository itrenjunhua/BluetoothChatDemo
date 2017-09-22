package com.renj.bluetoothchat.activity;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.renj.bluetoothchat.R;
import com.renj.bluetoothchat.bluetooth.BluetoothServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * <p>
 * 创建时间：2017-09-21   17:15
 * <p>
 * 描述：服务端Activity
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class ServerActivity extends Activity {
    private Button openServer, openBluetooth, btSend;
    private EditText etSendContent;
    private ListView listview;

    private OutputStream outputStream;
    private BluetoothServer bluetoothServer;
    private List<String> chatContent = new ArrayList<>();
    private MyAdapter myAdapter = new MyAdapter();

    private final int MSG_UPDATE_UI = 0XFF12;
    private final int MSG_CLEAR_EDITTEXT = 0XFF13;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_UI) {
                myAdapter.notifyDataSetChanged();
                listview.setSelection(chatContent.size() - 1);
            }else if(msg.what == MSG_CLEAR_EDITTEXT){
                etSendContent.setText("");
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        openServer = (Button) findViewById(R.id.bt_open_server);
        openBluetooth = (Button) findViewById(R.id.bt_open_bluetooth);
        etSendContent = (EditText) findViewById(R.id.et_send_content);
        btSend = (Button) findViewById(R.id.bt_send);
        listview = (ListView) findViewById(R.id.listview);

        listview.setAdapter(myAdapter);

        bluetoothServer = BluetoothServer.newInstance(getApplicationContext());

        bluetoothServer.setOnServerStateListener(new BluetoothServer.ServerStateListener() {
            @Override
            public void onOpenSucceed(boolean secure) {
                Toast.makeText(ServerActivity.this, "启动服务器成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenFailed(Exception e) {
                Toast.makeText(ServerActivity.this, "启动服务器失败", Toast.LENGTH_SHORT).show();
            }
        });

        openBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothServer.openBluetooth();
            }
        });

        openServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothServer.openBluetoothServer(true, serverAcceptListener);
            }
        });

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendContent = etSendContent.getText().toString().trim();
                if (TextUtils.isEmpty(sendContent)) {
                    Toast.makeText(ServerActivity.this, "发送内容不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(sendContent);
                }
            }
        });
    }

    private BluetoothServer.ServerAcceptListener serverAcceptListener = new BluetoothServer.ServerAcceptListener() {
        @Override
        public void onAccept(BluetoothSocket bluetoothSocket) {
            try {
                InputStream inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                while (true) {
                    byte[] bytes = new byte[1024];
                    inputStream.read(bytes);
                    String s = new String(bytes, 0, bytes.length);
                    chatContent.add("B" + s);
                    handler.sendEmptyMessage(MSG_UPDATE_UI);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 发送信息
     *
     * @param message
     */
    private void sendMessage(String message) {
        if (outputStream != null)
            try {
                outputStream.write(message.getBytes());
                chatContent.add("A" + message);
                handler.sendEmptyMessage(MSG_UPDATE_UI);
                handler.sendEmptyMessage(MSG_CLEAR_EDITTEXT);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    // 适配器
    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return chatContent.size();
        }

        @Override
        public Object getItem(int position) {
            return chatContent.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(ServerActivity.this).inflate(R.layout.item_devices, null);
            String itemContent = chatContent.get(position);
            TextView textview = (TextView) convertView.findViewById(R.id.textview);
            if (itemContent.contains("A")) {
                textview.setGravity(Gravity.END);
                textview.setTextColor(Color.parseColor("#333333"));
            } else {
                textview.setGravity(Gravity.START);
                textview.setTextColor(Color.parseColor("#777777"));
            }
            textview.setText(itemContent.substring(1));
            return convertView;
        }
    }
}
