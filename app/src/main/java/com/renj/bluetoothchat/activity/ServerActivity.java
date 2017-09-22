package com.renj.bluetoothchat.activity;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.renj.bluetoothchat.R;
import com.renj.bluetoothchat.bluetooth.BluetoothServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    private Button openServer,openBluetooth;

    private BluetoothServer bluetoothServer;
    private final int MSG_HAVE_CONTENT = 0XFF12;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_HAVE_CONTENT) {
                Toast.makeText(ServerActivity.this, msg.obj + "", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        openServer = (Button) findViewById(R.id.bt_open_server);
        openBluetooth = (Button) findViewById(R.id.bt_open_bluetooth);

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
                bluetoothServer.openBluetoothServer(true, new BluetoothServer.ServerAcceptListener() {
                    @Override
                    public void onAccept(BluetoothSocket bluetoothSocket) {
                        try {
                            InputStream inputStream = bluetoothSocket.getInputStream();
                            OutputStream outputStream = bluetoothSocket.getOutputStream();
                            while (true) {
                                byte[] bytes = new byte[1024];
                                inputStream.read(bytes);
                                Message message = Message.obtain();
                                message.what = MSG_HAVE_CONTENT;
                                String s = new String(bytes, 0, bytes.length);
                                message.obj = s;
                                handler.sendMessage(message);

                                outputStream.write(s.getBytes());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}
