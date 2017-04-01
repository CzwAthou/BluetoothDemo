package com.athou.bluetoothdemo;

import android.bluetooth.BluetoothDevice;

/**
 * Created by athou on 2017/4/1.
 */

public interface BluetoothCallback<T> {
    void onMessage(BluetoothDevice device, T t);

    void onConnectState(int status);

    static final int WAIT = 1;
    static final int CONNECTING = 2;
    static final int CONNECTED = 3;
}
