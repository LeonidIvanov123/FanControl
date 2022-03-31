package com.example.fancontrol;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button bconnect;
    TextView logview, inputtext;
    ListView listDevice;
    BluetoothDevice controller;
    BluetoothManager btManager;
    boolean stateconnection = false;
    BluetoothAdapter btAdapter;
    String addressBTController = "00:00:00:00";
    Thread myThreadConnectBTdevice;
    Thread myThreadIOdata;
    private UUID myUUID;
    private StringBuilder sb = new StringBuilder();

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
        inputtext = (TextView) findViewById(R.id.textfromfancontrol);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "You device BLUETOOTH not support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
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
        logview.setText( s + "\n List of devices");
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
                    myThreadConnectBTdevice = new BTFanConnection(device2);
                    myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            });
        }
    }

    public void sendToStartFan(View view) {
        String tmp = (String) inputtext.getText();
        inputtext.setText(tmp + "\n test scroll");
    }

    class BTFanConnection extends Thread{
        private BluetoothSocket btSocket = null;
        BluetoothDevice remoteDevice;

        public BTFanConnection(BluetoothDevice device) {
            remoteDevice = device;
            try {
                btSocket = remoteDevice.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                //logview.setText(logview.getText() + "\n Не удалось создать сокет");
                e.printStackTrace();
            }
        }
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            super.run();
            boolean success = false;
            try {
                btSocket.connect();
                success = true;
            } catch (IOException e) {
                //String tmp = (String) logview.getText();
                //logview.setText(tmp + "\n Socket создан, не удалось подключиться");
                e.printStackTrace();
            }
            if (success){
                //String tmp = (String) logview.getText();
                //logview.setText(tmp + "\n Подключение успешно, создаем поток I/O");
                //Thread read
                myThreadIOdata = new ThreadIOdata(btSocket);
                myThreadIOdata.run();
            }
        }
    }

    private class ThreadIOdata extends Thread {

        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private String sbprint;

        @SuppressLint("SetTextI18n")
        public ThreadIOdata(BluetoothSocket btSocket) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = btSocket.getInputStream();
                out = btSocket.getOutputStream();
            } catch (IOException e) {
                //String tmp = (String) logview.getText();
                //logview.setText(tmp + "\n Подключение успешно, создаем поток I/O");
                e.printStackTrace();
            }
            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки
                    if (endOfLineIndex > 0) {
                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());
                        String tmp = (String) inputtext.getText();
                        inputtext.setText(tmp + "\n" + sbprint);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}