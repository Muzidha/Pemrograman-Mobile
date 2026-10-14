package com.example.tugassensor;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Mengganti tema aplikasi (Mode Terang / Mode Gelap) secara otomatis
 * berdasarkan sensor cahaya (TYPE_LIGHT).
 * Dipakai di setiap Activity supaya semua halaman ikut menyesuaikan cahaya ruangan.
 */
public class PengaturTema implements SensorEventListener {

    /** Pemberitahuan untuk Activity yang ingin menampilkan data sensor cahaya. */
    public interface Pendengar {
        void onLuxBerubah(float lux);

        void onStatusBerubah(@StringRes int status);
    }

    // ===== Konstanta =====
    // Dua batas berbeda (histeresis) supaya tema tidak bolak-balik berkedip
    // ketika cahaya ruangan berada tepat di sekitar satu nilai batas.
    public static final int BATAS_GELAP = 20;           // lux < 20  -> Mode Gelap
    public static final int BATAS_TERANG = 50;          // lux > 50  -> Mode Terang

    // Tema baru diganti jika kondisi cahaya bertahan selama jeda ini,
    // agar tidak berubah hanya karena sensor tertutup tangan sesaat.
    private static final long JEDA_GANTI_TEMA_MS = 1500;

    // ===== Sensor =====
    private final Context context;
    private final Pendengar pendengar;
    private final SensorManager sensorManager;
    private final Sensor sensorCahaya;

    // ===== State =====
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable gantiTemaTertunda;

    /**
     * @param context   Activity pemakai (bukan Application), agar mode tema yang terbaca sesuai
     * @param pendengar boleh null jika Activity tidak menampilkan data sensor cahaya
     */
    public PengaturTema(Context context, @Nullable Pendengar pendengar) {
        this.context = context;
        this.pendengar = pendengar;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorCahaya = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
                : null;
    }

    public boolean isSensorTersedia() {
        return sensorCahaya != null;
    }

    /** Panggil di onResume(). */
    public void mulai() {
        if (sensorCahaya != null) {
            sensorManager.registerListener(this, sensorCahaya, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /** Panggil di onPause(), supaya sensor dilepas dan hemat baterai. */
    public void berhenti() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        batalkanGantiTema();
    }

    // ===== SensorEventListener =====

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;

        float lux = event.values[0];
        if (pendengar != null) {
            pendengar.onLuxBerubah(lux);
        }
        sesuaikanTema(lux);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Tidak dipakai
    }

    // ===== Logika =====

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
            kabariStatus(R.string.status_sesuai);
            return;
        }

        // Pergantian sudah dijadwalkan, tunggu jedanya selesai
        if (gantiTemaTertunda != null) return;

        kabariStatus(harusGelap ? R.string.status_ke_gelap : R.string.status_ke_terang);
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

    private void kabariStatus(@StringRes int status) {
        if (pendengar != null) {
            pendengar.onStatusBerubah(status);
        }
    }

    private boolean isModeGelap() {
        int modeMalam = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return modeMalam == Configuration.UI_MODE_NIGHT_YES;
    }
}
