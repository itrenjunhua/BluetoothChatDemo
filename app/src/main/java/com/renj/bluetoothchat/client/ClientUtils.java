package com.renj.bluetoothchat.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * <p>
 * 创建时间：2017-09-21   15:48
 * <p>
 * 描述：蓝牙客户端工具类
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class ClientUtils {
    private Context context;
    private boolean isAutoConn = false;
    private boolean secure;
    private BluetoothDevice device;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothReceiver bluetoothReceiver;
    private BluetoothSearchResultListener resultListener;
    private BluetoothSearchFinishedListener finishedListener;
    private BluetoothBondChangeListener bondChangeListener;
    // 连接监听，方法执行在子线程
    private ClientConnListener mClientConnListener;
    private List<BluetoothDevice> devices = new ArrayList<>();

    private final int MESSAGE_WHAT_FOUND_DEVICE = 0XFF04; // 找到设备 what
    private final int MESSAGE_WHAT_FINISHED_SEARCH = 0XFF05; // 搜索完成 what
    private final int MESSAGE_WHAT_BOND_NONE = 0XFF06; // 没有配对 what
    private final int MESSAGE_WHAT_BOND_BONDING = 0XFF07; // 正在配对 what
    private final int MESSAGE_WHAT_BOND_BONDED = 0XFF08; // 配对成功 what

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WHAT_FOUND_DEVICE: // 找到设备
                    BluetoothDevice obj = (BluetoothDevice) msg.obj;
                    devices.add(obj);
                    if (resultListener != null)
                        resultListener.findDevice(obj);
                    break;
                case MESSAGE_WHAT_FINISHED_SEARCH: // 搜索完成
                    if (finishedListener != null)
                        finishedListener.finishedSearch(devices);
                    break;
                case MESSAGE_WHAT_BOND_NONE: // 没有配对
                    if (bondChangeListener != null)
                        bondChangeListener.bond();
                    break;
                case MESSAGE_WHAT_BOND_BONDING: // 正在配对
                    if (bondChangeListener != null)
                        bondChangeListener.bonding();
                    break;
                case MESSAGE_WHAT_BOND_BONDED: // 配对成功
                    if (bondChangeListener != null)
                        bondChangeListener.bonded();
                    if (isAutoConn)
                        createConn(secure, device);
                    break;
            }
        }
    };

    /**
     * 构造函数
     *
     * @param context
     */
    public ClientUtils(Context context) {
        this.context = context;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        regiestReceiver();
    }

    /**
     * 设置找到设备监听
     *
     * @param resultListener
     * @return
     */
    public ClientUtils setOnBluetoothSearchResultListener(BluetoothSearchResultListener resultListener) {
        this.resultListener = resultListener;
        return this;
    }

    /**
     * 设置搜索完成监听
     *
     * @param finishedListener
     * @return
     */
    public ClientUtils setOnBluetoothSearchFinishedListener(BluetoothSearchFinishedListener finishedListener) {
        this.finishedListener = finishedListener;
        return this;
    }

    /**
     * 设置配对状态改变监听
     *
     * @param bondChangeListener
     * @return
     */
    public ClientUtils setOnBluetoothBondChangeListener(BluetoothBondChangeListener bondChangeListener) {
        this.bondChangeListener = bondChangeListener;
        return this;
    }

    /**
     * 设置连接监听
     *
     * @param clientConnListener
     * @return
     */
    public ClientUtils setOnBluetoothBondChangeListener(ClientConnListener clientConnListener) {
        this.mClientConnListener = clientConnListener;
        return this;
    }

    /**
     * 注册接受搜索到的蓝牙设备广播
     */
    private void regiestReceiver() {
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothReceiver, intentFilter);
    }

    /**
     * 打开设备蓝牙
     *
     * @return
     */
    public ClientUtils openBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        return this;
    }

    /**
     * 开始搜索设备
     *
     * @return
     */
    public ClientUtils startSearch() {
        // 如果没打开蓝牙就先打开蓝牙
        openBluetooth();

        // 如果没有注册广播，就先注册
        if (bluetoothReceiver == null)
            regiestReceiver();

        // 清除原来保存的设备信息
        devices.clear();

        // 开始搜索
        bluetoothAdapter.startDiscovery();
        return this;
    }

    /**
     * 取消搜索
     *
     * @return
     */
    public ClientUtils cancelDiscovery() {
        bluetoothAdapter.cancelDiscovery();
        return this;
    }

    /**
     * 取消广播注册，包括发现蓝牙设备和配对状态信息发生改变的广播
     */
    public void unRegiestReceiver() {
        if (bluetoothReceiver != null) {
            context.unregisterReceiver(bluetoothReceiver);
            bluetoothReceiver = null;
        }
    }

    /**
     * 配对蓝牙设备
     *
     * @param device BluetoothDevice对象
     */
    public void bondDevice(BluetoothDevice device) {
        isAutoConn = false;
        int bondState = device.getBondState();
        if (BluetoothDevice.BOND_NONE == bondState) {
            device.createBond();
        } else if (BluetoothDevice.BOND_BONDING == bondState) {
            Log.i("ClientUtils", "正在配对 ...");
        } else if (BluetoothDevice.BOND_BONDED == bondState) {
            // 建立连接
            Log.i("ClientUtils", "已经配对 ...");
        }
    }

    /**
     * 配对并且连接到服务器
     *
     * @param secure 是否建立安全连接
     * @param device BluetoothDevice对象
     */
    public void bondAndConn(boolean secure, BluetoothDevice device) {
        isAutoConn = true;
        this.secure = secure;
        this.device = device;
        int bondState = device.getBondState();
        if (BluetoothDevice.BOND_NONE == bondState) {
            device.createBond();
        } else if (BluetoothDevice.BOND_BONDING == bondState) {
            Log.i("ClientUtils", "正在配对 ...");
        } else if (BluetoothDevice.BOND_BONDED == bondState) {
            // 建立连接
            createConn(secure, device);
        }
    }

    /**
     * 建立连接
     *
     * @param secure          是否建立安全连接
     * @param bluetoothDevice
     */
    public void createConn(boolean secure, BluetoothDevice bluetoothDevice) {
        ClientConnUtils clientConnUtils = new ClientConnUtils();
        clientConnUtils.createConnection(secure, bluetoothDevice, new ClientConnUtils.ClientConnListener() {
            @Override
            public void onSucceed(boolean secure, BluetoothSocket bluetoothSocket) {
                if (mClientConnListener != null)
                    mClientConnListener.onSucceed(secure, bluetoothSocket);
            }

            @Override
            public void onFialed(Exception e) {
                if (mClientConnListener != null)
                    mClientConnListener.onFialed(e);
            }
        });
    }

    /**
     * 找到设备监听
     */
    public interface BluetoothSearchResultListener {
        void findDevice(BluetoothDevice device);
    }

    /**
     * 搜索完成监听
     */
    public interface BluetoothSearchFinishedListener {
        void finishedSearch(List<BluetoothDevice> devices);
    }

    /**
     * 配对状态发生改变监听
     */
    public interface BluetoothBondChangeListener {
        /**
         * 未配对
         */
        void bond();

        /**
         * 正在配对
         */
        void bonding();

        /**
         * 已经配对
         */
        void bonded();
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
     * 搜索设备广播
     */
    class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND == action) { // 找到设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Message message = Message.obtain();
                message.what = MESSAGE_WHAT_FOUND_DEVICE;
                message.obj = device;
                handler.sendMessage(message);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) { // 搜索完成
                handler.sendEmptyMessage(MESSAGE_WHAT_FINISHED_SEARCH);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) { // 配对状态改变
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                switch (state) {
                    case BluetoothDevice.BOND_NONE:
                        Log.d("aaa", "BOND_NONE 删除配对");
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Log.d("aaa", "BOND_BONDING 正在配对");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.d("aaa", "BOND_BONDED 配对成功");
                        break;
                }
            }
        }
    }
}
