package com.example.smd_udhaar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

public class Lent extends AppCompatActivity {

    private SearchView searchView;
    private FirebaseFirestore db;
    private List<Record> lentList = new ArrayList<>();
    private RecyclerView recyclerView;
    private List<Record> recordList;
    private com.example.smd_udhaar.LentAdapter lentAdapter;
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    public static final int REQUEST_CODE_VIEW_SCREENSHOT = 100;
    TextView tvEmptyListNotification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lent);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recordList = (ArrayList<Record>) getIntent().getSerializableExtra("recordList");
        searchView = findViewById(R.id.searchView);
        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recordsRecyclerView);
        tvEmptyListNotification = findViewById(R.id.tvEmptyListNotification);
        tvEmptyListNotification.setVisibility(View.GONE);

        lentAdapter = new LentAdapter(lentList, currentUserId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(lentAdapter);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_VIEW_SCREENSHOT && resultCode == RESULT_OK && data != null) {
            String recordId = data.getStringExtra("record_id");
            String action = data.getStringExtra("action");

            if (action != null && recordId != null) {
                if (action.equals("delete")) {
                    for (int i = 0; i < lentList.size(); i++) {
                        if (lentList.get(i).getRecordId().equals(recordId)) {
                            lentList.remove(i);
                            lentAdapter.notifyItemRemoved(i);
                            updateEmptyListNotification();
                            break;
                        }
                    }
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("record_id", recordId);
                    resultIntent.putExtra("action", "delete");
                    setResult(RESULT_OK, resultIntent);
//                    finish();


                } else if (action.equals("resolve")) {
                    for (Record record : lentList) {
                        if (record.getRecordId().equals(recordId)) {
                            record.setStatus("Resolve");
                            lentAdapter.notifyDataSetChanged();
                            updateEmptyListNotification();
                            break;
                        }

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("record_id", recordId);
                        resultIntent.putExtra("action", "resolve");
                        setResult(RESULT_OK, resultIntent);
//                        finish();
                    }
                }
            }
        }
    }

    private void changeStatus(String recordId) {
        for(int i=0;i<lentList.size(); i++) {
            Record record = lentList.get(i);
            if (record.getRecordId().equals(recordId)){
                record.setStatus("Resolve");
//                Toast.makeText(this, "here, setting equal to resolve for amount: "+ record.getAmount(), Toast.LENGTH_SHORT).show();
                lentAdapter.notifyItemChanged(i);
            }
        }

    }


    private void fetchRecords() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserName = currentUser.getDisplayName();
            if (currentUserName != null) {
//                Toast.makeText(Lent.this, "Fetching records for: " + currentUserName, Toast.LENGTH_SHORT).show();

                Query lenderQuery = db.collection("Records").whereEqualTo("LenderName", currentUserName);

                Task<List<QuerySnapshot>> allTasks = Tasks.whenAllSuccess(lenderQuery.get());
                allTasks.addOnSuccessListener(querySnapshots -> {
                    lentList.clear();
                    HashSet<Record> recordsSet = new HashSet<>(); // Use a set to avoid duplicates
                    for (QuerySnapshot snapshots : querySnapshots) {
                        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                            Record record = snapshot.toObject(Record.class);
                            if (record != null) {
                                record.setRecordId(snapshot.getId());
                                String status = snapshot.getString("Status");
                                record.setStatus(status);
                                fetchBorrowerPhoneNumber(record.getBorrowerName(), record);
                                recordsSet.add(record);
                            }
                        }
                    }
                    setRecordStatus();
                    lentList.addAll(recordsSet);
                    lentAdapter.notifyDataSetChanged();
                    updateEmptyListNotification();
                }).addOnFailureListener(e -> {
                    Toast.makeText(Lent.this, "Error fetching records: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } else {
                Toast.makeText(Lent.this, "Display name is not set in user profile", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchBorrowerPhoneNumber(String borrowerName, Record record) {
        db.collection("Users")
                .whereEqualTo("Username", borrowerName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String phoneNumber = document.getString("PhoneNumber");
                        record.setBorrowerPhoneNumber(phoneNumber);
                        lentAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(Lent.this, "No user found for lender: " + borrowerName, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Lent.this, "Error fetching lender phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void updateEmptyListNotification() {
        if (lentList.isEmpty()) {
            tvEmptyListNotification.setVisibility(View.VISIBLE);
        } else {
            tvEmptyListNotification.setVisibility(View.GONE);
        }
    }

    private void setRecordStatus() {
        for (int i = 0; i < lentList.size(); i++) {
            Record record = lentList.get(i);
            if(record.getStatus().equals("Pending")){
                record.setStatus("Pending");  // Ensure Record class has setStatus method
                lentAdapter.notifyItemChanged(i);  // Notify adapter to refresh the UI
            }
        }
    }


    private void filterRecords(String query) {
        List<Record> filteredList = new ArrayList<>();
        for (Record record : lentList) {
            if (record.getBorrowerName().toLowerCase().contains(query.toLowerCase()) ||
                    record.getLenderName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(record);
            }
        }

        // Update the adapter with the filtered list
        lentAdapter = new LentAdapter(filteredList, currentUserId, this);
        recyclerView.setAdapter(lentAdapter);
        updateEmptyListNotification();
    }
}