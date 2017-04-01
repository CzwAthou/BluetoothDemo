package com.athou.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * 客户端，用于连接服务端
 * Created by athou on 2017/4/1.
 */

public class ClientThread extends Thread implements IWrite {

    BluetoothSocket bluetoothSocket = null;
    ConnectedThread connectedThread = null;
    BluetoothCallback mCallback = null;

    /**
     * 在创建客户端的时候，创建bluetoothSocket
     */
    public ClientThread(BluetoothDevice bluetoothDevice, UUID uuid, BluetoothCallback callback) {
        try {
            mCallback = callback;
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在线程内创建子线程，进行连接，我觉的这是由于多线程思想决定的
     * 在一个主线程中开辟子线程实现操作，从而可以实现并发，不会导致
     * 主线程的阻塞，导致IOException
     */
    @Override
    public void run() {
        if (bluetoothSocket == null) {
            return;
        } else {
            try {
                bluetoothSocket.connect();
                Log.d("BluetoothDemo", "ClientThread run: connectsuccess");
                /**
                 * 连接成功后，将连接完成的socket传入已连接线程
                 */
                connectedThread = new ConnectedThread(bluetoothSocket, mCallback);
                connectedThread.start();
            } catch (IOException e) {
                e.printStackTrace();

                cancel();
            }
        }
    }

    @Override
    public void write(String msg) {
        if (connectedThread != null) {
            connectedThread.write(msg);
        }
    }

    /**
     * Will cancel an in-progress connection, and close the socket
     */
    public void cancel() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
        }
    }
}