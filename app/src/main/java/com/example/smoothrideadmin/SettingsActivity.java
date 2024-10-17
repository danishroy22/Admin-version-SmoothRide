package com.example.smoothrideadmin;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private Switch notificationsSwitch;
    private Switch darkModeSwitch;
    private CheckBox autoUpdateCheckbox;
    private Button saveSettingsButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        autoUpdateCheckbox = findViewById(R.id.autoUpdateCheckbox);
        saveSettingsButton = findViewById(R.id.saveSettingsButton);

        // Load saved preferences
        loadPreferences();

        // Save button click listener
        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                finish(); // Close the activity after saving
            }
        });
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        notificationsSwitch.setChecked(sharedPreferences.getBoolean("notificationsEnabled", true));
        darkModeSwitch.setChecked(sharedPreferences.getBoolean("darkModeEnabled", false));
        autoUpdateCheckbox.setChecked(sharedPreferences.getBoolean("autoUpdateEnabled", true));
    }

    private void savePreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("notificationsEnabled", notificationsSwitch.isChecked());
        editor.putBoolean("darkModeEnabled", darkModeSwitch.isChecked());
        editor.putBoolean("autoUpdateEnabled", autoUpdateCheckbox.isChecked());
        editor.apply();
    }
}