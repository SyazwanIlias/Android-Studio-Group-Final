package com.example.studentmanagerapp;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

public class BuddyAdapter extends RecyclerView.Adapter<BuddyAdapter.BuddyViewHolder> {

    private Cursor cursor;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(long id);
    }

    public BuddyAdapter(Cursor cursor) {
        this.cursor = cursor;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BuddyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.buddy_item, parent, false);
        return new BuddyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BuddyViewHolder holder, int position) {
        if (cursor != null && cursor.moveToPosition(position)) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_NAME));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_PHONE));

            // Get photo
            int photoIndex = cursor.getColumnIndex(DatabaseHelper.COL_BUDDY_PHOTO);
            String photoBase64 = (photoIndex != -1) ? cursor.getString(photoIndex) : null;

            holder.tvName.setText(name);
            holder.tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone");

            // Set photo or default icon
            if (photoBase64 != null && !photoBase64.trim().isEmpty()) {
                try {
                    Bitmap bitmap = base64ToBitmap(photoBase64);
                    if (bitmap != null) {
                        holder.ivBuddyIcon.setImageBitmap(bitmap);
                        holder.ivBuddyIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        holder.ivBuddyIcon.setPadding(0, 0, 0, 0);
                    } else {
                        setDefaultIcon(holder.ivBuddyIcon);
                    }
                } catch (Exception e) {
                    // If photo fails to load, show default icon
                    setDefaultIcon(holder.ivBuddyIcon);
                }
            } else {
                setDefaultIcon(holder.ivBuddyIcon);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(id);
                }
            });
        }
    }

    private void setDefaultIcon(ImageView imageView) {
        imageView.setImageResource(android.R.drawable.ic_menu_myplaces);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        int padding = (int) (14 * imageView.getContext().getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
    }

    private Bitmap base64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    static class BuddyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone;
        ImageView ivBuddyIcon;

        BuddyViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvBuddyName);
            tvPhone = itemView.findViewById(R.id.tvBuddyPhone);
            ivBuddyIcon = itemView.findViewById(R.id.ivBuddyIcon);
        }
    }
}