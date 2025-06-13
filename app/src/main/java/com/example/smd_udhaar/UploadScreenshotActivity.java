package com.example.smd_udhaar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UploadScreenshotActivity extends AppCompatActivity {

    private String recordId;
    private String lenderPhoneNumber;
    private FirebaseFirestore db;
    private static final int SMS_PERMISSION_CODE = 1;
    private ProgressBar uploadProgressBar;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_screenshot);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);

        db = FirebaseFirestore.getInstance();

        recordId = getIntent().getStringExtra("record_id");
        lenderPhoneNumber = getIntent().getStringExtra("lender_phone");

        TextView phoneTextView = findViewById(R.id.lenderPhoneNumber);
        phoneTextView.setText(lenderPhoneNumber);

        Button uploadButton = findViewById(R.id.uploadScreenshotButton);
        uploadButton.setOnClickListener(v -> openFilePicker());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            uploadFileToFirebase(fileUri);
        }
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
    }




    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Screenshot"), PICK_IMAGE_REQUEST);
    }

    private void updateRecordWithScreenshotUrl(String screenshotUrl) {
        db.collection("Records").document(recordId)
                .update("Screenshot", screenshotUrl, "Status", "Pending")  // Also update the Status field
                .addOnSuccessListener(aVoid -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("record_id", recordId);
                    resultIntent.putExtra("new_status", "Pending");
                    setResult(RESULT_OK, resultIntent);
                    Toast.makeText(this, "Screenshot uploaded and status updated to 'Pending'.", Toast.LENGTH_SHORT).show();
                    sendSms(lenderPhoneNumber, "Hi user! A new screenshot has been added against a loan you lent. Check app to know more.");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSms(String phoneNumber, String message) {
        if (checkSmsPermission()) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(this, "SMS sent.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "SMS failed to send.", Toast.LENGTH_SHORT).show();
                Log.e("SMS", "Failed to send SMS", e);
            }
        } else {
            requestSmsPermission();
        }
    }



    private void uploadFileToFirebase(Uri fileUri) {
        uploadProgressBar.setVisibility(View.VISIBLE);
        String path = "screenshots/" + recordId + ".jpg";
        Log.d("UploadScreenshot", "Uploading to path: " + path);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference(path);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String screenshotUrl = uri.toString();
                        updateRecordWithScreenshotUrl(screenshotUrl);
                        uploadProgressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload screenshot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
