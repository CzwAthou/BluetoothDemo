package com.athou.bluetoothdemo;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * Created by athou on 2017/4/1.
 */

public class BluetoothManager {
    private UUID uuid;

    private ServerThread serverThread;
    private ClientThread clientThread;
    private IWrite writeThread = null;

    public BluetoothManager() {
        uuid = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d");
    }

    public void startServerThread() {
        serverThread = new ServerThread(uuid, new BluetoothCallback<String>() {
            @Override
            public void onMessage(BluetoothDevice device, String s) {
                if (mCallback != null) {
                    mCallback.onMessage(device, s);
                }
            }

            @Override
            public void onConnectState(int status) {
                if (status == BluetoothCallback.CONNECTED) {
                    writeThread = serverThread;
                }
                if (mCallback != null) {
                    mCallback.onConnectState(status);
                }
            }
        });
        serverThread.start();
    }

    public void startClientThread(BluetoothDevice device) {
        if (clientThread != null) {
            clientThread.cancel();
        }
        writeThread = clientThread = new ClientThread(device, uuid, new BluetoothCallback<String>() {
            @Override
            public void onMessage(BluetoothDevice device, String s) {
                if (mCallback != null) {
                    mCallback.onMessage(device, s);
                }
            }

            @Override
            public void onConnectState(int status) {
                if (mCallback != null) {
                    mCallback.onConnectState(status);
                }
            }
        });
        clientThread.start();
    }

    public boolean write(String msg) {
        if (writeThread != null) {
            writeThread.write(msg);
            return true;
        }
        return false;
    }

    BluetoothCallback mCallback;

    public void setCallback(BluetoothCallback mCallback) {
        this.mCallback = mCallback;
    }
}
