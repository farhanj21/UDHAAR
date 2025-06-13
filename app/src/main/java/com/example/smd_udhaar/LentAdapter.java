package com.example.smd_udhaar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LentAdapter extends RecyclerView.Adapter<LentAdapter.RecordViewHolder> {

    private List<Record> recordList;
    private String currentUserId;
    private Activity activity;

    public LentAdapter(List<Record> recordList, String currentUserId, Activity activity) {
        this.recordList = recordList;
        this.currentUserId = currentUserId;
        this.activity = activity;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lent_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        Record record = recordList.get(position);
        holder.borrowerName.setText(record.getBorrowerName());
//        holder.lenderName.setText(record.getLenderName());
        holder.date.setText(record.getDate());
        holder.amount.setText(String.valueOf(record.getAmount()));

        String borrowerPhoneNumber = record.getBorrowerPhoneNumber();

        String recordId = record.getRecordId();
        holder.viewLent.setText("View");

        if ("Pending".equals(record.getStatus())) {
            holder.viewLent.setTextColor(Color.parseColor("#F3AA3C"));
            holder.viewLent.setEnabled(true);

            holder.viewLent.setOnClickListener(v -> {
                if (recordId != null) {
                    Intent intent = new Intent(activity, ViewScreenshotActivity.class);
                    intent.putExtra("record_id", recordId);
                    intent.putExtra("borrower_phone", borrowerPhoneNumber);
                    activity.startActivityForResult(intent, Lent.REQUEST_CODE_VIEW_SCREENSHOT);
                } else {
                    Log.e("LentAdapter", "Record ID is null for record: " + record.getBorrowerName());
                }
            });
        } else {
            holder.viewLent.setTextColor(Color.parseColor("#555555"));
            holder.viewLent.setEnabled(false);
        }
    }



    public void updateList(List<Record> newList) {
        recordList = newList;
        notifyDataSetChanged();
    }



    @Override
    public int getItemCount() {
        return recordList.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView borrowerName;
//        TextView lenderName;
        TextView amount;
        TextView date;
        TextView viewLent;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            borrowerName = itemView.findViewById(R.id.borrowerName);
//            lenderName = itemView.findViewById(R.id.lenderName);
            amount = itemView.findViewById(R.id.amount);
            date = itemView.findViewById(R.id.date);
            viewLent = itemView.findViewById(R.id.viewLent);
        }
    }
}
