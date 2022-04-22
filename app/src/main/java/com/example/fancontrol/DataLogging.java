package com.example.fancontrol;

import android.util.Log;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DataLogging implements Runnable{

    PipedReader reader;
    PipedWriter writer;
    private File logfile;
    private String filename;
    private FileWriter fileWriter;
    private volatile boolean shutdown = false;

    public DataLogging(PipedWriter writer) {
        this.writer = writer;
        Log.i("DataLogging", "DataLogging constructor");
        try {
            reader = new PipedReader(writer);
            Log.i("DataLogging", "pipedreader OK");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String thetime = new SimpleDateFormat("yyyyMMdd_HHmm-ss").format(Calendar.getInstance().getTime());
        logfile = new File(("/storage/emulated/0/Download/FanLOG/Log_" + thetime + ".txt"));
        if(!logfile.exists()) {
            try {
                boolean f = logfile.createNewFile();
                fileWriter = new FileWriter(logfile);
                fileWriter.write(thetime + '\n');
                Log.i("DataLogging", "filewriter create + " + logfile.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (!shutdown){
            try {
                String tmp = "";
                char c;
                //пишем в файл по строкам
                while((c = (char)reader.read())!= '\n'){ //блокирующая операция. ждет новых данных. Пишем по строкам
                    tmp = tmp + c;
                }
                fileWriter.write(tmp + "\n");
            } catch (IOException e1) {
                e1.printStackTrace();
                Log.i("DataLogging","IOException e1");
            }
        }

    }

    public void shutDown(){
        Log.i("DataLogging","called shutDown");
        shutdown = true;
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
                finalize();
            } catch (Throwable e) {
                e.printStackTrace();
            }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        fileWriter.close();
        Log.i("DataLogging", "Stop thread and close logFile");
    }
}
