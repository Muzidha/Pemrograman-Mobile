package com.example.tugassensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.location.LocationListenerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Kompas penunjuk arah kiblat.
 * Arah HP dihitung dari dua sensor hardware:
 * - TYPE_ACCELEROMETER  : arah gravitasi (mana bawah)
 * - TYPE_MAGNETIC_FIELD : medan magnet bumi (mana utara)
 */
public class KiblatActivity extends AppCompatActivity implements SensorEventListener {

    // ===== Konstanta =====
    // Koordinat Ka'bah di Makkah
    private static final double LAT_KABAH = 21.422487;
    private static final double LON_KABAH = 39.826206;

    // Lokasi cadangan (Monas, Jakarta) jika izin lokasi ditolak atau GPS mati
    private static final double LAT_DEFAULT = -6.175392;
    private static final double LON_DEFAULT = 106.827153;

    // Selisih sudut (derajat) yang sudah dianggap menghadap kiblat
    private static final float TOLERANSI_KIBLAT = 5f;

    // Faktor low-pass filter: makin kecil makin halus, tapi jarum makin lambat bergerak
    private static final float FAKTOR_HALUS = 0.1f;

    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_SUMBER = "sumber_lokasi";
    private static final String KEY_LOKASI_TERKINI = "lokasi_terkini";

    // ===== Sensor =====
    private SensorManager sensorManager;
    private Sensor sensorAkselerometer;
    private Sensor sensorMagnetometer;
    private PengaturTema pengaturTema;

    // ===== Lokasi =====
    private LocationManager locationManager;
    private final LocationListenerCompat pendengarLokasi = this::lokasiDiterima;
    private boolean sedangMencariLokasi = false;
    private boolean lokasiTerkiniDidapat = false;

    // ===== Views =====
    private KompasView kompasView;
    private TextView tvPetunjuk;
    private TextView tvKalibrasi;
    private TextView tvArahKiblat;
    private TextView tvArahHp;
    private TextView tvJarak;
    private TextView tvLokasi;
    private String[] namaArah;

    // ===== State =====
    private final float[] gravitasi = new float[3];
    private final float[] medanMagnet = new float[3];
    private boolean adaGravitasi = false;
    private boolean adaMedanMagnet = false;
    private final float[] matriksRotasi = new float[9];
    private final float[] orientasi = new float[3];

    private double latitude = LAT_DEFAULT;
    private double longitude = LON_DEFAULT;
    @StringRes
    private int sumberLokasi = R.string.sumber_mencari;
    private float arahKiblat;   // derajat dari utara sejati
    private float deklinasi;    // selisih utara magnet terhadap utara sejati
    private float arahHp;       // derajat dari utara sejati
    private int arahHpTampil = -1;
    private boolean menghadapKiblat = false;

    private final ActivityResultLauncher<String[]> mintaIzinLokasi =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), hasil -> {
                // Jika diizinkan, pencarian lokasi dimulai otomatis di onResume()
                if (!punyaIzinLokasi()) {
                    pakaiLokasi(LAT_DEFAULT, LON_DEFAULT, R.string.sumber_izin_ditolak);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kiblat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        kompasView = findViewById(R.id.kompas);
        tvPetunjuk = findViewById(R.id.tvPetunjuk);
        tvKalibrasi = findViewById(R.id.tvKalibrasi);
        tvArahKiblat = findViewById(R.id.tvArahKiblat);
        tvArahHp = findViewById(R.id.tvArahHp);
        tvJarak = findViewById(R.id.tvJarak);
        tvLokasi = findViewById(R.id.tvLokasi);
        namaArah = getResources().getStringArray(R.array.arah_mata_angin);

        // Halaman ini juga ikut berganti tema sesuai cahaya ruangan
        pengaturTema = new PengaturTema(this, null);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorAkselerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        if (sensorAkselerometer == null || sensorMagnetometer == null) {
            tvPetunjuk.setText(R.string.petunjuk_tanpa_sensor);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (savedInstanceState != null) {
            // Activity dibuat ulang (misalnya karena tema berganti): pakai lokasi sebelumnya
            lokasiTerkiniDidapat = savedInstanceState.getBoolean(KEY_LOKASI_TERKINI);
            pakaiLokasi(savedInstanceState.getDouble(KEY_LAT, LAT_DEFAULT),
                    savedInstanceState.getDouble(KEY_LON, LON_DEFAULT),
                    savedInstanceState.getInt(KEY_SUMBER, R.string.sumber_mencari));
        } else {
            pakaiLokasi(LAT_DEFAULT, LON_DEFAULT, R.string.sumber_mencari);
            if (!punyaIzinLokasi()) {
                mintaIzinLokasi.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pengaturTema.mulai();
        if (sensorAkselerometer != null && sensorMagnetometer != null) {
            sensorManager.registerListener(this, sensorAkselerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, sensorMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (!lokasiTerkiniDidapat && punyaIzinLokasi()) {
            mulaiCariLokasi();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Lepas semua sensor & GPS saat halaman tidak tampil supaya hemat baterai
        pengaturTema.berhenti();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        berhentiCariLokasi();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(KEY_LAT, latitude);
        outState.putDouble(KEY_LON, longitude);
        outState.putInt(KEY_SUMBER, sumberLokasi);
        outState.putBoolean(KEY_LOKASI_TERKINI, lokasiTerkiniDidapat);
    }

    // ===== SensorEventListener =====

    @Override
    public void onSensorChanged(SensorEvent event) {
        int tipe = event.sensor.getType();
        if (tipe == Sensor.TYPE_ACCELEROMETER) {
            haluskan(event.values, gravitasi, adaGravitasi);
            adaGravitasi = true;
        } else if (tipe == Sensor.TYPE_MAGNETIC_FIELD) {
            haluskan(event.values, medanMagnet, adaMedanMagnet);
            adaMedanMagnet = true;
        } else {
            return;
        }
        if (!adaGravitasi || !adaMedanMagnet) return;

        // Gabungkan arah gravitasi & medan magnet bumi menjadi matriks orientasi HP.
        // Gagal jika HP sedang jatuh bebas atau ada gangguan magnet yang kuat.
        if (!SensorManager.getRotationMatrix(matriksRotasi, null, gravitasi, medanMagnet)) return;
        SensorManager.getOrientation(matriksRotasi, orientasi);

        // orientasi[0] = azimuth (radian): arah atas HP terhadap utara magnet
        float azimuthMagnet = (float) Math.toDegrees(orientasi[0]);
        // Tambahkan deklinasi supaya menjadi arah terhadap utara sejati
        arahHp = normalisasi(azimuthMagnet + deklinasi);
        perbaruiKompas();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            boolean kurangAkurat = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW;
            tvKalibrasi.setVisibility(kurangAkurat ? View.VISIBLE : View.GONE);
        }
    }

    /** Low-pass filter supaya jarum kompas tidak bergetar. */
    private static void haluskan(float[] baru, float[] hasil, boolean sudahAda) {
        for (int i = 0; i < 3; i++) {
            hasil[i] = sudahAda ? hasil[i] + FAKTOR_HALUS * (baru[i] - hasil[i]) : baru[i];
        }
    }

    // ===== Lokasi =====

    private boolean punyaIzinLokasi() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission") // izin sudah dicek lewat punyaIzinLokasi()
    private void mulaiCariLokasi() {
        if (locationManager == null || sedangMencariLokasi) return;

        // Provider yang aktif & boleh diakses (gps, network, fused)
        List<String> daftarProvider = new ArrayList<>(locationManager.getProviders(true));
        daftarProvider.remove(LocationManager.PASSIVE_PROVIDER);
        if (daftarProvider.isEmpty()) {
            pakaiLokasi(LAT_DEFAULT, LON_DEFAULT, R.string.sumber_lokasi_mati);
            return;
        }

        // Tampilkan lokasi terakhir yang diketahui dulu supaya arah kiblat langsung muncul
        Location terakhir = null;
        for (String provider : daftarProvider) {
            Location lokasi = locationManager.getLastKnownLocation(provider);
            if (lokasi != null && (terakhir == null || lokasi.getTime() > terakhir.getTime())) {
                terakhir = lokasi;
            }
        }
        if (terakhir != null) {
            pakaiLokasi(terakhir.getLatitude(), terakhir.getLongitude(), R.string.sumber_terakhir);
        }

        // Lalu minta lokasi terkini dari semua provider, yang datang pertama dipakai
        for (String provider : daftarProvider) {
            locationManager.requestLocationUpdates(provider, 0L, 0f, pendengarLokasi,
                    Looper.getMainLooper());
        }
        sedangMencariLokasi = true;
    }

    private void berhentiCariLokasi() {
        if (sedangMencariLokasi) {
            locationManager.removeUpdates(pendengarLokasi);
            sedangMencariLokasi = false;
        }
    }

    private void lokasiDiterima(@NonNull Location lokasi) {
        lokasiTerkiniDidapat = true;
        berhentiCariLokasi();
        pakaiLokasi(lokasi.getLatitude(), lokasi.getLongitude(), R.string.sumber_terkini);
    }

    /** Hitung arah kiblat, jarak ke Ka'bah, dan deklinasi magnet dari sebuah lokasi. */
    private void pakaiLokasi(double lat, double lon, @StringRes int sumber) {
        latitude = lat;
        longitude = lon;
        sumberLokasi = sumber;

        // distanceBetween menghitung jalur terpendek di permukaan bumi (great circle):
        // hasil[0] = jarak (meter), hasil[1] = arah awal/bearing dari utara sejati (derajat)
        float[] hasil = new float[2];
        Location.distanceBetween(lat, lon, LAT_KABAH, LON_KABAH, hasil);
        arahKiblat = normalisasi(hasil[1]);

        // Kompas menunjuk utara magnet, padahal arah kiblat dihitung dari utara sejati.
        // Selisih keduanya (deklinasi) berbeda di tiap lokasi di bumi.
        deklinasi = new GeomagneticField((float) lat, (float) lon, 0f,
                System.currentTimeMillis()).getDeclination();

        int kiblatBulat = Math.round(arahKiblat) % 360;
        tvArahKiblat.setText(getString(R.string.format_arah, kiblatBulat, namaArah(kiblatBulat)));
        tvJarak.setText(getString(R.string.format_jarak, hasil[0] / 1000f));
        tvLokasi.setText(getString(R.string.format_lokasi, lat, lon, getString(sumber)));
        kompasView.setArah(arahHp, arahKiblat);
        perbaruiKompas();
    }

    // ===== Tampilan =====

    private void perbaruiKompas() {
        if (!adaGravitasi || !adaMedanMagnet) return; // belum ada data sensor

        kompasView.setArah(arahHp, arahKiblat);

        int hpBulat = Math.round(arahHp) % 360;
        if (hpBulat != arahHpTampil) {
            arahHpTampil = hpBulat;
            tvArahHp.setText(getString(R.string.format_arah, hpBulat, namaArah(hpBulat)));
        }

        // Selisih -180..180 derajat: positif = kiblat ada di sebelah kanan
        float selisih = normalisasi(arahKiblat - arahHp);
        if (selisih > 180) selisih -= 360;
        boolean pas = Math.abs(selisih) <= TOLERANSI_KIBLAT;

        if (pas) {
            tvPetunjuk.setText(R.string.petunjuk_pas);
        } else {
            tvPetunjuk.setText(getString(selisih > 0 ? R.string.petunjuk_kanan : R.string.petunjuk_kiri,
                    Math.round(Math.abs(selisih))));
        }

        if (pas != menghadapKiblat) {
            menghadapKiblat = pas;
            kompasView.setMenghadapKiblat(pas);
            tvPetunjuk.setTextColor(ContextCompat.getColor(this,
                    pas ? R.color.sukses : R.color.teks_utama));
            if (pas) {
                // Getar singkat saat HP pas menghadap kiblat
                kompasView.performHapticFeedback(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.CONFIRM
                        : HapticFeedbackConstants.VIRTUAL_KEY);
            }
        }
    }

    /** Nama arah mata angin (8 arah) dari sudut dalam derajat. */
    private String namaArah(int derajat) {
        return namaArah[Math.round(derajat / 45f) % 8];
    }

    /** Ubah sudut berapa pun menjadi rentang 0..360 derajat. */
    private static float normalisasi(float derajat) {
        return ((derajat % 360) + 360) % 360;
    }
}
