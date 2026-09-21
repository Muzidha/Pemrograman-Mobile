package com.example.intentsederhana;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private static final String TAG = "SecondActivity";

    TextView txtNama, txtNrp;
    Button btnTutup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        txtNama = findViewById(R.id.txtNama);
        txtNrp = findViewById(R.id.txtNrp);
        btnTutup = findViewById(R.id.btnTutup);

        String nama = getIntent().getStringExtra("nama");
        String nrp = getIntent().getStringExtra("nrp");

        txtNama.setText("Nama: " + nama);
        txtNrp.setText("NRP: " + nrp);

        btnTutup.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: SecondActivity dihancurkan");
    }
}