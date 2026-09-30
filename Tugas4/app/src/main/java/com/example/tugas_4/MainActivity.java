package com.example.tugas_4;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView lvKontak;
    private KontakAdapter kAdapter;
    private SQLiteDatabase dbKu;
    private DbHelper dbHelper;

    private LinearLayout layoutEmptyState;
    private TextView tvJumlahKontak;

    private ArrayList<Kontak> listKontak;
    private String selectedFotoUri = "";
    private ImageView activeDialogFotoView = null;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFotoUri = uri.toString();
                    if (activeDialogFotoView != null) {
                        try {
                            activeDialogFotoView.setImageURI(uri);
                        } catch (Exception e) {
                            activeDialogFotoView.setImageResource(R.drawable.ic_person);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvKontak        = findViewById(R.id.lvKontak);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvJumlahKontak  = findViewById(R.id.tvJumlahKontak);

        ExtendedFloatingActionButton btnTambah = findViewById(R.id.btnTambah);
        EditText etCari = findViewById(R.id.etCari);

        listKontak = new ArrayList<>();
        kAdapter   = new KontakAdapter(this, 0, listKontak);
        lvKontak.setAdapter(kAdapter);

        dbHelper = new DbHelper(this);
        dbKu     = dbHelper.getWritableDatabase();

        btnTambah.setOnClickListener(v -> tampilkanDialogTambah());

        lvKontak.setOnItemClickListener((parent, view, position, id) -> {
            Kontak selectedKontak = listKontak.get(position);
            tampilkanDetailKontak(selectedKontak);
        });

        if (etCari != null) {
            etCari.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    muatDataKontak(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        muatDataKontak(null);
    }

    @Override
    protected void onDestroy() {
        if (dbKu != null && dbKu.isOpen()) dbKu.close();
        if (dbHelper != null) dbHelper.close();
        super.onDestroy();
    }

    // ── Data Loader ────────────────────────────────────────────────────────────

    private void muatDataKontak(String query) {
        listKontak.clear();
        List<Kontak> data = dbHelper.getAllKontak(dbKu, query);
        listKontak.addAll(data);
        kAdapter.notifyDataSetChanged();

        int jumlah = listKontak.size();
        if (tvJumlahKontak != null) {
            tvJumlahKontak.setText(jumlah + " kontak");
        }

        boolean kosong = listKontak.isEmpty();
        layoutEmptyState.setVisibility(kosong ? View.VISIBLE : View.GONE);
        lvKontak.setVisibility(kosong ? View.GONE : View.VISIBLE);
        if (tvJumlahKontak != null) {
            tvJumlahKontak.setVisibility(kosong ? View.GONE : View.VISIBLE);
        }
    }

    // ── Dialog Tambah ──────────────────────────────────────────────────────────

    private void tampilkanDialogTambah() {
        selectedFotoUri = "";
        View viewInput = LayoutInflater.from(this).inflate(R.layout.add_kontak, null);

        final EditText etNik    = viewInput.findViewById(R.id.etNik);
        final EditText etNama   = viewInput.findViewById(R.id.etNama);
        final EditText etNoHp   = viewInput.findViewById(R.id.etNoHp);
        final EditText etAlamat = viewInput.findViewById(R.id.etAlamat);
        final ImageView imgDialogFoto  = viewInput.findViewById(R.id.imgDialogFoto);
        final View layoutPilihFoto     = viewInput.findViewById(R.id.layoutPilihFoto);

        if (layoutPilihFoto != null) {
            layoutPilihFoto.setOnClickListener(v -> {
                activeDialogFotoView = imgDialogFoto;
                imagePickerLauncher.launch("image/*");
            });
        }

        new AlertDialog.Builder(this)
                .setTitle("➕ Tambah Kontak Baru")
                .setView(viewInput)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String nikStr   = etNik.getText().toString().trim();
                    String namaStr  = etNama.getText().toString().trim();
                    String nohpStr  = etNoHp.getText().toString().trim();
                    String alamatStr = etAlamat.getText().toString().trim();

                    if (TextUtils.isEmpty(namaStr)) {
                        Toast.makeText(this, "⚠️ Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(nikStr)) {
                        Toast.makeText(this, "⚠️ NIK tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (nikStr.length() != 16) {
                        Toast.makeText(this, "⚠️ NIK harus 16 digit!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    simpanKontak(nikStr, namaStr, nohpStr, alamatStr, selectedFotoUri);
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ── Dialog Detail ──────────────────────────────────────────────────────────

    private void tampilkanDetailKontak(Kontak kontak) {
        View viewDetail = LayoutInflater.from(this).inflate(R.layout.detail_kontak, null);

        ImageView detailFoto   = viewDetail.findViewById(R.id.detailFoto);
        TextView  detailNama   = viewDetail.findViewById(R.id.detailNama);
        TextView  detailNik    = viewDetail.findViewById(R.id.detailNik);
        TextView  detailNoHp   = viewDetail.findViewById(R.id.detailNoHp);
        TextView  detailAlamat = viewDetail.findViewById(R.id.detailAlamat);
        LinearLayout rowNoHp   = viewDetail.findViewById(R.id.rowNoHp);
        View dividerAlamat     = viewDetail.findViewById(R.id.dividerAlamat);
        LinearLayout rowAlamat = viewDetail.findViewById(R.id.rowAlamat);

        // Nama
        detailNama.setText(kontak.getNama());

        // NIK (tampilkan langsung angkanya saja)
        detailNik.setText(TextUtils.isEmpty(kontak.getNik()) ? "-" : kontak.getNik());

        // No HP
        if (!TextUtils.isEmpty(kontak.getNohp())) {
            detailNoHp.setText(kontak.getNohp());
            if (rowNoHp != null) rowNoHp.setVisibility(View.VISIBLE);
        } else {
            if (rowNoHp != null) rowNoHp.setVisibility(View.GONE);
        }

        // Alamat
        if (!TextUtils.isEmpty(kontak.getAlamat())) {
            detailAlamat.setText(kontak.getAlamat());
            if (rowAlamat != null)   rowAlamat.setVisibility(View.VISIBLE);
            if (dividerAlamat != null) dividerAlamat.setVisibility(View.VISIBLE);
        } else {
            if (rowAlamat != null)   rowAlamat.setVisibility(View.GONE);
            if (dividerAlamat != null) dividerAlamat.setVisibility(View.GONE);
        }

        // Foto
        if (!TextUtils.isEmpty(kontak.getFoto())) {
            try { detailFoto.setImageURI(Uri.parse(kontak.getFoto())); }
            catch (Exception e) { detailFoto.setImageResource(R.drawable.ic_person); }
        } else {
            detailFoto.setImageResource(R.drawable.ic_person);
        }

        new AlertDialog.Builder(this)
                .setTitle("👤 Detail Kontak")
                .setView(viewDetail)
                .setPositiveButton("✏️ Edit", (dialog, which) -> {
                    dialog.dismiss();
                    tampilkanDialogEdit(kontak);
                })
                .setNeutralButton("🗑️ Hapus", (dialog, which) -> {
                    dialog.dismiss();
                    konfirmasiHapus(kontak.getNik());
                })
                .setNegativeButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ── Dialog Edit ────────────────────────────────────────────────────────────

    private void tampilkanDialogEdit(Kontak kontak) {
        selectedFotoUri = kontak.getFoto();
        View viewInput = LayoutInflater.from(this).inflate(R.layout.add_kontak, null);

        final EditText etNik    = viewInput.findViewById(R.id.etNik);
        final EditText etNama   = viewInput.findViewById(R.id.etNama);
        final EditText etNoHp   = viewInput.findViewById(R.id.etNoHp);
        final EditText etAlamat = viewInput.findViewById(R.id.etAlamat);
        final ImageView imgDialogFoto = viewInput.findViewById(R.id.imgDialogFoto);
        final View layoutPilihFoto   = viewInput.findViewById(R.id.layoutPilihFoto);

        // Pre-fill data
        etNik.setText(kontak.getNik());
        etNik.setEnabled(false); // NIK adalah primary key, tidak bisa diubah
        etNama.setText(kontak.getNama());
        etNoHp.setText(kontak.getNohp());
        etAlamat.setText(kontak.getAlamat());

        if (!TextUtils.isEmpty(selectedFotoUri)) {
            try { imgDialogFoto.setImageURI(Uri.parse(selectedFotoUri)); }
            catch (Exception ignored) {}
        }

        if (layoutPilihFoto != null) {
            layoutPilihFoto.setOnClickListener(v -> {
                activeDialogFotoView = imgDialogFoto;
                imagePickerLauncher.launch("image/*");
            });
        }

        new AlertDialog.Builder(this)
                .setTitle("✏️ Edit Kontak")
                .setView(viewInput)
                .setPositiveButton("Update", (dialog, which) -> {
                    String namaStr   = etNama.getText().toString().trim();
                    String nohpStr   = etNoHp.getText().toString().trim();
                    String alamatStr = etAlamat.getText().toString().trim();

                    if (TextUtils.isEmpty(namaStr)) {
                        Toast.makeText(this, "⚠️ Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateKontak(new Kontak(kontak.getNik(), namaStr, nohpStr, alamatStr, selectedFotoUri));
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ── CRUD Operations ────────────────────────────────────────────────────────

    private void simpanKontak(String nik, String nama, String nohp, String alamat, String foto) {
        Kontak k = new Kontak(nik, nama, nohp, alamat, foto);
        long result = dbHelper.insertKontak(dbKu, k);
        if (result != -1) {
            Toast.makeText(this, "✅ Kontak berhasil disimpan!", Toast.LENGTH_SHORT).show();
            muatDataKontak(null);
        } else {
            Toast.makeText(this, "❌ Gagal! NIK sudah terdaftar.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateKontak(Kontak k) {
        int rows = dbHelper.updateKontak(dbKu, k);
        if (rows > 0) {
            Toast.makeText(this, "✅ Kontak berhasil diupdate!", Toast.LENGTH_SHORT).show();
            muatDataKontak(null);
        } else {
            Toast.makeText(this, "❌ Gagal update kontak.", Toast.LENGTH_SHORT).show();
        }
    }

    private void konfirmasiHapus(String nik) {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Hapus Kontak")
                .setMessage("Apakah Anda yakin ingin menghapus kontak ini?\nAksi ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    dbHelper.deleteKontak(dbKu, nik);
                    Toast.makeText(this, "🗑️ Kontak berhasil dihapus.", Toast.LENGTH_SHORT).show();
                    muatDataKontak(null);
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}