package com.example.intentsederhana;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    TextInputEditText edtNama, edtNrp, edtProgramStudi;
    Button btnPindah, btnTutup, btnPilihFoto;
    ImageView imgFoto;
    Uri fotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtNama = findViewById(R.id.edtNama);
        edtNrp = findViewById(R.id.edtNrp);
        edtProgramStudi = findViewById(R.id.edtProgramStudi);
        imgFoto = findViewById(R.id.imgFoto);
        btnPilihFoto = findViewById(R.id.btnPilihFoto);
        btnPindah = findViewById(R.id.btnPindah);
        btnTutup = findViewById(R.id.btnTutup);

        btnPilihFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnPindah.setOnClickListener(v -> {
            String nama = edtNama.getText().toString();
            String nrp = edtNrp.getText().toString();
            String prodi = edtProgramStudi.getText().toString();

            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("nama", nama);
            intent.putExtra("nrp", nrp);
            intent.putExtra("prodi", prodi);
            if (fotoUri != null) {
                intent.putExtra("foto", fotoUri.toString());
            }

            startActivity(intent);

            // REQ: Setelah pindah halaman, isian di-destroy/dibersihkan sehingga tidak tersimpan
            edtNama.setText("");
            edtNrp.setText("");
            edtProgramStudi.setText("");
            imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
            fotoUri = null;
        });

        btnTutup.setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fotoUri = data.getData();
            imgFoto.setImageURI(fotoUri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MainActivity dihancurkan");
    }
}