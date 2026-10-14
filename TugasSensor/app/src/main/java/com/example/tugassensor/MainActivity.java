package com.example.tugassensor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements PengaturTema.Pendengar {

    // ===== Konstanta =====
    private static final int BATAS_SANGAT_TERANG = 1000;
    private static final String KEY_LUX_TERAKHIR = "lux_terakhir";

    // ===== Sensor =====
    // Sensor cahaya + pergantian tema otomatis ada di kelas PengaturTema
    private PengaturTema pengaturTema;

    // ===== Views =====
    private TextView tvLux;
    private TextView tvKategori;
    private TextView tvStatus;
    private LinearProgressIndicator pbCahaya;

    // ===== State =====
    private float luxTerakhir = -1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvLux = findViewById(R.id.tvLux);
        tvKategori = findViewById(R.id.tvKategori);
        tvStatus = findViewById(R.id.tvStatus);
        pbCahaya = findViewById(R.id.pbCahaya);

        TextView tvInfo = findViewById(R.id.tvInfo);
        tvInfo.setText(getString(R.string.info_batas,
                PengaturTema.BATAS_GELAP, PengaturTema.BATAS_TERANG));

        findViewById(R.id.kartuKiblat).setOnClickListener(v ->
                startActivity(new Intent(this, KiblatActivity.class)));

        pengaturTema = new PengaturTema(this, this);
        if (!pengaturTema.isSensorTersedia()) {
            tvKategori.setText(R.string.kategori_tidak_tersedia);
            tvStatus.setText(R.string.status_tanpa_sensor);
            return;
        }

        // Saat tema berganti, Activity dibuat ulang. Tampilkan nilai lux terakhir
        // agar layar tidak kosong sambil menunggu data baru dari sensor.
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_LUX_TERAKHIR)) {
            luxTerakhir = savedInstanceState.getFloat(KEY_LUX_TERAKHIR);
            tampilkanLux(luxTerakhir);
            tvStatus.setText(R.string.status_sesuai);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pengaturTema.mulai();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Lepas sensor saat aplikasi tidak tampil supaya hemat baterai
        pengaturTema.berhenti();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (luxTerakhir >= 0) {
            outState.putFloat(KEY_LUX_TERAKHIR, luxTerakhir);
        }
    }

    // ===== PengaturTema.Pendengar =====

    @Override
    public void onLuxBerubah(float lux) {
        luxTerakhir = lux;
        tampilkanLux(lux);
    }

    @Override
    public void onStatusBerubah(@StringRes int status) {
        tvStatus.setText(status);
    }

    // ===== Tampilan =====

    private void tampilkanLux(float lux) {
        tvLux.setText(String.format(Locale.getDefault(), "%.0f", lux));
        tvKategori.setText(kategoriCahaya(lux));

        // Progress 0-100 mengikuti 0-1000 lux (di atas 1000 dianggap penuh)
        int progress = (int) (Math.min(lux, BATAS_SANGAT_TERANG) * 100 / BATAS_SANGAT_TERANG);
        pbCahaya.setProgressCompat(progress, true);
    }

    @StringRes
    private int kategoriCahaya(float lux) {
        if (lux < PengaturTema.BATAS_GELAP) return R.string.kategori_gelap;
        if (lux <= PengaturTema.BATAS_TERANG) return R.string.kategori_redup;
        if (lux < BATAS_SANGAT_TERANG) return R.string.kategori_terang;
        return R.string.kategori_sangat_terang;
    }
}
