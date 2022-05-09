package com.example.fancontrol;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    Button bconnect, connectToFan;
    TextView logview, inputtext;
    ListView listDevice;
    ProgressBar statusFan;
    Switch logSwitch;
    volatile boolean writeLogfile = false;
    String dataforLOG = ""; //надо бы синхронизировать запись в переменную и запись в файл
    int forsedFan = 0; //принудительное включение Fan
    PipedWriter pipedWriterLOG;
    DataLogging dataLogging;


    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    Thread myThreadIOdata;
    Handler inputMSGhandler;
    String fileSettings = "/storage/emulated/0/Download/FanLOG/settings.xml"; //файл с настройками приложения


    Set<BluetoothDevice> paredDev;
    ArrayList<String> pairedDeviceArrayList;
    ArrayAdapter<String> pairedDeviceAdapter;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bconnect = (Button) findViewById(R.id.buttonconnect);
        connectToFan = (Button) findViewById(R.id.connectFan);
        logview = (TextView) findViewById(R.id.loggingview);
        listDevice = (ListView) findViewById(R.id.listdevice);
        inputtext = (TextView) findViewById(R.id.textfromfancontrol);
        statusFan = (ProgressBar) findViewById(R.id.statusFan);
        logSwitch = (Switch) findViewById(R.id.writeLogFile);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "You device BLUETOOTH not support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String stInfo = btAdapter.getName() + " = " + btAdapter.getAddress() + "State bt module = " + btAdapter.getState();
        logview.setText(String.format("Это устройство:\n %s", stInfo));

        inputMSGhandler = new Handler(){
            int i = 0;
            public void handleMessage(android.os.Message msg) {
                String temp = (String) inputtext.getText();
                inputtext.setText(msg.obj.toString() + " ==== line = " + i + "\n" + temp);
                i = i+1;
                if(writeLogfile) {
                    dataforLOG = msg.obj.toString(); //по старой логике для работы с внутренним классом
                    try {
                        pipedWriterLOG.write(dataforLOG);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //Индикация состояния Fan
                if((msg.obj.toString()).contains("state fan = ")) {
                    if (checkStatusFan(msg.obj.toString())) {
                        statusFan.setVisibility(ProgressBar.VISIBLE);
                    }
                    else {
                        statusFan.setVisibility(ProgressBar.INVISIBLE);
                    }
                }

            }
        };
    }

    boolean checkStatusFan(String data){
            String tmp = data.substring(12);
            int i = 0;
            try{
                i = Integer.parseInt(tmp);
            }
            catch (NumberFormatException ex){
                ex.printStackTrace();
            }
        return i != 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
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
                    connectToFan.setEnabled(false);
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
        //State "forsedFan" check in I\O Thread
        if(forsedFan == 0){
            forsedFan = 1;
            inputtext.setText(tmp + "\n принудительно forsedFan = " + forsedFan);
        }else{
            forsedFan = 0;
            inputtext.setText(tmp + "\n принудительно forsedFan = " + forsedFan);
        }
    }

    public void clearDataDisplay(View view) {
        inputtext.setText("Input data from Fan controller\n");
    }

    public void connectToFan(View view) {
        connectToFan.setClickable(false); //чтоб второй раз не подключиться
        String MACcontroller = "98:D3:31:B0:81:7F";
        BluetoothDevice device2 = btAdapter.getRemoteDevice(MACcontroller);
        String tmp = (String) logview.getText();
        logview.setText(tmp + "\n Connect to: " + MACcontroller);
        btAdapter.cancelDiscovery();
        BTFanConnection myThreadConnectBTdevice = new BTFanConnection(device2);
        myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
    }

    public void startSettingView(View view) {
        Intent intent = new Intent(this, SettingActivity.class);
        intent.putExtra("namefile", fileSettings);
        startActivity(intent);

    }

    @SuppressLint("NewApi")
    public void onCheckedLogSwitch(View view) {
        //логика в Handler'e
        Thread t = null;
        if(!writeLogfile){
            writeLogfile = true;
            pipedWriterLOG = new PipedWriter();
            dataLogging = new DataLogging(pipedWriterLOG);
            t = new Thread(dataLogging);
            t.start();
            Log.i("DataLogging", "start DataLogging from main class");
            logview.setText(logview.getText() + "\n Пишем логи в файл...");

            try {
                pipedWriterLOG.write("privet медвед \n"); //test message
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            writeLogfile = false;
            try {
                pipedWriterLOG.close(); //вызывает исключение в DataLogging. finalize закрывает файл.
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataLogging.shutDown();
            Log.i("DataLogging", "interrapt thread DataLogging from main class");

            logview.setText(logview.getText() + "\n Запись в файл остановлена.\nФайл сохранен в /Download/FanLOG/");
        }

        /*
        WriteLogs writeLogs;
        if(!writeLogfile){
            writeLogfile = true;
            logview.setText(logview.getText() + "\n Пишем логи в файл...");
            writeLogs = new WriteLogs();
            writeLogs.start();
        }else{
            writeLogfile = false;
            logview.setText(logview.getText() + "\n Запись в файл остановлена.\nФайл сохранен в /Download/FanLOG/");
        } */
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
                    //На моём Honor 50 ни разу не работает. Костыль ниже решает проблему(в оф. документации Андроид нет такого)
                    btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            try{
                btSocket.connect();
                success = true;
            } catch (IOException e) {
                Log.e("BTFanConnection_log",e.getMessage());
                try {
                    Log.e("BTFanConnection_log","trying fallback...");
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
                    Log.i("BTFanConnection_log","Connected");
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
                Log.i("BTFanConnection_log","Create thread IO");
                myThreadIOdata = new ThreadIOdata(btSocket);
                myThreadIOdata.start();
            }
        }
    }

    private class ThreadIOdata extends Thread {

        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private String sbprint;
        private StringBuilder sb;

        public ThreadIOdata(BluetoothSocket btSocket) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = btSocket.getInputStream();
                out = btSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    btSocket.close();
                    Log.e("ThreadIOdata_log", "btSocket = close, error in open io stream");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            connectedInputStream = in;
            connectedOutputStream = out;
            sb = new StringBuilder();
        }

        private void sendData(int data){
            try {
                connectedOutputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
                        inputMSGhandler.obtainMessage(1,sbprint).sendToTarget();
                    }
                    //if(forsedFan) //надо отрабатывать только по изменению
                } catch (IOException e) {
                    try {
                        connectedInputStream.close();
                        connectedOutputStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    //вынесен в DataLogging
    private class WriteLogs extends Thread{
        /*пишем логи в файл. запускаем и стопим по чекбоксу на главном экране(чекбокс доступен после успешного соединения).
        файл называем Log+текущая дата.время
        Handler получает данные из потока ThreadIOdata
        */
        private String filename;
        private File logfile;
        private FileWriter fileWriter;

        @RequiresApi(api = Build.VERSION_CODES.R)
        public WriteLogs() {
            @SuppressLint("SimpleDateFormat")
            String thetime = new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime());
            //filename = Environment.getStorageDirectory()
            filename = "/storage/emulated/0/Download/FanLOG/Log_" + thetime;
            logfile = new File(filename);

            try {
                if(!logfile.exists())
                    logfile.createNewFile();
                fileWriter = new FileWriter(logfile);
                fileWriter.append(thetime + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            fileWriter.close();
        }

        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            super.run();
            while(writeLogfile){
                if(!dataforLOG.equals("")){
                    try {
                        fileWriter.append(dataforLOG).append("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }



    }
}