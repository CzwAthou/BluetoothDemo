package com.athou.bluetoothdemo;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 已连接的线程，用于通信
 * Created by athou on 2017/4/1.
 */

public class ConnectedThread extends Thread {
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    byte[] bytes = new byte[1024];

    private BluetoothCallback mCallback;

    /**
     * 获取输入输出流
     *
     * @param bluetoothSocket
     */
    public ConnectedThread(BluetoothSocket bluetoothSocket, BluetoothCallback callback) {
        this.bluetoothSocket = bluetoothSocket;
        this.mCallback = callback;
        try {
            outputStream = this.bluetoothSocket.getOutputStream();
            inputStream = this.bluetoothSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 读取线程，一直工作
     */
    @Override
    public void run() {
        int i = 0;
        if (inputStream == null)
            return;
        do {
            try {
                i = inputStream.read(bytes);

                Message message = new Message();
                message.what = MSG_READ_STRING;
                String string = new String(bytes, 0, i, "utf-8");
                message.obj = string;
                mHandler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } while (i != 0);
    }

    /**
     * 写不需要线程，值得一提的是读取和写入都是用bytes串的形式传输的
     *
     * @param string
     */
    public void write(String string) {
        try {
            byte[] bytes = string.getBytes("utf-8");
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_READ_STRING) {
                if (mCallback != null) {
                    mCallback.onMessage(bluetoothSocket.getRemoteDevice(), msg.obj);
                }
            }
        }
    };

    private static final int MSG_READ_STRING = 0x101;
}