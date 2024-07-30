package com.example.tripper;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Temporary sign out to clear any existing authentication state
           mAuth.signOut();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, redirect to MatchingActivity
            Log.d(TAG, "User is signed in: " + currentUser.getUid());
            checkUserProfile(currentUser.getUid());
        } else {
            // No user is signed in, redirect to LoginActivity
            Log.d(TAG, "No user is signed in, redirecting to LoginActivity");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void checkUserProfile(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "User profile exists, redirecting to MatchingActivity");
                            startActivity(new Intent(MainActivity.this, MatchingActivity.class));
                        } else {
                            Log.d(TAG, "User profile does not exist, redirecting to BioActivity");
                            startActivity(new Intent(MainActivity.this, BioActivity.class));
                        }
                    } else {
                        Log.d(TAG, "Error checking user profile: ", task.getException());
                        // Handle the error appropriately in your app
                    }
                    finish();
                });
    }
}
