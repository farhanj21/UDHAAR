package com.example.smd_udhaar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewScreenshotActivity extends AppCompatActivity {

    private String recordId;
    private ImageView screenshotImageView;
    private Button approveButton;
    private Button disapproveButton;
    private FirebaseFirestore db;
    RecordAdapter recordAdapter;
    private static final int SMS_PERMISSION_CODE = 1;
    LentAdapter lentAdapter;
    String borrowerPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_screenshot);

        db = FirebaseFirestore.getInstance();

        recordId = getIntent().getStringExtra("record_id");
        borrowerPhoneNumber = getIntent().getStringExtra("borrower_phone");

        if (recordId == null || recordId.isEmpty()) {
            Toast.makeText(this, "No record ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        screenshotImageView = findViewById(R.id.screenshotImageView);
        approveButton = findViewById(R.id.approveButton);
        disapproveButton = findViewById(R.id.disapproveButton);

        loadScreenshot();

        approveButton.setOnClickListener(v -> approveRecord());

        disapproveButton.setOnClickListener(v -> disapproveRecord());
    }

    private void loadScreenshot() {
        if (recordId != null) {
            db.collection("Records").document(recordId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String screenshotUrl = documentSnapshot.getString("Screenshot");
                        if (screenshotUrl != null) {
                            Glide.with(this).load(Uri.parse(screenshotUrl)).into(screenshotImageView);
                        } else {
                            Toast.makeText(this, "No screenshot found for this record.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ViewScreenshotActivity", "Failed to fetch record: " + e.getMessage());
                        Toast.makeText(this, "Failed to fetch record.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("ViewScreenshotActivity", "Record ID is null. Cannot load screenshot.");
        }
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }

    private void sendSMSNotification(String message) {
        if (!checkSmsPermission()) {
            requestSmsPermission();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(borrowerPhoneNumber, null, message, null, null);
            Toast.makeText(this, "Notification sent to borrower.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed to send, please try again.", Toast.LENGTH_LONG).show();
        }
    }


    private void disapproveRecord() {
        db.collection("Records").document(recordId)
                .update("Status", "Resolve", "Screenshot", "")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ViewScreenshotActivity.this, "Record disapproved and status set to Resolve.", Toast.LENGTH_SHORT).show();
                    sendSMSNotification("Hi user ! A screenshot you uploaded has been disapproved by the lender. Check app to know more.");
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("record_id", recordId);
                    resultIntent.putExtra("action", "resolve");
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewScreenshotActivity.this, "Failed to disapprove record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void approveRecord() {
        db.collection("Records").document(recordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ViewScreenshotActivity.this, "Record approved and deleted.", Toast.LENGTH_SHORT).show();
                    sendSMSNotification("Hi user ! A screenshot you uploaded has been approved by the lender. Check app to know more.");
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("record_id", recordId);
                    resultIntent.putExtra("action", "delete");
                    setResult(RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewScreenshotActivity.this, "Failed to approve record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private void notifyBorrowerAdapter() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("record_id", recordId);
        resultIntent.putExtra("new_status", "Resolve");
        setResult(RESULT_OK, resultIntent);
    }
}
