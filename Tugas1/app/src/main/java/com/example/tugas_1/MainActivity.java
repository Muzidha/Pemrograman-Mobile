package com.example.tugas_1;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etAngka1, etAngka2;
    private TextView tvHasil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etAngka1 = findViewById(R.id.etAngka1);
        etAngka2 = findViewById(R.id.etAngka2);
        tvHasil = findViewById(R.id.tvHasil);
    }

    public void onOperasiClick(View view) {
        String input1 = etAngka1.getText().toString().trim();
        String input2 = etAngka2.getText().toString().trim();

        if (input1.isEmpty() || input2.isEmpty()) {
            tvHasil.setText("Input harus angka");
            return;
        }

        try {
            double n1 = Double.parseDouble(input1);
            double n2 = Double.parseDouble(input2);
            double hasil = 0;
            int viewId = view.getId();

            if (viewId == R.id.btnTambah) {
                hasil = n1 + n2;
            } else if (viewId == R.id.btnKurang) {
                hasil = n1 - n2;
            } else if (viewId == R.id.btnKali) {
                hasil = n1 * n2;
            } else if (viewId == R.id.btnBagi) {
                if (n2 != 0) {
                    hasil = n1 / n2;
                } else {
                    tvHasil.setText("Tidak bisa bagi 0");
                    return;
                }
            }

            String hasilStr = (hasil == Math.floor(hasil) && !Double.isInfinite(hasil))
                    ? String.valueOf((long) hasil)
                    : String.valueOf(hasil);

            tvHasil.setText("Hasil: " + hasilStr);

        } catch (NumberFormatException e) {
            tvHasil.setText("Input harus angka");
        }
    }
}