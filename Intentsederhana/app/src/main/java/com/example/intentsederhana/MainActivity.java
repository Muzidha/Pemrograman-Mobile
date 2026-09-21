package com.example.intentsederhana; // FIX: hilangkan spasi

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    EditText edtNama, edtNrp;
    Button btnPindah, btnTutup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtNama = findViewById(R.id.edtNama);
        edtNrp = findViewById(R.id.edtNrp);
        btnPindah = findViewById(R.id.btnPindah);
        btnTutup = findViewById(R.id.btnTutup);

        btnPindah.setOnClickListener(v -> {
            String nama = edtNama.getText().toString();
            String nrp = edtNrp.getText().toString();

            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("nama", nama);
            intent.putExtra("nrp", nrp);

            startActivity(intent);
        });

        btnTutup.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MainActivity dihancurkan");
    }
}