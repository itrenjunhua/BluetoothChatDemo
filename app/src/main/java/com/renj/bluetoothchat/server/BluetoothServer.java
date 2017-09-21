package com.renj.bluetoothchat.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.renj.bluetoothchat.common.Constants;
import com.renj.bluetoothchat.common.LogUtil;

import java.io.IOException;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * <p>
 * 创建时间：2017-09-21   10:57
 * <p>
 * 描述：蓝牙服务端控制类(服务器和客户端一对一)
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class BluetoothServer {
    // 连接类型 安全/受保护的连接(Secure)或者不安全的连接/不受保护的连接(Insecure)
    private String mSocketType;
    // 定义蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    // 蓝牙服务器Socket对象
    private BluetoothServerSocket mBluetoothServerSocket;
    // 客户端连接监听(当客户端成功连接时回调)
    private ServerAcceptListener mServerAcceptListener;
    // 服务器端线程对象
    private BluetoothServerThread mBluetoothServerThread;
    // 使用单例
    private static BluetoothServer mBluetoothServer = new BluetoothServer();

    private BluetoothServer() {
    }

    public static BluetoothServer newInstance() {
        return mBluetoothServer;
    }

    /**
     * 打开蓝牙服务器端
     *
     * @param secure               是否安全打开
     * @param serverAcceptListener 客户端连接监听器
     */
    public void openBluetoothServer(boolean secure, ServerAcceptListener serverAcceptListener) {
        if (mBluetoothServerThread == null) {
            this.mServerAcceptListener = serverAcceptListener;
            this.mBluetoothServerThread = new BluetoothServerThread(secure);
            this.mBluetoothServerThread.start();
        } else {
            LogUtil.d("BluetoothServer already open ...");
        }
    }

    /**
     * 关闭蓝牙服务器
     */
    public void closeBluetoothServer() {
        if (mBluetoothServerThread != null) {
            mBluetoothServerThread.interrupt();
            mBluetoothServerThread = null;
            mBluetoothAdapter = null;
        }
    }

    /**
     * 客户端连接监听器
     */
    public interface ServerAcceptListener {
        void onAccept(BluetoothSocket bluetoothSocket);
    }

    /**
     * 蓝牙端服务端线程类(一对一)
     */
    class BluetoothServerThread extends Thread {
        /**
         * 构造函数
         *
         * @param secure 连接类型<br />
         *               true：安全/受保护的连接(Secure)<br />
         *               false：不安全的连接/不受保护的连接(Insecure)
         */
        public BluetoothServerThread(boolean secure) {
            mSocketType = secure ? "Secure" : "Insecure";
            BluetoothServerSocket tmp = null;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                            Constants.NAME_SECURE, Constants.MY_UUID_SECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            Constants.NAME_INSECURE, Constants.MY_UUID_INSECURE);
                }
                LogUtil.i("Socket Type：" + mSocketType + " listen() succeed");
            } catch (IOException e) {
                LogUtil.e("Socket Type：" + mSocketType + " listen() failed\n" + e);
            }
            mBluetoothServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket bluetoothSocket = null;
            try {
                bluetoothSocket = mBluetoothServerSocket.accept();
                LogUtil.i("Socket Type：" + mSocketType + " accept() succeed");
            } catch (IOException e) {
                LogUtil.e("Socket Type: " + mSocketType + "accept() failed\n" + e);
            }

            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                if (mServerAcceptListener != null)
                    mServerAcceptListener.onAccept(bluetoothSocket);
            }
        }
    }
}
