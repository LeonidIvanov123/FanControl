package com.example.fancontrol;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button bconnect;
    TextView logview;
    //ListView listView;
    BluetoothDevice controller;
    BluetoothManager btManager;
    boolean stateconnection = false;
    BluetoothAdapter btAdapter;
    String addressBTController = "00:00:00:00";

    List<String> paredDevicelist;
    ArrayAdapter<String> arrayListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bconnect = (Button) findViewById(R.id.buttonconnect);
        logview = (TextView) findViewById(R.id.loggingview);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "You device BLUETOOTH not support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        System.out.println(btAdapter);
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String stInfo = btAdapter.getName() + " = " + btAdapter.getAddress();
        logview.setText(String.format("Это устройство: %s", stInfo));
    }

    @Override
    protected void onStart() {
        super.onStart();
        logview.setText((String) logview.getText() + "\n Start app");
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
    }

    @SuppressLint("SetTextI18n")
    public void connectToController(View view) {
        String s = (String) logview.getText();
        logview.setText( s + "\n connect");
       // BluetoothDevice device2 = btAdapter.getRemoteDevice(addressBTController);
        //start thread connecting
    }
}