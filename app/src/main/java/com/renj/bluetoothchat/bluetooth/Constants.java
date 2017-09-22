package com.renj.bluetoothchat.bluetooth;

import java.util.UUID;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * <p>
 * 创建时间：2017-09-21   11:06
 * <p>
 * 描述：保存一些常量
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public interface Constants {
    // 服务器端名称
    String NAME_SECURE = "BluetoothChatSecure";      // 安全连接
    String NAME_INSECURE = "BluetoothChatInsecure";  // 不安全连接

    // 服务器端UUID
    UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");   // 安全连接
    UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"); // 不安全连接
}
