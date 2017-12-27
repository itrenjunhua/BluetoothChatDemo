# Android 蓝牙聊天简单Demo

## 基本步骤和要求
> 基本步骤  
1. 客户端和服务端都打开蓝牙并是可被搜索到的状态  
2. 将客户端和服务端进行配对  
3. 配对成功之后就客户端可以通过 `BluetoothSocket` 对象的 `connect()` 方法进行连接，服务端通过   `BluetoothServerSocket` 对象的 `accept()`方法监听客服端的链接
4. 连接成功之后就可以通过 `BluetoothSocket` 对象拿到 `OutputStream`、`InputStream`输入输出流对象了  
5. 拿到输入输出流对象，就可以进行读写操作  
> 要求
1. 需要2台手机，一台作为客户端，另外一台作为服务端  
2. 2台手机都要装上应用，并打开蓝牙建立连接
# 使用说明
> 1. 需要进行蓝牙间的通讯，首先需要2台手机，一个服务端、一个客户端，然后在2台手机上都运行这个Demo；  
> 2. 先在一台手机上点击按钮进入服务端，然后打开蓝牙->启动服务器(也可以直接启动服务，如果没有打开蓝牙就会自动打开蓝牙)，这样就可以等待客户端的扫描和链接了；  
> 3. 在另一台手机点击进入客户端按钮，进入客户端，先打开蓝牙，然后点击搜索设备；  
> 4. 点击搜索设备之后系统会开始扫描周边蓝牙设备并使用列表的形式展示出来，现在就可以看到作为服务器的手机地址了；  
> 5. 点击地址条目，会进行自动配对和建立连接，连接建立成功就会跳转到聊天界面；  
> 6. 然后就可以使用2个手机进行通讯了。  

# 效果图展示
## Demo主页

<img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/main.png"  height="500" width="300"/>

## 服务端

<div><img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/server_1.png"  height="500" width="300"/> <img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/server_2.png"  height="500" width="300"/> <img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/server_3.png"  height="500" width="300"/></div>

## 客户端

<div><img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/client_main.png"  height="500" width="300"/> <img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/client_search_result.png"  height="500" width="300"/> <img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/client_chat_1.png"  height="500" width="300"/> <img src="https://github.com/itrenjunhua/BluetoothChatDemo/blob/master/images/client_chat_2.png"  height="500" width="300"/></div>
