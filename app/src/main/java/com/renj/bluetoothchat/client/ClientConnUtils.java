package com.renj.bluetoothchat.client;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.renj.bluetoothchat.common.Constants;
import com.renj.bluetoothchat.common.LogUtil;

import java.io.IOException;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * <p>
 * 创建时间：2017-09-21   15:02
 * <p>
 * 描述：客户端连接工具类
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class ClientConnUtils {
    // 连接类型 安全/受保护的连接(Secure)或者不安全的连接/不受保护的连接(Insecure)
    private String mSocketType;
    // 蓝牙设备
    private BluetoothDevice bluetoothDevice;
    // 蓝牙 Socket 对象
    private BluetoothSocket bluetoothSocket;
    // 连接监听，方法执行在子线程
    private ClientConnListener clientConnListener;
    // 连接的线程对象
    private ClientConnThread clientConnThread;

    /**
     * 创建连接
     *
     * @param secure             是否建立安全连接
     * @param bluetoothDevice    需要建立连接的设备
     * @param clientConnListener 连接监听器对象
     */
    public void createConnection(boolean secure, BluetoothDevice bluetoothDevice, ClientConnListener clientConnListener) {
        if (clientConnThread == null) {
            this.bluetoothDevice = bluetoothDevice;
            this.clientConnListener = clientConnListener;
            clientConnThread = new ClientConnThread(secure);
            clientConnThread.start();
        }
    }

    /**
     * 关闭连接
     */
    public void closeConnection() {
        if (bluetoothSocket != null) {
            if (bluetoothSocket.isConnected()) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    LogUtil.e("client close BluetoothSocket failed.\n" + e);
                }
            }
            clientConnThread.stop();
            clientConnThread = null;
            bluetoothSocket = null;
        }
    }

    /**
     * 建立连接监听，<b>方法运行在子线程</b>
     */
    public interface ClientConnListener {
        /**
         * 连接建立成功，<b>方法运行在子线程</b>
         *
         * @param secure          是否为安全的连接 true：是 false：不是
         * @param bluetoothSocket 建立连接成功之后的 BluetoothSocket 对象
         */
        void onSucceed(boolean secure, BluetoothSocket bluetoothSocket);

        /**
         * 连接建立失败，<b>方法运行在子线程</b>
         *
         * @param e 异常信息
         */
        void onFialed(Exception e);
    }

    /**
     * 客户端与服务端建立连接的线程类
     */
    class ClientConnThread extends Thread {
        private boolean secure;

        public ClientConnThread(boolean secure) {
            this.secure = secure;
            mSocketType = secure ? "Secure" : "Insecure";
        }

        @Override
        public void run() {
            try {
                if (secure) {
                    bluetoothSocket =
                            bluetoothDevice.createRfcommSocketToServiceRecord(Constants.MY_UUID_SECURE);
                } else {
                    bluetoothSocket =
                            bluetoothDevice.createInsecureRfcommSocketToServiceRecord(Constants.MY_UUID_INSECURE);
                }
                if (clientConnListener != null)
                    clientConnListener.onSucceed(secure, bluetoothSocket);

                LogUtil.i("Socket Type：" + mSocketType + " connection service succeed");
            } catch (IOException e) {
                if (clientConnListener != null)
                    clientConnListener.onFialed(e);

                LogUtil.e("Socket Type：" + mSocketType + " connection service failed\n" + e);
            }
        }
    }
}
