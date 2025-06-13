package com.example.smd_udhaar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Borrowed extends AppCompatActivity {

    private SearchView searchView;
    private FirebaseFirestore db;
    private List<Record> borrowedList = new ArrayList<>();
    private RecyclerView recyclerView;
    private BorrowedAdapter borrowedAdapter;
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    TextView tvEmptyListNotification;

    private ActivityResultLauncher<Intent> uploadScreenshotLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String recordId = result.getData().getStringExtra("record_id");

                    for (int i = 0; i < borrowedList.size(); i++) {
                        Record record = borrowedList.get(i);
                        if (record.getRecordId().equals(recordId)) {

                            record.setStatus("Pending");
                            borrowedAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
    );

    public void setRecordStatus(){
        for (int i = 0; i < borrowedList.size(); i++) {
            Record record = borrowedList.get(i);
            if(record.getStatus().equals("Pending")){
                record.setStatus("Pending");
                borrowedAdapter.notifyItemChanged(i);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrowed);

        searchView = findViewById(R.id.searchView);
        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recordsRecyclerView);

        borrowedAdapter = new BorrowedAdapter(borrowedList, currentUserId, uploadScreenshotLauncher);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(borrowedAdapter);
        tvEmptyListNotification = findViewById(R.id.tvEmptyListNotification);
        tvEmptyListNotification.setVisibility(View.GONE);


        fetchRecords();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterRecords(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRecords(newText);
                return true;
            }
        });
    }

    private ActivityResultLauncher<Intent> viewScreenshotLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String recordId = result.getData().getStringExtra("record_id");
                    String newStatus = result.getData().getStringExtra("new_status");

                    for (int i = 0; i < borrowedList.size(); i++) {
                        Record record = borrowedList.get(i);
                        if (record.getRecordId().equals(recordId)) {
                            record.setStatus(newStatus);
                            borrowedAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
    );


    private void fetchRecords() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserName = currentUser.getDisplayName();
            if (currentUserName != null) {
//                Toast.makeText(Borrowed.this, "Fetching records for: " + currentUserName, Toast.LENGTH_SHORT).show();

                Query borrowedQuery = db.collection("Records").whereEqualTo("BorrowerName", currentUserName);

                Task<List<QuerySnapshot>> allTasks = Tasks.whenAllSuccess(borrowedQuery.get());
                allTasks.addOnSuccessListener(querySnapshots -> {
                    borrowedList.clear();
                    HashSet<Record> recordsSet = new HashSet<>();
                    for (QuerySnapshot snapshots : querySnapshots) {
                        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                            Record record = snapshot.toObject(Record.class);
                            record.setRecordId(snapshot.getId());
                            String status = snapshot.getString("Status");
                            record.setStatus(status);
                            fetchLenderPhoneNumber(record.getLenderName(), record);
                            recordsSet.add(record);
                        }
                    }
                    setRecordStatus();
                    borrowedList.addAll(recordsSet);
                    borrowedAdapter.notifyDataSetChanged();
                    updateEmptyListNotification();

                }).addOnFailureListener(e -> {
                    Toast.makeText(Borrowed.this, "Error fetching records: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } else {
                Toast.makeText(Borrowed.this, "Display name is not set in user profile", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmptyListNotification() {
        if (borrowedList.isEmpty()) {
            tvEmptyListNotification.setVisibility(View.VISIBLE);
        } else {
            tvEmptyListNotification.setVisibility(View.GONE);
        }
    }


    private void filterRecords(String query) {
        List<Record> filteredList = new ArrayList<>();
        for (Record record : borrowedList) {
            if (record.getBorrowerName().toLowerCase().contains(query.toLowerCase()) ||
                    record.getLenderName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(record);
            }
        }
        borrowedAdapter.updateList(filteredList);
        updateEmptyListNotification();
    }

    private void fetchLenderPhoneNumber(String lenderName, Record record) {
        db.collection("Users")
                .whereEqualTo("Username", lenderName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String phoneNumber = document.getString("PhoneNumber");
                        record.setLenderPhoneNumber(phoneNumber);
                        borrowedAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(Borrowed.this, "No user found for lender: " + lenderName, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Borrowed.this, "Error fetching lender phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
