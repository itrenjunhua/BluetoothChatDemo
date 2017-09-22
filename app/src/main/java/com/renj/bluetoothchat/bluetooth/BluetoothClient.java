package com.renj.bluetoothchat.bluetooth;

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
 * 描述：蓝牙客户端
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class BluetoothClient {
    private Context mContext;
    // 是否自动连接
    private boolean isAutoConn = false;
    // 连接类型 安全/受保护的连接(Secure)或者不安全的连接/不受保护的连接(Insecure)
    private boolean mSecure;
    // 进行配对和连接的设备
    private BluetoothDevice mDevice;
    // 连接工具类
    private BluetoothClientConnUtils mBluetoothClientConnUtils;
    // 蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    // 广播接收着，搜索设备和配对状态改变广播
    private BluetoothReceiver mBluetoothReceiver;
    // 找到设备监听，每找到一个设备回调一次方法
    private BluetoothFindDeviceListener mFindDeviceListener;
    // 搜索完成监听，搜索完成时回调
    private BluetoothSearchFinishedListener mFinishedListener;
    // 进行配对时，配对状态发生改变时回调相关方法
    private BluetoothBondChangeListener mBondChangeListener;
    // 连接监听，方法执行在子线程
    private ClientConnListener mClientConnListener;
    // 保存所有搜索到的设备
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    // 使用单例
    private static BluetoothClient mBluetoothClient;

    private final int MSG_WHAT_FOUND_DEVICE = 0XFF04; // 找到设备 what
    private final int MSG_WHAT_FINISHED_SEARCH = 0XFF05; // 搜索完成 what
    private final int MSG_WHAT_BOND_NONE = 0XFF06; // 没有配对 what
    private final int MSG_WHAT_BOND_BONDING = 0XFF07; // 正在配对 what
    private final int MSG_WHAT_BOND_BONDED = 0XFF08; // 配对成功 what

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_FOUND_DEVICE: // 找到设备
                    BluetoothDevice obj = (BluetoothDevice) msg.obj;
                    mDevices.add(obj);
                    if (mFindDeviceListener != null)
                        mFindDeviceListener.onFindDevice(obj);
                    break;
                case MSG_WHAT_FINISHED_SEARCH: // 搜索完成
                    if (mFinishedListener != null)
                        mFinishedListener.onFinishedSearch(mDevices);
                    break;
                case MSG_WHAT_BOND_NONE: // 没有配对
                    if (mBondChangeListener != null)
                        mBondChangeListener.onBond();
                    break;
                case MSG_WHAT_BOND_BONDING: // 正在配对
                    if (mBondChangeListener != null)
                        mBondChangeListener.onBonding();
                    break;
                case MSG_WHAT_BOND_BONDED: // 配对成功
                    if (mBondChangeListener != null)
                        mBondChangeListener.onBonded();
                    if (isAutoConn)
                        createConn(mSecure, mDevice);
                    break;
            }
        }
    };

    /**
     * 创建 BluetoothClient 对象，使用单例模式
     *
     * @param context
     * @return
     */
    public static BluetoothClient newInstance(Context context) {
        if (mBluetoothClient == null) {
            synchronized (BluetoothClient.class) {
                if (mBluetoothClient == null) {
                    mBluetoothClient = new BluetoothClient(context);
                }
            }
        }
        return mBluetoothClient;
    }

    /**
     * 构造函数
     *
     * @param context
     */
    private BluetoothClient(Context context) {
        this.mContext = context;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothClientConnUtils = BluetoothClientConnUtils.newInsteace();
        regiestReceiver();
    }

    /**
     * 设置找到设备监听，每找到一个设备会调用一次回调方法
     *
     * @param findDeviceListener
     * @return
     */
    public BluetoothClient setOnBluetoothFindDeviceListener(BluetoothFindDeviceListener findDeviceListener) {
        this.mFindDeviceListener = findDeviceListener;
        return mBluetoothClient;
    }

    /**
     * 设置搜索完成监听，搜索结束之后回调方法
     *
     * @param finishedListener
     * @return
     */
    public BluetoothClient setOnBluetoothSearchFinishedListener(BluetoothSearchFinishedListener finishedListener) {
        this.mFinishedListener = finishedListener;
        return mBluetoothClient;
    }

    /**
     * 设置配对状态改变监听
     *
     * @param bondChangeListener
     * @return
     */
    public BluetoothClient setOnBluetoothBondChangeListener(BluetoothBondChangeListener bondChangeListener) {
        this.mBondChangeListener = bondChangeListener;
        return mBluetoothClient;
    }

    /**
     * 设置连接监听
     *
     * @param clientConnListener
     * @return
     */
    public BluetoothClient setOnClientConnListener(ClientConnListener clientConnListener) {
        this.mClientConnListener = clientConnListener;
        return mBluetoothClient;
    }

    /**
     * 注册接受搜索到的蓝牙设备广播
     */
    private void regiestReceiver() {
        mBluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mBluetoothReceiver, intentFilter);
    }

    /**
     * 打开设备蓝牙
     *
     * @return
     */
    public BluetoothClient openBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        return mBluetoothClient;
    }

    /**
     * 开始搜索设备
     *
     * @return
     */
    public BluetoothClient startSearch() {
        // 如果没打开蓝牙就先打开蓝牙
        openBluetooth();

        // 如果没有注册广播，就先注册
        if (mBluetoothReceiver == null)
            regiestReceiver();

        // 清除原来保存的设备信息
        mDevices.clear();

        // 开始搜索
        mBluetoothAdapter.startDiscovery();
        return mBluetoothClient;
    }

    /**
     * 取消搜索
     *
     * @return
     */
    public BluetoothClient cancelDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
        return mBluetoothClient;
    }

    /**
     * 取消广播注册，包括发现蓝牙设备和配对状态信息发生改变的广播
     */
    public void unRegiestReceiver() {
        if (mBluetoothReceiver != null) {
            mContext.unregisterReceiver(mBluetoothReceiver);
            mBluetoothReceiver = null;
        }
    }

    /**
     * 配对蓝牙设备
     *
     * @param device BluetoothDevice对象
     * @return
     */
    public BluetoothClient bondDevice(BluetoothDevice device) {
        isAutoConn = false;
        int bondState = device.getBondState();
        if (BluetoothDevice.BOND_NONE == bondState) {
            device.createBond();
        } else if (BluetoothDevice.BOND_BONDING == bondState) {
            Log.i("BluetoothClient", "正在配对 ...");
        } else if (BluetoothDevice.BOND_BONDED == bondState) {
            // 已经配对
            Log.i("BluetoothClient", "已经配对 ...");
        }
        return mBluetoothClient;
    }

    /**
     * 配对并且连接到服务器
     *
     * @param secure 是否建立安全连接
     * @param device BluetoothDevice对象
     * @return
     */
    public BluetoothClient bondAndConn(boolean secure, BluetoothDevice device) {
        isAutoConn = true;
        this.mSecure = secure;
        this.mDevice = device;
        int bondState = device.getBondState();
        if (BluetoothDevice.BOND_NONE == bondState) {
            device.createBond();
        } else if (BluetoothDevice.BOND_BONDING == bondState) {
            Log.i("BluetoothClient", "正在配对 ...");
        } else if (BluetoothDevice.BOND_BONDED == bondState) {
            // 建立连接
            createConn(secure, device);
        }
        return mBluetoothClient;
    }

    /**
     * 建立连接
     *
     * @param secure          是否建立安全连接
     * @param bluetoothDevice
     * @return
     */
    public BluetoothClient createConn(boolean secure, BluetoothDevice bluetoothDevice) {
        mBluetoothClientConnUtils.createConnection(secure, bluetoothDevice, new BluetoothClientConnUtils.ClientConnListener() {
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
        return mBluetoothClient;
    }

    /**
     * 关闭客户端连接、注销广播并且停止搜索<br />
     * <b>关闭之后不能在进行数据传递，如果需要继续使用，需要重新搜索设备并建立连接</b>
     */
    public void closeClientConn() {
        mBluetoothClientConnUtils.closeConnection();
        unRegiestReceiver();
        cancelDiscovery();
    }

    /**
     * 找到设备监听
     */
    public interface BluetoothFindDeviceListener {
        /**
         * 每找到一个设备回调一次
         *
         * @param device 找到的设备
         */
        void onFindDevice(BluetoothDevice device);
    }

    /**
     * 搜索完成监听
     */
    public interface BluetoothSearchFinishedListener {
        /**
         * 搜索结束时回调
         *
         * @param devices 找到的所有设备
         */
        void onFinishedSearch(List<BluetoothDevice> devices);
    }

    /**
     * 配对状态发生改变监听
     */
    public interface BluetoothBondChangeListener {
        /**
         * 未配对
         */
        void onBond();

        /**
         * 正在配对
         */
        void onBonding();

        /**
         * 已经配对
         */
        void onBonded();
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
     * 搜索设备和配对状态改变广播
     */
    class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND == action) { // 找到设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Message message = Message.obtain();
                message.what = MSG_WHAT_FOUND_DEVICE;
                message.obj = device;
                mHandler.sendMessage(message);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) { // 搜索完成
                mHandler.sendEmptyMessage(MSG_WHAT_FINISHED_SEARCH);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) { // 配对状态改变
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                switch (state) {
                    case BluetoothDevice.BOND_NONE:
                        mHandler.sendEmptyMessage(MSG_WHAT_BOND_NONE);
                        Log.d("aaa", "BOND_NONE 删除配对");
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        mHandler.sendEmptyMessage(MSG_WHAT_BOND_BONDING);
                        Log.d("aaa", "BOND_BONDING 正在配对");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        mHandler.sendEmptyMessage(MSG_WHAT_BOND_BONDED);
                        Log.d("aaa", "BOND_BONDED 配对成功");
                        break;
                }
            }
        }
    }
}
