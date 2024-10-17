package com.example.smoothrideadmin;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_us);

        // Here you can set the content of the about us text dynamically if needed
        String aboutUsContent = "SmoothRide is dedicated to ensuring a safe and smooth journey for all.\n" +
                "Our mission is to fix potholes in our roads and improve the quality of transportation.\n" +
                "We believe in community engagement and transparency.\n\n" +
                "Our team is committed to providing the best service possible.\n\n" +
                "Thank you for being a part of our journey!\n\n" +
                "We are Anissah, Danish, Ritesh and Yusuf";

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        TextView aboutUsTextView = findViewById(R.id.aboutUsTextView);
        aboutUsTextView.setText(aboutUsContent);
    }
}