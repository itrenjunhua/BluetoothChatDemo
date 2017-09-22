package com.renj.bluetoothchat.activity;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
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
import com.renj.bluetoothchat.bluetooth.BluetoothClient;

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
 * 创建时间：2017-09-22   11:27
 * <p>
 * 描述：客户端聊天界面
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class ClientChatActivity extends Activity {
    private BluetoothClient bluetoothClient;
    private BluetoothSocket bluetoothSocket;
    private ReadWriteThread readWriteThread;
    private MyAdapter myAdapter;

    private final int MSG_WHAT_CLIENT_CONNECTION_SUCCEED = 0X01; // 客户端连接成功
    private final int MSG_WHAT_CLIENT_CONNECTION_FAILED = 0X02;  // 客户端连接失败
    private final int MSG_UPDATE_UI = 0X03; // 更新UI
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_CLIENT_CONNECTION_SUCCEED:
                    bluetoothSocket = (BluetoothSocket) msg.obj;
                    readWriteThread = new ReadWriteThread(bluetoothSocket);
                    readWriteThread.start();
                    Toast.makeText(ClientChatActivity.this, "连接到服务器成功", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WHAT_CLIENT_CONNECTION_FAILED:
                    Toast.makeText(ClientChatActivity.this, "连接到服务器失败", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_UPDATE_UI:
                    myAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
    private EditText etSendContent;
    private Button btSend;
    private ListView listview;
    private List<String> chatContent = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_chat);
        etSendContent = (EditText) findViewById(R.id.et_send_content);
        btSend = (Button) findViewById(R.id.bt_send);
        listview = (ListView) findViewById(R.id.listview);

        bluetoothClient = BluetoothClient.newInstance(getApplicationContext());

        Intent intent = getIntent();
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra("bluetoothdevice");
        bluetoothClient.bondAndConn(true, bluetoothDevice);

        myAdapter = new MyAdapter();
        listview.setAdapter(myAdapter);

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendContent = etSendContent.getText().toString().trim();
                if (TextUtils.isEmpty(sendContent)) {
                    Toast.makeText(ClientChatActivity.this, "发送内容不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    startSend(sendContent);
                }
            }
        });

        bluetoothClient
                .setOnBluetoothBondChangeListener(new BluetoothClient.BluetoothBondChangeListener() {
                    @Override
                    public void onBond() {
                        Toast.makeText(ClientChatActivity.this, "未配对", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onBonding() {
                        Toast.makeText(ClientChatActivity.this, "正在配对", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onBonded() {
                        Toast.makeText(ClientChatActivity.this, "配对成功", Toast.LENGTH_SHORT).show();
                    }
                })
                .setOnClientConnListener(new BluetoothClient.ClientConnListener() {
                    @Override
                    public void onSucceed(boolean secure, BluetoothSocket bluetoothSocket) {
                        Message msg = Message.obtain();
                        msg.what = MSG_WHAT_CLIENT_CONNECTION_SUCCEED;
                        msg.obj = bluetoothSocket;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onFialed(Exception e) {
                        Message msg = Message.obtain();
                        msg.what = MSG_WHAT_CLIENT_CONNECTION_FAILED;
                        msg.obj = e;
                        handler.sendMessage(msg);
                    }
                });
    }

    private void startSend(String sendContent) {
        if (readWriteThread != null) {
            readWriteThread.sendMessage(sendContent);
        } else {
            Toast.makeText(ClientChatActivity.this, "连接失败，不能发送消息", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 读取和发送的线程
     */
    class ReadWriteThread extends Thread {
        OutputStream outputStream;
        InputStream inputStream;

        public ReadWriteThread(BluetoothSocket bluetoothSocket) {
            try {
                this.outputStream = bluetoothSocket.getOutputStream();
                this.inputStream = bluetoothSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (inputStream != null) {
                while (true) {
                    try {
                        byte[] bytes = new byte[1024];
                        inputStream.read(bytes);
                        chatContent.add("B" + new String(bytes, 0, bytes.length));
                        handler.sendEmptyMessage(MSG_UPDATE_UI);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void sendMessage(String message) {
            if (outputStream != null) {
                try {
                    outputStream.write(message.getBytes());
                    chatContent.add("A" + message);
                    handler.sendEmptyMessage(MSG_UPDATE_UI);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
                convertView = LayoutInflater.from(ClientChatActivity.this).inflate(R.layout.item_devices, null);
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
