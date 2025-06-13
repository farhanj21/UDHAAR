package com.example.smd_udhaar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class signup extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText usernameEditText;
    private EditText phoneNumberEditText;
    private TextView tvAlreadyAccount;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        usernameEditText = findViewById(R.id.username);
        phoneNumberEditText = findViewById(R.id.mobile_number);
        tvAlreadyAccount = findViewById(R.id.tvAlreadyAccount);
        progressBar = findViewById(R.id.progressBar);

        Button signupButton = findViewById(R.id.btnSignUp);
        signupButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String phoneNumber = phoneNumberEditText.getText().toString().trim();

            if (validateInput(email, password, username, phoneNumber)) {
                showProgressBar();
                checkDuplicateUsername(username, isDuplicateUsername -> {
                    if (isDuplicateUsername) {
                        hideProgressBar();
                        Toast.makeText(signup.this, "Username already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    checkDuplicateEmail(email, isDuplicateEmail -> {
                        if (isDuplicateEmail) {
                            hideProgressBar();
                            Toast.makeText(signup.this, "Email already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        checkDuplicateNumber(phoneNumber, isDuplicateNumber -> {
                            if (isDuplicateNumber) {
                                hideProgressBar();
                                Toast.makeText(signup.this, "Phone number already exists", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            signupUser(email, password, username, phoneNumber);
                        });
                    });
                });
            }
        });

        tvAlreadyAccount.setOnClickListener(view -> {
            Intent i = new Intent(signup.this, login.class);
//            i.putExtra("username")
            startActivity(i);
            finish();
        });
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }


    private void checkDuplicateEmail(String email, OnCheckCompleteListener listener) {
        Query emailQuery = db.collection("Users").whereEqualTo("Email", email);
        emailQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean isDuplicate = !queryDocumentSnapshots.isEmpty();
            listener.onCheckComplete(isDuplicate);
        }).addOnFailureListener(e -> {
            Toast.makeText(signup.this, "Error checking email: " + e.getMessage(), Toast.LENGTH_LONG).show();
            listener.onCheckComplete(false);
        });
    }

    private void checkDuplicateUsername(String username, OnCheckCompleteListener listener) {
        Query usernameQuery = db.collection("Users").whereEqualTo("Username", username);
        usernameQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean isDuplicate = !queryDocumentSnapshots.isEmpty();
            listener.onCheckComplete(isDuplicate);
        }).addOnFailureListener(e -> {
            Toast.makeText(signup.this, "Error checking username: " + e.getMessage(), Toast.LENGTH_LONG).show();
            listener.onCheckComplete(false);
        });
    }



    private void checkDuplicateNumber(String phoneNumber, OnCheckCompleteListener listener) {
        Query phoneNumberQuery = db.collection("Users").whereEqualTo("PhoneNumber", phoneNumber);
        phoneNumberQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean isDuplicate = !queryDocumentSnapshots.isEmpty();
            listener.onCheckComplete(isDuplicate);
        }).addOnFailureListener(e -> {
            Toast.makeText(signup.this, "Error checking phone number: " + e.getMessage(), Toast.LENGTH_LONG).show();
            listener.onCheckComplete(false);
        });
    }

    public interface OnCheckCompleteListener {
        void onCheckComplete(boolean isDuplicate);
    }

    private void signupUser(String email, String password, final String username, String phoneNumber) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(signup.this, "Signup Successful!", Toast.LENGTH_SHORT).show();

                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            saveUserData(firebaseUser.getUid(), email, username, phoneNumber, password);
                                        } else {
                                            hideProgressBar();
                                            Toast.makeText(signup.this, "Failed to set user profile.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        hideProgressBar();
                        Toast.makeText(signup.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String uid, String email, String username, String phoneNumber, String password) {
        Map<String, Object> user = new HashMap<>();
        user.put("Email", email);
        user.put("Username", username);
        user.put("PhoneNumber", phoneNumber);

        db.collection("Users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    hideProgressBar();
                    Toast.makeText(signup.this, "User Profile Created", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("email", email);
                    intent.putExtra("password",password);
                    setResult(RESULT_OK, intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressBar();
                    Toast.makeText(signup.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput(String email, String password, String username, String phoneNumber) {
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return false;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            return false;
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumberEditText.setError("Phone number is required");
            return false;
        }
        return true;
    }
}
