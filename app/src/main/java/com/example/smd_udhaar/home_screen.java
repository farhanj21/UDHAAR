package com.example.smd_udhaar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class home_screen extends AppCompatActivity {

        private Record currentEntry;
        private static final int SMS_PERMISSION_CODE = 1;
        private FirebaseFirestore db;
        public static RecordAdapter recordAdapter;
        private List<Record> recordList = new ArrayList<>();
        private RecyclerView recyclerView;
        private SearchView searchView;
        private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FloatingActionButton btnStartAddNew;
        private Button btnLent;
        private Button btnBorrowed;
        private Button btnLogout;
        FirebaseAuth auth;
        private FirebaseUser user;
        TextView emptyListNotification;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_home_screen);

            db = FirebaseFirestore.getInstance();
            recyclerView = findViewById(R.id.recordsRecyclerView);
            searchView = findViewById(R.id.searchView);
            btnStartAddNew = findViewById(R.id.btnStartAddNew);
            btnLent=findViewById(R.id.btnLent);
            btnBorrowed=findViewById(R.id.btnBorrowed);
            auth = FirebaseAuth.getInstance();
            user= auth.getCurrentUser();
            btnLogout = findViewById(R.id.btnLogout);
            emptyListNotification = findViewById(R.id.tvEmptyListNotification);
            emptyListNotification.setVisibility(View.GONE);
            user.reload();

            if(user==null){
                startActivity(new Intent(home_screen.this, login.class));
                finish();
            }

            recordAdapter = new RecordAdapter(recordList, currentUserId);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(recordAdapter);

            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    auth.signOut();
                    startActivity(new Intent(home_screen.this, login.class));
                    finish();
                }
            });

            fetchRecords();

            checkAndSendLoanReminders();

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

            btnLent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(home_screen.this, Lent.class);
                    i.putExtra("recordList", (ArrayList<Record>) recordList);
                    startActivityForResult(i, Lent.REQUEST_CODE_VIEW_SCREENSHOT);  // Use startActivityForResult
                }
            });


            btnBorrowed.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i =new Intent(home_screen.this, Borrowed.class);
                    startActivity(i);
                }
            });

            btnStartAddNew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(home_screen.this);
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View customLayout = inflater.inflate(R.layout.add_new_loan, null, false);


                    builder.setView(customLayout);

                    Dialog dialog = builder.create();
                    dialog.show();

                    TextInputEditText etBorrowerName = customLayout.findViewById(R.id.etBorrowerName);
                    TextInputEditText etLenderName = customLayout.findViewById(R.id.etLenderName);
                    TextInputEditText etAmount = customLayout.findViewById(R.id.etAmount);
                    TextInputEditText etDate = customLayout.findViewById(R.id.etDate);

                    etDate.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showDatePickerDialog(etDate);
                        }
                    });

                    Button btnCancel = customLayout.findViewById(R.id.btnCancel);
                    Button btnAdd = customLayout.findViewById(R.id.btnAdd);

                    btnAdd.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String borrower = etBorrowerName.getText().toString().trim();
                            String lender = etLenderName.getText().toString().trim();
                            String amountText = etAmount.getText().toString().trim();
//                            Integer amount = Integer.parseInt(amountText);
                            String date = etDate.getText().toString().trim();
                            if(borrower.isEmpty()){
                                Toast.makeText(home_screen.this,"Enter Borrower Name", Toast.LENGTH_SHORT).show();
                            }
                            else if(lender.isEmpty()){
                                Toast.makeText(home_screen.this,"Enter Lender Name", Toast.LENGTH_SHORT).show();
                            }
                            else if(amountText.isEmpty()){
                                Toast.makeText(home_screen.this,"Enter Amount", Toast.LENGTH_SHORT).show();
                            }
                            else if(date.isEmpty()){
                                Toast.makeText(home_screen.this,"Enter Date", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Integer amount = 0;
                                try {
                                    amount = Integer.parseInt(amountText);
                                } catch (NumberFormatException e) {
                                    Toast.makeText(home_screen.this,"Invalid amount entered. Please enter a numeric value.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Record newRecord = new Record(borrower, lender, amount, date, "Resolve");
                                addRecordToDatabase(newRecord);
                                updateEmptyListNotification();
                                dialog.dismiss();
                            }
                        }
                    });
                    btnCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(home_screen.this, "New Loan Entry Cancelled", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
                }
            });
        }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Lent.REQUEST_CODE_VIEW_SCREENSHOT && resultCode == RESULT_OK && data != null) {
            String recordId = data.getStringExtra("record_id");
            String action = data.getStringExtra("action");
//            Log.d("return",recordId);
//            Log.d("return2",action);
            if (action != null && recordId != null) {
                if (action.equals("delete")) {
                    // Remove the record from recordList in home_screen
                    for (int i = 0; i < recordList.size(); i++) {
                        if (recordList.get(i).getRecordId().equals(recordId)) {
//                            Toast.makeText(home_screen.this,"deleting "+recordList.get(i).getRecordId(),Toast.LENGTH_SHORT).show();
                            recordList.remove(i);
                            recordAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    updateEmptyListNotification();
                } else if (action.equals("resolve")) {
//                    Toast.makeText(home_screen.this,"here in two",Toast.LENGTH_SHORT).show();
                    for (Record record : recordList) {
                        if (record.getRecordId().equals(recordId)) {
                            record.setStatus("Resolve");
                            recordAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
        }
    }


    private void showDatePickerDialog(final TextInputEditText etDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Show DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(home_screen.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // Adjust monthOfYear to be 1-based, since it is 0-based by default
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        // Format the date as yyyy-MM-dd
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String formattedDate = sdf.format(calendar.getTime());
                        // Set the selected date in the TextInputEditText
                        etDate.setText(formattedDate);
                    }
                }, year, month, day);

        datePickerDialog.show();
    }

    private void addRecordToDatabase(Record entry) {
        Map<String, Object> newRecord = new HashMap<>();
        newRecord.put("BorrowerName", entry.getBorrowerName());
        newRecord.put("LenderName", entry.getLenderName());
        newRecord.put("Amount", entry.getAmount());
        newRecord.put("Date", entry.getDate());
        newRecord.put("Status", entry.getStatus());
        newRecord.put("Screenshot", "");

        db.collection("Records").add(newRecord)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(home_screen.this, "Record added successfully", Toast.LENGTH_SHORT).show();
                    recordList.add(entry);
                    recordAdapter.notifyItemInserted(recordList.size() - 1);
                    recyclerView.scrollToPosition(recordList.size() - 1);
                    currentEntry=entry;
                    Log.d("UpdateList", "Record added and list updated");

                    updateEmptyListNotification();  // Call after the list is updated
                    Log.d("UpdateList", "Empty list notification updated. List size: " + recordList.size());
                    if (checkSmsPermission()) {
                        getBorrowerPhoneNumber(entry.getBorrowerName(), new OnPhoneNumberFetchedListener() {
                            @Override
                            public void onPhoneNumberFetched(String phoneNumber) {
                                sendSms(phoneNumber, entry.getBorrowerName(), entry.getAmount());
                            }
                        });
                    } else {
                        requestSmsPermission();
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(home_screen.this, "Error adding record: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();

                getBorrowerPhoneNumber(currentEntry.getBorrowerName(), new OnPhoneNumberFetchedListener() {
                    @Override
                    public void onPhoneNumberFetched(String phoneNumber) {
                        sendSms(phoneNumber, currentEntry.getBorrowerName(), currentEntry.getAmount());
                    }
                });
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void sendSms(String phoneNumber, String borrowerName, int amount) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            String message = "Hello " + borrowerName + ", you have borrowed an amount of " + amount + ". Please return it as per agreement.";

            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(home_screen.this, "SMS sent to " + borrowerName, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(home_screen.this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(home_screen.this, "Borrower's phone number not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndSendLoanReminders() {
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (currentUserName != null) {
            Query lenderQuery = db.collection("Records")
                    .whereEqualTo("LenderName", currentUserName);
            lenderQuery.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        Record record = document.toObject(Record.class);
                        if (isLoanOverdue(record.getDate())) {
                            getBorrowerPhoneNumber(record.getBorrowerName(), phoneNumber -> {
                                sendReminderSms(phoneNumber, record.getBorrowerName(), record.getLenderName() , record.getAmount());
                            });
                        }
                    }
                } else {
                    Toast.makeText(home_screen.this, "Error fetching records: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isLoanOverdue(String loanDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = sdf.parse(loanDate);
            long diff = new Date().getTime() - date.getTime();
            long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            return days > 7;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendReminderSms(String phoneNumber, String borrowerName, String lenderName, int amount) {
        String message = "Hello " + borrowerName + ", it has been more than 7 days since you borrowed " + amount + " from "+lenderName+". Please repay it as soon as possible.";
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } catch (Exception e) {
                Toast.makeText(home_screen.this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(home_screen.this, "Borrower's phone number not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void getBorrowerPhoneNumber(String borrowerName, OnPhoneNumberFetchedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .whereEqualTo("Username", borrowerName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String phoneNumber = document.getString("PhoneNumber");

                        if (phoneNumber != null) {
                            listener.onPhoneNumberFetched(phoneNumber);
                        } else {
                            Toast.makeText(home_screen.this, "Phone number not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(home_screen.this, "Error fetching phone number", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public interface OnPhoneNumberFetchedListener {
        void onPhoneNumberFetched(String phoneNumber);
    }

        private void fetchRecords() {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String currentUserName = currentUser.getDisplayName();
                if (currentUserName != null) {
//                    Toast.makeText(home_screen.this, "Fetching records for: " + currentUserName, Toast.LENGTH_SHORT).show();

                    Query borrowerQuery = db.collection("Records").whereEqualTo("BorrowerName", currentUserName);
                    Query lenderQuery = db.collection("Records").whereEqualTo("LenderName", currentUserName);
                    ProgressDialog progressDialog = new ProgressDialog(home_screen.this);
                    progressDialog.show();
                    Task<List<QuerySnapshot>> allTasks = Tasks.whenAllSuccess(borrowerQuery.get(), lenderQuery.get());
                    allTasks.addOnSuccessListener(querySnapshots -> {
                        recordList.clear();
                        HashSet<Record> recordsSet = new HashSet<>();
                        for (QuerySnapshot snapshots : querySnapshots) {
                            for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                                Record record = snapshot.toObject(Record.class);
                                record.setRecordId(snapshot.getId());
                                recordsSet.add(record);
                            }
                        }
                        recordList.addAll(recordsSet);
                        recordAdapter.notifyDataSetChanged();
                        updateEmptyListNotification();
                        progressDialog.dismiss();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(home_screen.this, "Error fetching records: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    });
                } else {
                    Toast.makeText(home_screen.this, "Display name is not set in user profile", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            }
        }

    private void updateEmptyListNotification() {
        if (recordList.isEmpty()) {
            emptyListNotification.setVisibility(View.VISIBLE);
        } else {
            emptyListNotification.setVisibility(View.GONE);
        }
    }

    private void filterRecords(String query) {
            List<Record> filteredList = new ArrayList<>();
            for (Record record : recordList) {
                if (record.getBorrowerName().toLowerCase().contains(query.toLowerCase()) ||
                        record.getLenderName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(record);
                }
            }
            recordAdapter = new RecordAdapter(filteredList, currentUserId);
            recyclerView.setAdapter(recordAdapter);
            updateEmptyListNotification();
        }


    }

