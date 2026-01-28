package com.example.studentmanagerapp;

import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

public class BuddyAdapter extends RecyclerView.Adapter<BuddyAdapter.BuddyViewHolder> {

    private Cursor mCursor;
    private OnItemClickListener mListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnItemClickListener {
        void onItemClick(long id);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public BuddyAdapter(Cursor cursor) {
        mCursor = cursor;
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
        if (mCursor == null || !mCursor.moveToPosition(position)) return;

        try {
            String name = mCursor.getString(mCursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_NAME));
            String phone = mCursor.getString(mCursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_PHONE));
            long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_ID));

            holder.tvBuddyName.setText(name);
            holder.tvBuddyPhone.setText(phone);
            holder.itemView.setTag(id);

            MaterialCardView card = holder.itemView.findViewById(R.id.buddyCard);
            if (card != null) {
                if (selectedPosition == position) {
                    // Smooth Purple Highlight matching your theme
                    card.setCardBackgroundColor(Color.parseColor("#F3E8FF"));
                    card.setStrokeWidth(4);
                    card.setStrokeColor(Color.parseColor("#A855F7"));
                } else {
                    card.setCardBackgroundColor(Color.WHITE);
                    card.setStrokeWidth(2);
                    card.setStrokeColor(Color.parseColor("#EEEEEE"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return (mCursor == null) ? 0 : mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        if (mCursor != null) mCursor.close();
        mCursor = newCursor;
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public class BuddyViewHolder extends RecyclerView.ViewHolder {
        TextView tvBuddyName, tvBuddyPhone;

        public BuddyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBuddyName = itemView.findViewById(R.id.tvBuddyName);
            tvBuddyPhone = itemView.findViewById(R.id.tvBuddyPhone);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    int previousSelected = selectedPosition;
                    selectedPosition = position;
                    notifyItemChanged(previousSelected);
                    notifyItemChanged(selectedPosition);

                    if (mListener != null) {
                        mListener.onItemClick((Long) itemView.getTag());
                    }
                }
            });
        }
    }
}