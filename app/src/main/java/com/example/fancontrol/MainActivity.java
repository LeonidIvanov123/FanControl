package com.example.fancontrol;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button bconnect;
    TextView logview, inputtext;
    ListView listDevice;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    Thread myThreadIOdata;
    UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    StringBuilder sb = new StringBuilder();
    Handler inputMSGhandler;

    Set<BluetoothDevice> paredDev;
    ArrayList<String> pairedDeviceArrayList;
    ArrayAdapter<String> pairedDeviceAdapter;

    @SuppressLint("HandlerLeak")
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
        //myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String stInfo = btAdapter.getName() + " = " + btAdapter.getAddress() + "State bt module = " + btAdapter.getState();
        logview.setText(String.format("Это устройство:\n %s", stInfo));

        inputMSGhandler = new Handler(){
            public void handleMessage(android.os.Message msg) {
                inputtext.setText(inputtext.getText() + "\n" + msg.obj.toString());
            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        //logview.setText("Start app");
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
    }

    public void connectToController(View view) throws IOException {
        String s = (String) logview.getText();
        logview.setText( s + "\n Present list of available devices");
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
                    logview.setText(tmp + "\n Connect to: " + MAC);

                    btAdapter.cancelDiscovery();
                    BTFanConnection myThreadConnectBTdevice = new BTFanConnection(device2);
                    myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            });
        }
    }

    public void sendToStartFan(View view) throws IOException {
        String tmp = (String) inputtext.getText();
        inputtext.setText(tmp + "\n test scroll 1233");

        //btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //ConnectToController connection = new ConnectToController(btManager);
        //connection.start();

    }

    class BTFanConnection extends Thread{
        BluetoothSocket btSocket = null;
        BluetoothDevice btDevice = null;

        public BTFanConnection(BluetoothDevice device) {
            btDevice = device;
        }
        @Override
        public void run() {
            super.run();
            boolean success = false;
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                try {
                    btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //logview.setText(logview.getText() + "\nСокет создан...");
            try{
                btSocket.connect();
                success = true;
                //logview.setText(logview.getText() + "\nСоединение установлено");
            } catch (IOException e) {
                Log.e("===========",e.getMessage());
                try {
                    Log.e("","trying fallback...");

                    try {
                        btSocket =(BluetoothSocket) btDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(btDevice,1);
                    } catch (NoSuchMethodException ex) {
                        ex.printStackTrace();
                    } catch (InvocationTargetException ex) {
                        ex.printStackTrace();
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                    btSocket.connect();
                    success = true;
                    Log.e("===========","Connected");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    try {
                        btSocket.close();
                    } catch (IOException ex1) {
                        ex1.printStackTrace();
                    }
                }
            }
            if (success){
                //logview.setText(logview.getText() + "\nПодключение успешно, создаем поток I/O");
                myThreadIOdata = new ThreadIOdata(btSocket);
                myThreadIOdata.start();
            }
        }
    }

    private class ThreadIOdata extends Thread {

        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private String sbprint;

        public ThreadIOdata(BluetoothSocket btSocket) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = btSocket.getInputStream();
                out = btSocket.getOutputStream();
                //logview.setText(logview.getText() + "\nОткрыты потоки I/O");
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    btSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            super.run();
            //inputtext.setText("Читаем в потоке I/O ");
            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки
                    if (endOfLineIndex > 0) {
                        sbprint = sb.substring(0, endOfLineIndex);
                        //sb.delete(0, sb.length());
                        //inputtext.setText(inputtext.getText() + "\n" + sbprint);
                        inputMSGhandler.obtainMessage(1,sbprint).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}