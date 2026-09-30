package com.example.tugas_4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "db_kontak";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_NAME = "kontak";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "nik TEXT PRIMARY KEY, " +
                "nama TEXT, " +
                "nohp TEXT, " +
                "alamat TEXT, " +
                "foto TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertKontak(SQLiteDatabase db, Kontak k) {
        ContentValues cv = new ContentValues();
        cv.put("nik", k.getNik());
        cv.put("nama", k.getNama());
        cv.put("nohp", k.getNohp());
        cv.put("alamat", k.getAlamat());
        cv.put("foto", k.getFoto());
        return db.insert(TABLE_NAME, null, cv);
    }

    public int updateKontak(SQLiteDatabase db, Kontak k) {
        ContentValues cv = new ContentValues();
        cv.put("nama", k.getNama());
        cv.put("nohp", k.getNohp());
        cv.put("alamat", k.getAlamat());
        cv.put("foto", k.getFoto());
        return db.update(TABLE_NAME, cv, "nik = ?", new String[]{k.getNik()});
    }

    public int deleteKontak(SQLiteDatabase db, String nik) {
        return db.delete(TABLE_NAME, "nik = ?", new String[]{nik});
    }

    public List<Kontak> getAllKontak(SQLiteDatabase db, String query) {
        List<Kontak> list = new ArrayList<>();
        String sql;
        String[] args = null;

        if (query == null || query.trim().isEmpty()) {
            sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY nama ASC";
        } else {
            sql = "SELECT * FROM " + TABLE_NAME + " WHERE nik LIKE ? OR nama LIKE ? OR nohp LIKE ? OR alamat LIKE ? ORDER BY nama ASC";
            String p = "%" + query.trim() + "%";
            args = new String[]{p, p, p, p};
        }

        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                int nikIdx = c.getColumnIndex("nik");
                int namaIdx = c.getColumnIndex("nama");
                int nohpIdx = c.getColumnIndex("nohp");
                int alamatIdx = c.getColumnIndex("alamat");
                int fotoIdx = c.getColumnIndex("foto");

                String nik = (nikIdx != -1) ? c.getString(nikIdx) : "";
                String nama = (namaIdx != -1) ? c.getString(namaIdx) : "";
                String nohp = (nohpIdx != -1 && !c.isNull(nohpIdx)) ? c.getString(nohpIdx) : "";
                String alamat = (alamatIdx != -1 && !c.isNull(alamatIdx)) ? c.getString(alamatIdx) : "";
                String foto = (fotoIdx != -1 && !c.isNull(fotoIdx)) ? c.getString(fotoIdx) : "";

                list.add(new Kontak(nik, nama, nohp, alamat, foto));
            }
        }
        return list;
    }
}