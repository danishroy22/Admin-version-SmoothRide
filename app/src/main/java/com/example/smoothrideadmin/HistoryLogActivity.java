package com.example.smoothrideadmin;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AppCompatActivity;

public class HistoryLogActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private ListView historyListView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_log_layout);

        databaseHelper = new DatabaseHelper(this);
        historyListView = findViewById(R.id.historyListView);

        displayHistory();
    }

    private void displayHistory() {
        Cursor cursor = databaseHelper.getAllRepairLogs();

        // Define the columns to display and the views in the layout
        String[] from = {
                DatabaseHelper.COLUMN_LATITUDE,
                DatabaseHelper.COLUMN_LONGITUDE,
                DatabaseHelper.COLUMN_TIMESTAMP
        };
        int[] to = {
                R.id.latTextView,
                R.id.lngTextView,
                R.id.dateTimeTextView
        };

        // Create an adapter and set it to the ListView
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_history_log,
                cursor,
                from,
                to,
                0
        );
        historyListView.setAdapter(adapter);
    }
}
