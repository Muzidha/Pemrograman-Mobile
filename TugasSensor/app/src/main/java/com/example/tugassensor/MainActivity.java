package com.example.tugassensor;

import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // ===== Konstanta =====
    // Dua batas berbeda (histeresis) supaya tema tidak bolak-balik berkedip
    // ketika cahaya ruangan berada tepat di sekitar satu nilai batas.
    private static final int BATAS_GELAP = 20;          // lux < 20  -> Mode Gelap
    private static final int BATAS_TERANG = 50;         // lux > 50  -> Mode Terang
    private static final int BATAS_SANGAT_TERANG = 1000;

    // Tema baru diganti jika kondisi cahaya bertahan selama jeda ini,
    // agar tidak berubah hanya karena sensor tertutup tangan sesaat.
    private static final long JEDA_GANTI_TEMA_MS = 1500;

    private static final String KEY_LUX_TERAKHIR = "lux_terakhir";

    // ===== Sensor =====
    private SensorManager sensorManager;
    private Sensor sensorCahaya;

    // ===== Views =====
    private TextView tvLux;
    private TextView tvKategori;
    private TextView tvStatus;
    private LinearProgressIndicator pbCahaya;

    // ===== State =====
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable gantiTemaTertunda;
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
        tvInfo.setText(getString(R.string.info_batas, BATAS_GELAP, BATAS_TERANG));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorCahaya = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        if (sensorCahaya == null) {
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
        if (sensorCahaya != null) {
            sensorManager.registerListener(this, sensorCahaya, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Lepas sensor saat aplikasi tidak tampil supaya hemat baterai
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        batalkanGantiTema();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (luxTerakhir >= 0) {
            outState.putFloat(KEY_LUX_TERAKHIR, luxTerakhir);
        }
    }

    // ===== SensorEventListener =====

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;

        float lux = event.values[0];
        luxTerakhir = lux;
        tampilkanLux(lux);
        sesuaikanTema(lux);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Tidak dipakai
    }

    // ===== Logika =====

    private void tampilkanLux(float lux) {
        tvLux.setText(String.format(Locale.getDefault(), "%.0f", lux));
        tvKategori.setText(kategoriCahaya(lux));

        // Progress 0-100 mengikuti 0-1000 lux (di atas 1000 dianggap penuh)
        int progress = (int) (Math.min(lux, BATAS_SANGAT_TERANG) * 100 / BATAS_SANGAT_TERANG);
        pbCahaya.setProgressCompat(progress, true);
    }

    @StringRes
    private int kategoriCahaya(float lux) {
        if (lux < BATAS_GELAP) return R.string.kategori_gelap;
        if (lux <= BATAS_TERANG) return R.string.kategori_redup;
        if (lux < BATAS_SANGAT_TERANG) return R.string.kategori_terang;
        return R.string.kategori_sangat_terang;
    }

    /**
     * Menentukan apakah tema perlu diganti berdasarkan nilai lux.
     * Gelap  -> Mode Gelap, Terang -> Mode Terang,
     * Redup (di antara dua batas) -> tema yang sedang aktif dipertahankan.
     */
    private void sesuaikanTema(float lux) {
        boolean sedangGelap = isModeGelap();
        boolean harusGelap;
        if (lux < BATAS_GELAP) {
            harusGelap = true;
        } else if (lux > BATAS_TERANG) {
            harusGelap = false;
        } else {
            harusGelap = sedangGelap;
        }

        if (harusGelap == sedangGelap) {
            batalkanGantiTema();
            tvStatus.setText(R.string.status_sesuai);
            return;
        }

        // Pergantian sudah dijadwalkan, tunggu jedanya selesai
        if (gantiTemaTertunda != null) return;

        tvStatus.setText(harusGelap ? R.string.status_ke_gelap : R.string.status_ke_terang);
        final int modeBaru = harusGelap
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        gantiTemaTertunda = () -> {
            gantiTemaTertunda = null;
            // Activity otomatis dibuat ulang memakai resource values / values-night
            AppCompatDelegate.setDefaultNightMode(modeBaru);
        };
        handler.postDelayed(gantiTemaTertunda, JEDA_GANTI_TEMA_MS);
    }

    private void batalkanGantiTema() {
        if (gantiTemaTertunda != null) {
            handler.removeCallbacks(gantiTemaTertunda);
            gantiTemaTertunda = null;
        }
    }

    private boolean isModeGelap() {
        int modeMalam = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return modeMalam == Configuration.UI_MODE_NIGHT_YES;
    }
}
