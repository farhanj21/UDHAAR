package com.example.smd_udhaar;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BorrowedAdapter extends RecyclerView.Adapter<BorrowedAdapter.RecordViewHolder> {

    private List<Record> recordList;
    private String currentUserId;
    private final ActivityResultLauncher<Intent> uploadScreenshotLauncher;

    public BorrowedAdapter(List<Record> recordList, String currentUserId, ActivityResultLauncher<Intent> uploadScreenshotLauncher) {
        this.recordList = recordList;
        this.currentUserId = currentUserId;
        this.uploadScreenshotLauncher = uploadScreenshotLauncher;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.borrowed_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        Record record = recordList.get(position);
//        holder.borrowerName.setText(record.getBorrowerName());
        holder.lenderName.setText(record.getLenderName());
        holder.date.setText(record.getDate());
        holder.amount.setText(String.valueOf(record.getAmount()));
        holder.btnResolve.setText(record.getStatus());

        holder.btnResolve.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), UploadScreenshotActivity.class);
            intent.putExtra("record_id", record.getRecordId());
            intent.putExtra("lender_phone", record.getLenderPhoneNumber());

            uploadScreenshotLauncher.launch(intent);
        });

        if ("Pending".equals(record.getStatus())) {
            holder.btnResolve.setText("Pending");
            holder.btnResolve.setTextColor(Color.parseColor("#3CB371"));
            holder.btnResolve.setEnabled(false);
        } else {
            holder.btnResolve.setText("Resolve");
            holder.btnResolve.setEnabled(true);
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

//        TextView borrowerName;
        TextView lenderName;
        TextView amount;
        TextView date;
        TextView btnResolve;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
//            borrowerName = itemView.findViewById(R.id.borrowerName);
            lenderName = itemView.findViewById(R.id.lenderName);
            amount = itemView.findViewById(R.id.amount);
            date = itemView.findViewById(R.id.date);
            btnResolve = itemView.findViewById(R.id.btnResolve);
        }
    }
}
