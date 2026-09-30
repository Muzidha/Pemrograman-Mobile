package com.example.tugas_4;

public class Kontak {
    private String nik;
    private String nama;
    private String nohp;
    private String alamat;
    private String foto;

    public Kontak(String nik, String nama, String nohp, String alamat, String foto) {
        this.nik = nik;
        this.nama = nama;
        this.nohp = nohp;
        this.alamat = alamat;
        this.foto = foto;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getNohp() {
        return nohp;
    }

    public void setNohp(String nohp) {
        this.nohp = nohp;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}