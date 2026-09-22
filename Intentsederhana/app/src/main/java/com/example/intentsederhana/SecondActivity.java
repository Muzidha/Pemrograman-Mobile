package com.example.intentsederhana;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private static final String TAG = "SecondActivity";

    TextView txtNama, txtNrp, txtProgramStudi;
    ImageView imgFotoDetail;
    Button btnTutup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        txtNama = findViewById(R.id.txtNama);
        txtNrp = findViewById(R.id.txtNrp);
        txtProgramStudi = findViewById(R.id.txtProgramStudi);
        imgFotoDetail = findViewById(R.id.imgFotoDetail);
        btnTutup = findViewById(R.id.btnTutup);

        String nama = getIntent().getStringExtra("nama");
        String nrp = getIntent().getStringExtra("nrp");
        String prodi = getIntent().getStringExtra("prodi");
        String fotoString = getIntent().getStringExtra("foto");

        txtNama.setText(nama != null && !nama.isEmpty() ? nama : "-");
        txtNrp.setText(nrp != null && !nrp.isEmpty() ? nrp : "-");
        txtProgramStudi.setText(prodi != null && !prodi.isEmpty() ? prodi : "-");

        if (fotoString != null) {
            try {
                imgFotoDetail.setImageURI(Uri.parse(fotoString));
            } catch (Exception e) {
                Log.e(TAG, "Gagal memuat URI Foto", e);
                imgFotoDetail.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            imgFotoDetail.setImageResource(android.R.drawable.ic_menu_camera);
        }

        btnTutup.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: SecondActivity dihancurkan");
    }
}