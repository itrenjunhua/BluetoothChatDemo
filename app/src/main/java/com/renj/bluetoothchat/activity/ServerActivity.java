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
import com.renj.bluetoothchat.common.LogUtil;
import com.renj.bluetoothchat.server.BluetoothServer;

import java.io.IOException;
import java.io.InputStream;

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
    private Button openServer;

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

        bluetoothServer = BluetoothServer.newInstance();

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

        openServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothServer.openBluetoothServer(true, new BluetoothServer.ServerAcceptListener() {
                    @Override
                    public void onAccept(BluetoothSocket bluetoothSocket) {
                        try {
                            InputStream inputStream = bluetoothSocket.getInputStream();
                            byte[] bytes = new byte[1024 * 2];
                            inputStream.read(bytes);
                            LogUtil.i("接收到信息：" + new String(bytes, 0, bytes.length));
                            Message message = Message.obtain();
                            message.what = MSG_HAVE_CONTENT;
                            message.obj = new String(bytes, 0, bytes.length);
                            handler.sendMessage(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}
