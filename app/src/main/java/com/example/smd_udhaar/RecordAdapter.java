package com.example.smd_udhaar;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    private List<com.example.smd_udhaar.Record> recordList;
    private String currentUserId;

    public RecordAdapter(List<com.example.smd_udhaar.Record> recordList, String currentUserId) {
        this.recordList = recordList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        com.example.smd_udhaar.Record record = recordList.get(position);
        holder.borrowerName.setText(record.getBorrowerName());
        holder.lenderName.setText(record.getLenderName());
        holder.date.setText(record.getDate());

        // Set the amount text color based on whether the user is a lender or borrower
//        if (record.getLenderId().equals(currentUserId)) {
//            holder.amount.setTextColor(Color.GREEN);
//        } else {
//            holder.amount.setTextColor(Color.RED);
//        }
        holder.amount.setText(String.valueOf(record.getAmount()));

        // Handle action button click
//        holder.actionButton.setOnClickListener(v -> {
//            // Add your action logic here, e.g., marking as paid or sending a reminder
//        });
    }



    static class RecordViewHolder extends RecyclerView.ViewHolder {

        TextView borrowerName;
        TextView lenderName;
        TextView amount;
        TextView date;
        TextView actionButton;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            borrowerName = itemView.findViewById(R.id.borrowerName);
            lenderName = itemView.findViewById(R.id.lenderName);
            amount = itemView.findViewById(R.id.amount);
            date = itemView.findViewById(R.id.date);
//            actionButton = itemView.findViewById(R.id.actionButton);
        }
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }
}
