package com.example.tugassensor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Gambar kompas dengan Canvas.
 * Piringan berputar mengikuti arah HP sehingga huruf U selalu menunjuk utara,
 * dan jarum + ikon Ka'bah selalu menunjuk arah kiblat.
 * Segitiga di atas piringan = arah depan HP.
 */
public class KompasView extends View {

    // ===== State =====
    private float arahHp;          // derajat dari utara
    private float arahKiblat;      // derajat dari utara
    private boolean menghadapKiblat;

    // ===== Alat gambar =====
    private final float dp;
    private final float ukuranTeksBesar;
    private final float ukuranTeksKecil;
    private final String[] labelMataAngin; // U, T, S, B

    private final int warnaKartu;
    private final int warnaGaris;
    private final int warnaTeks;
    private final int warnaTeksKedua;
    private final int warnaUtara;
    private final int warnaAksen;
    private final int warnaSukses;

    private final Paint paintIsi = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGaris = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTeks = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF kotak = new RectF();

    public KompasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        ukuranTeksBesar = sp(18);
        ukuranTeksKecil = sp(11);
        labelMataAngin = getResources().getStringArray(R.array.label_kompas);

        // Warna diambil dari resource, jadi otomatis ikut Mode Terang / Mode Gelap
        warnaKartu = ContextCompat.getColor(context, R.color.kartu);
        warnaGaris = ContextCompat.getColor(context, R.color.garis);
        warnaTeks = ContextCompat.getColor(context, R.color.teks_utama);
        warnaTeksKedua = ContextCompat.getColor(context, R.color.teks_kedua);
        warnaUtara = ContextCompat.getColor(context, R.color.utara);
        warnaAksen = ContextCompat.getColor(context, R.color.aksen);
        warnaSukses = ContextCompat.getColor(context, R.color.sukses);

        paintGaris.setStyle(Paint.Style.STROKE);
        paintGaris.setStrokeCap(Paint.Cap.ROUND);
        paintTeks.setTextAlign(Paint.Align.CENTER);
        paintTeks.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setArah(float arahHp, float arahKiblat) {
        this.arahHp = arahHp;
        this.arahKiblat = arahKiblat;
        invalidate();
    }

    public void setMenghadapKiblat(boolean menghadapKiblat) {
        if (this.menghadapKiblat != menghadapKiblat) {
            this.menghadapKiblat = menghadapKiblat;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Kompas selalu persegi, mengikuti lebar yang tersedia
        int lebar = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(lebar, lebar);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - 16 * dp; // sisakan ruang untuk penunjuk di atas
        int warnaStatus = menghadapKiblat ? warnaSukses : warnaAksen;

        // 1. Piringan kompas (tepinya hijau saat sudah menghadap kiblat)
        paintIsi.setColor(warnaKartu);
        canvas.drawCircle(cx, cy, r, paintIsi);
        paintGaris.setColor(menghadapKiblat ? warnaSukses : warnaGaris);
        paintGaris.setStrokeWidth((menghadapKiblat ? 4 : 2) * dp);
        canvas.drawCircle(cx, cy, r, paintGaris);

        // 2. Garis skala tiap 5 derajat, diputar berlawanan arah HP
        canvas.save();
        canvas.rotate(-arahHp, cx, cy);
        float awalSkala = cy - r + 8 * dp;
        for (int d = 0; d < 360; d += 5) {
            boolean besar = d % 30 == 0;
            paintGaris.setColor(besar ? warnaTeks : warnaTeksKedua);
            paintGaris.setStrokeWidth((besar ? 2f : 1f) * dp);
            canvas.drawLine(cx, awalSkala, cx, awalSkala + (besar ? 14 : 7) * dp, paintGaris);
            canvas.rotate(5, cx, cy);
        }
        canvas.restore();

        // 3. Label U/T/S/B dan angka derajat, digambar tegak supaya mudah dibaca
        float rLabel = r - 40 * dp;
        for (int d = 0; d < 360; d += 30) {
            double sudut = Math.toRadians(d - arahHp);
            float x = (float) (cx + rLabel * Math.sin(sudut));
            float y = (float) (cy - rLabel * Math.cos(sudut));
            boolean mataAngin = d % 90 == 0;
            paintTeks.setTextSize(mataAngin ? ukuranTeksBesar : ukuranTeksKecil);
            paintTeks.setColor(d == 0 ? warnaUtara : mataAngin ? warnaTeks : warnaTeksKedua);
            String teks = mataAngin ? labelMataAngin[d / 90] : String.valueOf(d);
            // Geser setengah tinggi huruf agar teks tepat di tengah titik (x, y)
            canvas.drawText(teks, x, y - (paintTeks.descent() + paintTeks.ascent()) / 2, paintTeks);
        }

        // 4. Jarum kiblat + ikon Ka'bah, diputar ke arah kiblat relatif terhadap HP
        canvas.save();
        canvas.rotate(arahKiblat - arahHp, cx, cy);
        float lebarJarum = 9 * dp;
        float ujungJarum = cy - r + 82 * dp;

        path.reset();
        path.moveTo(cx, ujungJarum);
        path.lineTo(cx + lebarJarum, cy);
        path.lineTo(cx - lebarJarum, cy);
        path.close();
        paintIsi.setColor(warnaStatus);
        canvas.drawPath(path, paintIsi);

        path.reset();
        path.moveTo(cx + lebarJarum, cy);
        path.lineTo(cx, cy + 28 * dp);
        path.lineTo(cx - lebarJarum, cy);
        path.close();
        paintIsi.setColor(warnaGaris);
        canvas.drawPath(path, paintIsi);

        gambarKabah(canvas, cx, cy - r + 66 * dp, 11 * dp);
        canvas.restore();

        // 5. Titik tengah
        paintIsi.setColor(warnaTeks);
        canvas.drawCircle(cx, cy, 6 * dp, paintIsi);
        paintIsi.setColor(warnaKartu);
        canvas.drawCircle(cx, cy, 2.5f * dp, paintIsi);

        // 6. Penunjuk arah depan HP (tetap di atas, tidak ikut berputar)
        float atas = cy - r - 12 * dp;
        path.reset();
        path.moveTo(cx - 9 * dp, atas);
        path.lineTo(cx + 9 * dp, atas);
        path.lineTo(cx, atas + 16 * dp);
        path.close();
        paintIsi.setColor(menghadapKiblat ? warnaSukses : warnaTeks);
        canvas.drawPath(path, paintIsi);
    }

    /** Ka'bah sederhana: kotak hitam dengan sabuk (kiswah) emas. */
    private void gambarKabah(Canvas canvas, float x, float y, float s) {
        kotak.set(x - s, y - s, x + s, y + s);
        paintIsi.setColor(0xFF1C1C1C);
        canvas.drawRoundRect(kotak, 2 * dp, 2 * dp, paintIsi);

        paintIsi.setColor(0xFFD4AF37);
        canvas.drawRect(x - s, y - s * 0.5f, x + s, y - s * 0.2f, paintIsi);

        paintGaris.setColor(0xFFD4AF37);
        paintGaris.setStrokeWidth(1 * dp);
        canvas.drawRoundRect(kotak, 2 * dp, 2 * dp, paintGaris);
    }

    private float sp(float nilai) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, nilai,
                getResources().getDisplayMetrics());
    }
}
