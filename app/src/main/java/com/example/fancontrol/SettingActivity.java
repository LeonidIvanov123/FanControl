package com.example.fancontrol;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Placeholder;
import androidx.fragment.app.Fragment;

import java.io.*;

import static com.example.fancontrol.R.id.tactivation;


public class SettingActivity extends AppCompatActivity {

    //this parametrs send to microcontroller for create temperature activation and deactivation FAN
    private int tactivation;
    private int tdeactivation;
    EditText tact, tdeact;
    TextView logdata;
    Button saveset;
    File fileSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
     /*   if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
        }
      */
        tact = (EditText) findViewById(R.id.tactivation);
        tdeact = (EditText) findViewById(R.id.tdeactivation);
        saveset = (Button) findViewById(R.id.savesettings);
        logdata = (TextView) findViewById(R.id.logSettingAct);

        Intent intent = getIntent();
        String fname = intent.getStringExtra("namefile"); //Name file with settings from Main thread
        fileSettings = new File(fname);
        if(!fileSettings.exists()){
            logdata.setText("file not found" + fname);
        }else {
            //read saved data to edittext
            try {
                FileReader fr = new FileReader(fileSettings);
                BufferedReader br = new BufferedReader(fr);
                String tmp = br.readLine();
                tact.setText(tmp);
                tmp = br.readLine();
                tdeact.setText(tmp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @SuppressLint("ResourceType")
    public void savesetting(View view) {
        int ta = Integer.parseInt(String.valueOf(tact.getText()));
        int tde = Integer.parseInt(String.valueOf(tdeact.getText()));
        //Create file if file !exist()
        if(!fileSettings.exists()){
            try {
                boolean f = fileSettings.createNewFile();
                logdata.setText("create new setting-file: " + f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileSettings));
            bw.write(ta);
            bw.write("\n");
            bw.write(tde);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static class PlaceholderFragment extends Fragment {
        public PlaceholderFragment() { }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_settings, container, false);
            return rootView;
        }
    }

}
