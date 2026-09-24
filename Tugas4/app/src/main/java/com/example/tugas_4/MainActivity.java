package com.example.tugas_4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText nrp, nama;
    private SQLiteDatabase dbku;
    private DatabaseHelper openDb;

    private RecyclerView rvMahasiswa;
    private TextView tvEmptyState, tvHeaderDaftar;
    private MaterialButton btnResetFilter;
    private ScrollView scrollView;

    private final List<Mahasiswa> mahasiswaList = new ArrayList<>();
    private MahasiswaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nrp = findViewById(R.id.nrp);
        nama = findViewById(R.id.nama);

        scrollView = findViewById(R.id.scrollView);
        rvMahasiswa = findViewById(R.id.rvMahasiswa);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvHeaderDaftar = findViewById(R.id.tvHeaderDaftar);
        btnResetFilter = findViewById(R.id.btnResetFilter);

        findViewById(R.id.btnSimpan).setOnClickListener(v -> simpan());
        findViewById(R.id.btnCari).setOnClickListener(v -> cari());
        findViewById(R.id.btnUpdate).setOnClickListener(v -> update());
        findViewById(R.id.btnHapus).setOnClickListener(v -> hapus());

        if (findViewById(R.id.btnClear) != null) {
            findViewById(R.id.btnClear).setOnClickListener(v -> resetFields());
        }

        btnResetFilter.setOnClickListener(v -> {
            resetFields();
            loadDataMahasiswa(null);
        });

        openDb = new DatabaseHelper(this);
        dbku = openDb.getWritableDatabase();

        setupRecyclerView();
        loadDataMahasiswa(null);
    }

    private void setupRecyclerView() {
        rvMahasiswa.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MahasiswaAdapter(mahasiswaList, item -> {
            nrp.setText(item.getNrp());
            nama.setText(item.getNama());
            if (scrollView != null) {
                scrollView.smoothScrollTo(0, 0);
            }
        });
        rvMahasiswa.setAdapter(adapter);
    }

    private void loadDataMahasiswa(String searchQuery) {
        mahasiswaList.clear();

        try (Cursor cursor = TextUtils.isEmpty(searchQuery)
                ? dbku.rawQuery("SELECT nrp, nama FROM mhs ORDER BY nrp ASC", null)
                : dbku.rawQuery("SELECT nrp, nama FROM mhs WHERE nrp LIKE ? OR nama LIKE ? ORDER BY nrp ASC",
                new String[]{"%" + searchQuery.trim() + "%", "%" + searchQuery.trim() + "%"})) {

            if (TextUtils.isEmpty(searchQuery)) {
                btnResetFilter.setVisibility(View.GONE);
            } else {
                btnResetFilter.setVisibility(View.VISIBLE);
            }

            while (cursor.moveToNext()) {
                int nrpIdx = cursor.getColumnIndex("nrp");
                int namaIdx = cursor.getColumnIndex("nama");
                if (nrpIdx != -1 && namaIdx != -1) {
                    String nrpVal = cursor.getString(nrpIdx);
                    String namaVal = cursor.getString(namaIdx);
                    mahasiswaList.add(new Mahasiswa(nrpVal, namaVal));
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (mahasiswaList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvMahasiswa.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(searchQuery)) {
                tvEmptyState.setText(getString(R.string.empty_search_data, searchQuery));
            } else {
                tvEmptyState.setText(getString(R.string.empty_data_mahasiswa));
            }
            tvHeaderDaftar.setText(getString(R.string.title_daftar_mahasiswa));
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvMahasiswa.setVisibility(View.VISIBLE);
            tvHeaderDaftar.setText(getString(R.string.title_daftar_mahasiswa_count, mahasiswaList.size()));
        }
    }

    @Override
    protected void onDestroy() {
        if (dbku != null && dbku.isOpen()) {
            dbku.close();
        }
        if (openDb != null) {
            openDb.close();
        }
        super.onDestroy();
    }

    private void simpan() {
        String nrpText = nrp.getText().toString().trim();
        String namaText = nama.getText().toString().trim();

        if (TextUtils.isEmpty(nrpText) || TextUtils.isEmpty(namaText)) {
            Toast.makeText(this, "NRP dan Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNrpExist(nrpText)) {
            Toast.makeText(this, "NRP sudah terdaftar! Gunakan Update untuk mengubah.", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues data = new ContentValues();
        data.put("nrp", nrpText);
        data.put("nama", namaText);

        long result = dbku.insert("mhs", null, data);
        if (result != -1) {
            Toast.makeText(this, "Data Tersimpan", Toast.LENGTH_LONG).show();
            resetFields();
            loadDataMahasiswa(null);
        } else {
            Toast.makeText(this, "Gagal Menyimpan Data", Toast.LENGTH_LONG).show();
        }
    }

    private void cari() {
        String nrpText = nrp.getText().toString().trim();
        String namaText = nama.getText().toString().trim();

        String query = !TextUtils.isEmpty(nrpText) ? nrpText : namaText;

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Masukkan NRP atau Nama untuk mencari!", Toast.LENGTH_SHORT).show();
            loadDataMahasiswa(null);
            return;
        }

        loadDataMahasiswa(query);

        if (!mahasiswaList.isEmpty()) {
            Mahasiswa firstMatch = mahasiswaList.get(0);
            nrp.setText(firstMatch.getNrp());
            nama.setText(firstMatch.getNama());
            Toast.makeText(this, "Ditemukan " + mahasiswaList.size() + " data", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Data Tidak Ditemukan", Toast.LENGTH_SHORT).show();
        }
    }

    private void update() {
        String nrpText = nrp.getText().toString().trim();
        String namaText = nama.getText().toString().trim();

        if (TextUtils.isEmpty(nrpText) || TextUtils.isEmpty(namaText)) {
            Toast.makeText(this, "NRP dan Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues data = new ContentValues();
        data.put("nama", namaText);

        int rows = dbku.update("mhs", data, "nrp = ?", new String[]{nrpText});
        if (rows > 0) {
            Toast.makeText(this, "Data Terupdate", Toast.LENGTH_LONG).show();
            loadDataMahasiswa(null);
        } else {
            Toast.makeText(this, "Data Tidak Ditemukan / Gagal Update", Toast.LENGTH_LONG).show();
        }
    }

    private void hapus() {
        String nrpText = nrp.getText().toString().trim();

        if (TextUtils.isEmpty(nrpText)) {
            Toast.makeText(this, "Masukkan NRP untuk menghapus!", Toast.LENGTH_SHORT).show();
            return;
        }

        int rows = dbku.delete("mhs", "nrp = ?", new String[]{nrpText});
        if (rows > 0) {
            Toast.makeText(this, "Data Terhapus", Toast.LENGTH_LONG).show();
            resetFields();
            loadDataMahasiswa(null);
        } else {
            Toast.makeText(this, "Data Tidak Ditemukan", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNrpExist(String nrpText) {
        try (Cursor cursor = dbku.rawQuery("SELECT 1 FROM mhs WHERE nrp = ?", new String[]{nrpText})) {
            return cursor.getCount() > 0;
        }
    }

    private void resetFields() {
        nrp.setText("");
        nama.setText("");
        nrp.requestFocus();
        loadDataMahasiswa(null);
    }

    public static class Mahasiswa {
        private final String nrp;
        private final String nama;

        public Mahasiswa(String nrp, String nama) {
            this.nrp = nrp;
            this.nama = nama;
        }

        public String getNrp() {
            return nrp;
        }

        public String getNama() {
            return nama;
        }
    }

    public static class MahasiswaAdapter extends RecyclerView.Adapter<MahasiswaAdapter.ViewHolder> {

        public interface OnItemClickListener {
            void onItemClick(Mahasiswa item);
        }

        private final List<Mahasiswa> list;
        private final OnItemClickListener listener;

        public MahasiswaAdapter(List<Mahasiswa> list, OnItemClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mahasiswa, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Mahasiswa item = list.get(position);
            Context context = holder.itemView.getContext();
            holder.tvNama.setText(item.getNama());
            holder.tvNrp.setText(context.getString(R.string.label_nrp, item.getNrp()));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNama, tvNrp;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNama = itemView.findViewById(R.id.tvNama);
                tvNrp = itemView.findViewById(R.id.tvNrp);
            }
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "db_mahasiswa";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS mhs (nrp TEXT PRIMARY KEY, nama TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS mhs");
            onCreate(db);
        }
    }
}