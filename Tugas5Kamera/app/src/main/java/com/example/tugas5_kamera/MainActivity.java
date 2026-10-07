package com.example.tugas5_kamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ===== Konstanta =====
    private static final int REQUEST_PERMISSIONS = 100;

    // ===== Views =====
    private ImageView imageView;
    private TextView tvStatus;
    private TextView tvSavedPath;
    private LinearLayout btnAmbilFoto;
    private LinearLayout btnRekamVideo;
    private Button btnLihatGaleri;

    // ===== State =====
    // Kita simpan referensi ke File aslinya (bukan hanya URI)
    // agar bisa: decode bitmap langsung, scan MediaStore, dan buka galeri
    private File currentPhotoFile;
    private File currentVideoFile;

    // URI MediaStore (setelah di-scan) untuk membuka di galeri
    private Uri lastMediaStoreUri;

    // ===== Activity Result Launchers =====

    /**
     * Launcher untuk ambil foto.
     * Setelah kamera selesai, baca foto dari file langsung → tampilkan preview.
     */
    private final ActivityResultLauncher<Intent> launcherFoto =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK) {
                                tampilkanPreviewFoto();
                            } else {
                                tvStatus.setText("Pengambilan foto dibatalkan.");
                                // Hapus file kosong jika ada
                                if (currentPhotoFile != null && currentPhotoFile.exists()
                                        && currentPhotoFile.length() == 0) {
                                    currentPhotoFile.delete();
                                }
                                currentPhotoFile = null;
                            }
                        }
                    });

    /**
     * Launcher untuk rekam video.
     */
    private final ActivityResultLauncher<Intent> launcherVideo =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK) {
                                tampilkanSuksesVideo();
                            } else {
                                tvStatus.setText("Perekaman video dibatalkan.");
                                if (currentVideoFile != null && currentVideoFile.exists()
                                        && currentVideoFile.length() == 0) {
                                    currentVideoFile.delete();
                                }
                                currentVideoFile = null;
                            }
                        }
                    });

    // ===== Lifecycle =====

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inisialisasiViews();
        setListeners();
    }

    // ===== Inisialisasi =====

    private void inisialisasiViews() {
        imageView     = findViewById(R.id.imageView);
        tvStatus      = findViewById(R.id.tvStatus);
        tvSavedPath   = findViewById(R.id.tvSavedPath);
        btnAmbilFoto  = findViewById(R.id.btnAmbilFoto);
        btnRekamVideo = findViewById(R.id.btnRekamVideo);
        btnLihatGaleri = findViewById(R.id.btnLihatGaleri);
    }

    private void setListeners() {
        btnAmbilFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cekDanMintaIzin()) {
                    bukaKameraFoto();
                }
            }
        });

        btnRekamVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cekDanMintaIzin()) {
                    bukaKameraVideo();
                }
            }
        });

        btnLihatGaleri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bukaGaleri();
            }
        });
    }

    // ===== Permission =====

    private boolean cekDanMintaIzin() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        // Storage write hanya diperlukan untuk Android 9 ke bawah
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_PERMISSIONS
            );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean semuaDiizinkan = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    semuaDiizinkan = false;
                    break;
                }
            }
            if (semuaDiizinkan) {
                Toast.makeText(this, "Izin diberikan! Silakan coba lagi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Izin kamera/storage diperlukan untuk menggunakan fitur ini.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ===== Kamera Foto =====

    /**
     * Buka kamera untuk ambil foto.
     * Output langsung disimpan ke DCIM/Camera via FileProvider URI.
     */
    private void bukaKameraFoto() {
        try {
            currentPhotoFile = buatFile("IMG_", ".jpg",
                    Environment.DIRECTORY_DCIM + "/Camera");
            if (currentPhotoFile == null) {
                Toast.makeText(this, "Gagal menyiapkan file foto.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri fotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    currentPhotoFile
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            tvStatus.setText("Membuka kamera...");
            launcherFoto.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Tampilkan preview foto dari path file langsung (bukan lewat URI content).
     * Ini lebih andal dan tidak butuh permission tambahan.
     */
    private void tampilkanPreviewFoto() {
        if (currentPhotoFile == null || !currentPhotoFile.exists()) {
            tvStatus.setText("File foto tidak ditemukan.");
            return;
        }

        // Decode bitmap dari file path langsung — tidak perlu buka stream via ContentResolver
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2; // Downsample 2x agar tidak OOM
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoFile.getAbsolutePath(), options);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            tvStatus.setText("Gagal menampilkan preview foto.");
            return;
        }

        tvStatus.setText("✅ Foto berhasil disimpan!");
        tvSavedPath.setText("📁 DCIM/Camera/" + currentPhotoFile.getName());

        // Scan file ke MediaStore agar muncul di Galeri lokal
        scanFileKeMediaStore(currentPhotoFile.getAbsolutePath(), "image/jpeg");

        Toast.makeText(this, "Foto disimpan ke DCIM/Camera 📷", Toast.LENGTH_SHORT).show();
    }

    // ===== Kamera Video =====

    /**
     * Buka kamera untuk rekam video.
     * Output langsung disimpan ke DCIM/Camera.
     */
    private void bukaKameraVideo() {
        try {
            currentVideoFile = buatFile("VID_", ".mp4",
                    Environment.DIRECTORY_DCIM + "/Camera");
            if (currentVideoFile == null) {
                Toast.makeText(this, "Gagal menyiapkan file video.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri videoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    currentVideoFile
            );

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // Kualitas tinggi
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            tvStatus.setText("Membuka kamera video...");
            launcherVideo.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Tampilkan status sukses setelah rekam video.
     */
    private void tampilkanSuksesVideo() {
        if (currentVideoFile == null || !currentVideoFile.exists()) {
            tvStatus.setText("File video tidak ditemukan.");
            return;
        }

        // Tampilkan icon video (thumbnail sulit di-generate tanpa async)
        imageView.setImageResource(R.drawable.ic_photo_placeholder);

        tvStatus.setText("✅ Video berhasil disimpan!");
        tvSavedPath.setText("📁 DCIM/Camera/" + currentVideoFile.getName());

        // Scan file ke MediaStore agar muncul di Galeri lokal
        scanFileKeMediaStore(currentVideoFile.getAbsolutePath(), "video/mp4");

        Toast.makeText(this, "Video disimpan ke DCIM/Camera 🎥", Toast.LENGTH_SHORT).show();
    }

    // ===== Helper =====

    /**
     * Buat file baru di folder DCIM/Camera dengan nama berbasis timestamp.
     *
     * @param prefix  Awalan nama file, misalnya "IMG_" atau "VID_"
     * @param suffix  Ekstensi file, misalnya ".jpg" atau ".mp4"
     * @param subDir  Subdirektori relatif dari getExternalStorageDirectory, misalnya "DCIM/Camera"
     * @return File yang sudah dibuat, atau null jika gagal.
     */
    private File buatFile(String prefix, String suffix, String subDir) {
        try {
            String timestamp = DateFormat.format("MM-dd-yy_hh-mm-ss", new Date()).toString();
            String namaFile = prefix + timestamp + suffix;

            File folder = new File(
                    Environment.getExternalStorageDirectory(), subDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, namaFile);
            file.createNewFile();
            return file;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Scan file ke MediaStore menggunakan MediaScannerConnection.
     * Setelah ini, file langsung muncul di aplikasi Galeri/Files tanpa restart.
     * Simpan URI MediaStore yang didapat untuk keperluan membuka galeri.
     *
     * @param filePath  Path absolut file yang ingin di-scan.
     * @param mimeType  MIME type file, misalnya "image/jpeg" atau "video/mp4".
     */
    private void scanFileKeMediaStore(final String filePath, final String mimeType) {
        MediaScannerConnection.scanFile(
                this,
                new String[]{ filePath },
                new String[]{ mimeType },
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        // uri ini adalah URI MediaStore yang valid dan bisa dibuka galeri mana saja
                        if (uri != null) {
                            lastMediaStoreUri = uri;
                        }
                    }
                }
        );
    }

    /**
     * Buka file terakhir di galeri lokal menggunakan MediaStore URI.
     * Jika belum ada file, buka galeri umum.
     */
    private void bukaGaleri() {
        Intent intent;

        if (lastMediaStoreUri != null) {
            // Buka foto/video terakhir yang tersimpan di galeri lokal
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(lastMediaStoreUri, "image/*");
        } else if (currentPhotoFile != null && currentPhotoFile.exists()) {
            // Fallback: gunakan Uri.fromFile (Android < 7 tidak butuh FileProvider untuk ACTION_VIEW)
            intent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = Uri.fromFile(currentPhotoFile);
            intent.setDataAndType(fileUri, "image/*");
        } else {
            // Buka galeri umum
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("image/*");
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            // Fallback ke file manager
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("*/*");
            startActivity(fallback);
        }
    }
}
