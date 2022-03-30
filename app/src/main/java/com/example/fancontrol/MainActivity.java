package com.example.fancontrol;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
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
    ListView listDevice;
    BluetoothDevice controller;
    BluetoothManager btManager;
    boolean stateconnection = false;
    BluetoothAdapter btAdapter;
    String addressBTController = "00:00:00:00";

    Set<BluetoothDevice> paredDev;
    ArrayList pairedDeviceArrayList;
    ArrayAdapter< String> pairedDeviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bconnect = (Button) findViewById(R.id.buttonconnect);
        logview = (TextView) findViewById(R.id.loggingview);
        listDevice = (ListView) findViewById(R.id.listdevice);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "You device BLUETOOTH not support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        btAdapter = btManager.getAdapter();

        //System.out.println(btAdapter);
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String stInfo = btAdapter.getName() + " = " + btAdapter.getAddress() + "State bt module = " + btAdapter.getState();
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
        //if(pairedDeviceArrayList == null)
            fillList();
    }

    public void fillList(){
        paredDev = btAdapter.getBondedDevices();
        if (paredDev.size() > 0) { // Если есть сопряжённые устройства
            pairedDeviceArrayList = new ArrayList<>();
            for (BluetoothDevice device : paredDev) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }
            pairedDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            listDevice.setAdapter(pairedDeviceAdapter);
            listDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Клик по нужному устройству
                @Override
                public void onItemClick(AdapterView< ?> parent, View view, int position, long id) { //тут пробел после скобки !!!!
                    //listDevice.setVisibility(View.GONE); // После клика скрываем список
                    String  itemValue = (String) listDevice.getItemAtPosition(position);
                    String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес
                    BluetoothDevice device2 = btAdapter.getRemoteDevice(MAC);
                    String tmp = (String) logview.getText();
                    logview.setText(tmp + MAC);
                    //myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                    //myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            });
        }
        /*
        paredDev = btAdapter.getBondedDevices();
        ArrayList<String> list = new ArrayList<String>();
        for(BluetoothDevice bt : paredDev) list.add(bt.getName());
        Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        listDevice.setAdapter(adapter);
        */
    }

    class BTFanConnection extends Thread{
        BluetoothSocket btSocket = null;
        BluetoothDevice btDevice;

        public BTFanConnection(BluetoothDevice myDevice) {
            btDevice = myDevice;
        }

        @Override
        public void run() {
            super.run();


        }
    }
}