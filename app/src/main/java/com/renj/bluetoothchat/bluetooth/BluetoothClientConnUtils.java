package com.renj.bluetoothchat.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

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
public class BluetoothClientConnUtils {
    // 连接类型 安全/受保护的连接(Secure)或者不安全的连接/不受保护的连接(Insecure)
    private String mSocketType;
    // 蓝牙设备
    private BluetoothDevice mBluetoothDevice;
    // 蓝牙 Socket 对象
    private BluetoothSocket mBluetoothSocket;
    // 连接监听，方法执行在子线程
    private ClientConnListener mClientConnListener;
    // 连接的线程对象
    private ClientConnThread mClientConnThread;
    // 使用单例
    private static BluetoothClientConnUtils mBluetoothClientConnUtils = new BluetoothClientConnUtils();

    private BluetoothClientConnUtils() {
    }

    static BluetoothClientConnUtils newInsteace() {
        return mBluetoothClientConnUtils;
    }

    /**
     * 创建连接
     *
     * @param secure             是否建立安全连接
     * @param bluetoothDevice    需要建立连接的设备
     * @param clientConnListener 连接监听器对象
     */
    void createConnection(boolean secure, BluetoothDevice bluetoothDevice, ClientConnListener clientConnListener) {
        if (mClientConnThread == null) {
            this.mBluetoothDevice = bluetoothDevice;
            this.mClientConnListener = clientConnListener;
            mClientConnThread = new ClientConnThread(secure);
            mClientConnThread.start();
        }
    }

    /**
     * 关闭连接
     */
    void closeConnection() {
        if (mBluetoothSocket != null) {
            if (mBluetoothSocket.isConnected()) {
                try {
                    mBluetoothSocket.close();
                } catch (IOException e) {
                    LogUtil.e("client close BluetoothSocket failed.\n" + e);
                }
            }
            mClientConnThread.stop();
            mClientConnThread = null;
            mBluetoothSocket = null;
        }
    }

    /**
     * 建立连接监听，<b>方法运行在子线程</b>
     */
    interface ClientConnListener {
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
                    mBluetoothSocket =
                            mBluetoothDevice.createRfcommSocketToServiceRecord(Constants.MY_UUID_SECURE);
                } else {
                    mBluetoothSocket =
                            mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(Constants.MY_UUID_INSECURE);
                }
                if (!mBluetoothSocket.isConnected())
                    mBluetoothSocket.connect();
                if (mClientConnListener != null)
                    mClientConnListener.onSucceed(secure, mBluetoothSocket);

                LogUtil.i("Socket Type：" + mSocketType + " connection service succeed");
            } catch (IOException e) {
                if (mClientConnListener != null)
                    mClientConnListener.onFialed(e);

                LogUtil.e("Socket Type：" + mSocketType + " connection service failed\n" + e);
            }
        }
    }
}
