package com.example.drawingapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

// NetworkService类继承自Service，用于处理网络连接和数据传输
public class NetworkService extends Service {
    // 定义断开连接的命令字符串
//    public static final String DISCONNECT_COMMAND = "DISCONNECT";
    // 定义Binder实例，用于Activity与Service的绑定
    private final IBinder binder = new LocalBinder();
    // Socket用于网络连接
    private Socket socket;
    // 数据输出流，用于发送数据
    private DataOutputStream dos;
    // 数据输入流，用于接收数据
    private DataInputStream dis;
    // 控制监听循环的布尔值
    private boolean keepListening = true;



    // 定义回调接口，用于Service与Activity之间的通信
    public interface NetworkCallback {
        void onConnected(); // 连接成功时调用
        void onConnectionFailed(Exception e); // 连接失败时调用
        void onMessageReceived(String message); // 接收到消息时调用
    }

    // 回调接口的实例
    private NetworkCallback callback;

    // LocalBinder类，用于返回Service的实例
    public class LocalBinder extends Binder {
        NetworkService getService() {
            return NetworkService.this;
        }
    }

    // 当Service被绑定时调用，返回IBinder实例
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    // 发送消息的方法
    public void sendMessage(String message) {
        if (dos != null) {
            new Thread(() -> {
                try {
                    dos.writeUTF(message);
                    dos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }


    public void sendUserInfo(String name, String selectedText) {
        if (dos != null) {
            new Thread(() -> {
                try {
                    // 分别发送用户名和选中的文本内容
                    dos.writeUTF(name); // 首先发送name
                    dos.flush(); // 确保name发送完成
                    dos.writeUTF(selectedText); // 然后发送selectedText
                    dos.flush(); // 确保selectedText发送完成
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // 设置连接的方法
    public void setupConnection(String ip, String port, NetworkCallback callback) {
        this.callback = callback;
        new Thread(() -> {
            try {
                socket = new Socket(ip, Integer.parseInt(port));
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());

                keepListening = true;
                if (callback != null) {
                    callback.onConnected();
                }

                // 开始监听服务器消息
                startListening();
            } catch (Exception e) {
                if (callback != null) {
                    callback.onConnectionFailed(e);
                }
            }
        }).start();
    }


    // 关闭连接的方法
    public void closeConnection() {
        if (dos != null) {
            new Thread(() -> {
                try {
//                    dos.writeUTF(NetworkService.DISCONNECT_COMMAND);
//                    dos.flush();
                    socket.close(); // 确保关闭套接字
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // 检查连接状态的方法
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    // 开始监听服务器消息的方法
    private void startListening() {
        new Thread(() -> {
            try {
                while (keepListening) {
                    String message = dis.readUTF();
                    if (callback != null) {
                        callback.onMessageReceived(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 在Service销毁时确保关闭连接
    @Override
    public void onDestroy() {
        super.onDestroy();
        closeConnection();
    }

    public void setupConnectionCallback(NetworkCallback callback) {
        this.callback = callback;
    }



}
