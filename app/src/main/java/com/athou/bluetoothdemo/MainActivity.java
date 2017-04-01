package com.athou.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothReceiver bluetoothReceiver = null;

    RecyclerView listView = null;
    DevicesAdapter mDevicesAdapter = null;
    List<BluetoothDevice> deviceList;

    ToggleButton switchBtn;
    ToggleButton visableBtn;

    Button searchBtn;
    ProgressBar searchPb;

    EditText inputEt;
    Button sendBtn;
    TextView recTv;

    BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataBindingUtil.setContentView(this, R.layout.activity_main);

        switchBtn = (ToggleButton) findViewById(R.id.bluetooth_switch_btn);
        visableBtn = (ToggleButton) findViewById(R.id.bluetooth_visable_btn);
        searchBtn = (Button) findViewById(R.id.bluetooth_search_btn);
        searchPb = (ProgressBar) findViewById(R.id.bluetooth_search_pb);

        inputEt = (EditText) findViewById(R.id.bluetooth_input_et);
        sendBtn = (Button) findViewById(R.id.bluetooth_send_btn);
        recTv = (TextView) findViewById(R.id.bluetooth_recieve_msg_tv);

        listView = (RecyclerView) findViewById(R.id.bluetooth_devices_list);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "该设备不支持蓝牙!", Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothManager = new BluetoothManager();
        bluetoothManager.setCallback(new BluetoothCallback() {
            @Override
            public void onMessage(BluetoothDevice device, Object o) {
                Log.i("BluetoothDemo", "onMessage:" + o);
                recTv.append((device != null ? device.getName() : "对方") + ": " + o + "\n");
            }

            @Override
            public void onConnectState(int status) {
                Log.i("BluetoothDemo", "onConnectState:" + status);
            }
        });

        deviceList = new ArrayList<>();
        mDevicesAdapter = new DevicesAdapter();
        listView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(mDevicesAdapter);

        switchBtn.setChecked(mBluetoothAdapter.isEnabled());
        searchPb.setVisibility(mBluetoothAdapter.isDiscovering() ? View.VISIBLE : View.GONE);
        openBluetooth();

        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchBtn.isChecked()) { //打开蓝牙
                    //蓝牙是否已经启用
                    openBluetooth();
                } else { //关闭蓝牙
                    closeBluetooth();
                }
            }
        });
        visableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (visableBtn.isChecked()) { //打开设备可见性
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                } else { //关闭蓝牙可见性
                    closeBluetoothVisable();
                }
            }
        });
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceList.clear();
                mDevicesAdapter.notifyDataSetChanged();

                boolean ret = mBluetoothAdapter.startDiscovery();
                if (ret) {//成功启动
                    searchPb.setVisibility(View.VISIBLE);
                } else {
                    searchPb.setVisibility(View.GONE);
                }
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = inputEt.getText().toString();

                if (!TextUtils.isEmpty(msg)) {
                    if (bluetoothManager.write(msg)) {
                        recTv.append("我: " + msg + "\n");

                        inputEt.setText(null);
                    }
                }
            }
        });

        registerBluetoothReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
        }
    }

    private void registerBluetoothReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //蓝牙状态广播
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND); //发现其他设备的广播
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED); //可被查找模式发生改变的广播
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //发现设备完成的广播
        bluetoothReceiver = new BluetoothReceiver();
        registerReceiver(bluetoothReceiver, intentFilter);
    }

    private void openBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) { //蓝牙未开启，则发启动蓝牙请求
            //方法一，打开不会有提示
            mBluetoothAdapter.enable();

            //方法二，打开会有提示
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else { //蓝牙已经开启，则搜索可用设备
            getBondedDevices();
        }

        bluetoothManager.startServerThread();
    }

    private void closeBluetoothVisable() {
        //尝试关闭蓝牙可见性
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(mBluetoothAdapter, 1);
            setScanMode.invoke(mBluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getBondedDevices() {
        Set<BluetoothDevice> deviceSet = mBluetoothAdapter.getBondedDevices();
        deviceList.addAll(deviceSet);
        mDevicesAdapter.notifyDataSetChanged();
    }

    private void closeBluetooth() {
        mBluetoothAdapter.cancelDiscovery(); //取消发现

        mBluetoothAdapter.disable();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            getBondedDevices();
        } else if (requestCode == 300 && resultCode == RESULT_OK) {
            //开放可见成功的回调
        }
    }

    class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0);
                int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Log.i("BluetoothDemo", "oldState:" + oldState + "  newState:" + newState);
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int oldMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, 0);
                int newMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                Log.i("BluetoothDemo", "oldMode:" + oldMode + "  newMode:" + newMode);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) { //发现设备
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    deviceList.add(device);
                    mDevicesAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {//搜索完成
                searchPb.setVisibility(View.GONE);
            }
        }
    }

    class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.BluetoothViewHolder> {

        @Override
        public BluetoothViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
            return new BluetoothViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BluetoothViewHolder holder, final int position) {
            TextView tvName = (TextView) holder.itemView.findViewById(android.R.id.text1);
            TextView tvMac = (TextView) holder.itemView.findViewById(android.R.id.text2);

            tvName.setText(deviceList.get(position).getName());
            tvMac.setText(deviceList.get(position).getAddress());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothManager.startClientThread(deviceList.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            return deviceList.size();
        }

        class BluetoothViewHolder extends RecyclerView.ViewHolder {
            public BluetoothViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    public static final int REQUEST_ENABLE_BT = 0x01;
}
