package com.example.tugas_4;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class KontakAdapter extends ArrayAdapter<Kontak> {

    private static class ViewHolder {
        TextView  tvNama;
        TextView  tvNoHp;
        TextView  tvNik;
        ImageView imgFoto;
    }

    public KontakAdapter(Context context, int resource, List<Kontak> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Kontak dataKontak = getItem(position);
        ViewHolder holder;
        View rowView = convertView;

        if (rowView == null) {
            rowView = LayoutInflater.from(getContext()).inflate(R.layout.item_kontak, parent, false);
            holder           = new ViewHolder();
            holder.tvNama    = rowView.findViewById(R.id.tvNama);
            holder.tvNoHp    = rowView.findViewById(R.id.tvNoHp);
            holder.tvNik     = rowView.findViewById(R.id.tvNik);
            holder.imgFoto   = rowView.findViewById(R.id.imgFoto);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        if (dataKontak != null) {
            // Nama
            holder.tvNama.setText(dataKontak.getNama());

            // NIK
            if (holder.tvNik != null) {
                if (!TextUtils.isEmpty(dataKontak.getNik())) {
                    holder.tvNik.setText(dataKontak.getNik());
                    holder.tvNik.setVisibility(View.VISIBLE);
                } else {
                    holder.tvNik.setVisibility(View.GONE);
                }
            }

            // No HP
            if (holder.tvNoHp != null) {
                if (!TextUtils.isEmpty(dataKontak.getNohp())) {
                    holder.tvNoHp.setText(dataKontak.getNohp());
                    holder.tvNoHp.setVisibility(View.VISIBLE);
                } else {
                    holder.tvNoHp.setVisibility(View.GONE);
                }
            }

            // Foto
            if (holder.imgFoto != null) {
                if (!TextUtils.isEmpty(dataKontak.getFoto())) {
                    try {
                        holder.imgFoto.setImageURI(Uri.parse(dataKontak.getFoto()));
                    } catch (Exception e) {
                        holder.imgFoto.setImageResource(R.drawable.ic_person);
                    }
                } else {
                    holder.imgFoto.setImageResource(R.drawable.ic_person);
                }
            }
        }

        return rowView;
    }
}