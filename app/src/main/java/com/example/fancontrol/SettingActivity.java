package com.example.fancontrol;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Placeholder;
import androidx.fragment.app.Fragment;

import java.io.File;

public class SettingActivity extends AppCompatActivity {

    private int tactivation;
    private int tdeactivation;
    EditText tact, tdeact;
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

        Intent intent = getIntent();
        String fname = intent.getStringExtra("namefile");

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @SuppressLint("ResourceType")
    public void savesetting(View view) {
        tact.setText(2);

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
