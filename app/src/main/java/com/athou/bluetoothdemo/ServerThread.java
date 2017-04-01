package com.athou.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * 服务器线程，用于接受来自客户端的访问
 * Created by athou on 2017/4/1.
 */

public class ServerThread extends Thread implements IWrite {

    BluetoothServerSocket bluetoothServerSocket = null;
    BluetoothSocket bluetoothSocket = null;

    ConnectedThread connectedThread = null;

    BluetoothCallback mCallback = null;

    /**
     * 蓝牙通信，两者需要使用同一个UUID
     */
    public ServerThread(UUID uuid, BluetoothCallback callback) {
        try {
            mCallback = callback;
            bluetoothServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("athou_bluetooth", uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务端等待连接线程，从等级上讲，比客户端的连接线程高一个级别，应该是由于服务器端
     * 只需要等待一个连接请求，但并不考虑多线程，因为请求发出必有先后，而客户端却需要考
     * 虑线程，所以低了一个级别
     */
    @Override
    public void run() {
        while (bluetoothServerSocket != null) {
            try {
                Log.e("BluetoothDemo", "serverThread run:  等待连接");
                bluetoothSocket = bluetoothServerSocket.accept();

                Message message = new Message();
                message.what = MSG_WAIT_CONNECT;
                mHandler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                Log.d("BluetoothDemo", "serverThread run: connectsuccess");
                connectedThread = new ConnectedThread(bluetoothSocket, mCallback);
                connectedThread.start();

                Message message = new Message();
                message.what = MSG_CONNECT_SUCCESS;
                mHandler.sendMessage(message);
                break;
            }
        }
    }

    @Override
    public void write(String str) {
        if (connectedThread != null) {
            connectedThread.write(str);
        }
    }

    /**
     * Will cancel the listening socket, and cause the thread to finish
     */
    public void cancel() {
        try {
            if (bluetoothServerSocket != null) {
                bluetoothServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_WAIT_CONNECT) {
                if (mCallback != null) {
                    mCallback.onConnectState(BluetoothCallback.WAIT);
                }
            } else if (msg.what == MSG_CONNECT_SUCCESS) {
                if (mCallback != null) {
                    mCallback.onConnectState(BluetoothCallback.CONNECTED);
                }
            }
        }
    };

    private static final int MSG_WAIT_CONNECT = 0x101;
    private static final int MSG_CONNECT_SUCCESS = 0x102;
}
