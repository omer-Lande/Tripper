package com.example.tripper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingActivity extends AppCompatActivity {

    private static final String TAG = "MatchingActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private ImageView profileImageView;
    private TextView bioTextView;
    private ImageButton dislikeButton, likeButton;
    private Button unlimitedSwipesButton;
    private List<DocumentSnapshot> potentialMatches;
    private int currentMatchIndex;
    private int swipeCount;
    private boolean hasUnlimitedSwipes;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        profileImageView = findViewById(R.id.profile_image);
        bioTextView = findViewById(R.id.bio_text);
        dislikeButton = findViewById(R.id.dislike_button);
        likeButton = findViewById(R.id.like_button);
        unlimitedSwipesButton = findViewById(R.id.unlimited_swipes_button);

        potentialMatches = new ArrayList<>();
        currentMatchIndex = 0;
        swipeCount = 0;
        hasUnlimitedSwipes = false;

        loadUserProfile();
        loadPotentialMatches();

        dislikeButton.setOnClickListener(v -> swipe(false));
        likeButton.setOnClickListener(v -> swipe(true));
        unlimitedSwipesButton.setOnClickListener(v -> showPaymentDialog());
    }

    private void loadUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();
        usersRef.document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("swipe_count")) {
                    swipeCount = documentSnapshot.getLong("swipe_count").intValue();
                } else {
                    swipeCount = 0;
                }

                if (documentSnapshot.contains("has_unlimited_swipes")) {
                    hasUnlimitedSwipes = documentSnapshot.getBoolean("has_unlimited_swipes");
                } else {
                    hasUnlimitedSwipes = false;
                }
            }
        });
    }

    private void loadPotentialMatches() {
        String userId = mAuth.getCurrentUser().getUid();
        usersRef.document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> interests = new ArrayList<>();
                for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                    if (entry.getKey().startsWith("filter_interest_") && (boolean) entry.getValue()) {
                        interests.add(entry.getKey());
                    }
                }

                if (interests.isEmpty()) {
                    // Handle the case when interests are empty to avoid the array_contains_any filter error
                    Toast.makeText(MatchingActivity.this, "Please set your interests in search filters.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                usersRef.whereArrayContainsAny("interests", interests).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            if (!document.getId().equals(userId)) {
                                potentialMatches.add(document);
                            }
                        }
                        showNextMatch();
                    } else {
                        Log.e(TAG, "Error getting potential matches: ", task.getException());
                    }
                });
            }
        });
    }

    private void showNextMatch() {
        if (currentMatchIndex >= potentialMatches.size()) {
            // No more matches to show
            profileImageView.setImageResource(R.drawable.ic_app_icon);
            bioTextView.setText("No more matches left");
            unlimitedSwipesButton.setVisibility(View.VISIBLE);
            return;
        }

        DocumentSnapshot match = potentialMatches.get(currentMatchIndex);
        String imageUrl = match.getString("imageUrl");
        String bio = match.getString("bio");

        Glide.with(this).load(imageUrl).into(profileImageView);
        bioTextView.setText(bio);

        currentMatchIndex++;
    }

    private void swipe(boolean liked) {
        if (!hasUnlimitedSwipes && swipeCount >= 15) {
            Toast.makeText(MatchingActivity.this, "No more swipes left. Get unlimited swipes!", Toast.LENGTH_SHORT).show();
            unlimitedSwipesButton.setVisibility(View.VISIBLE);
            return;
        }

        if (liked) {
            DocumentSnapshot match = potentialMatches.get(currentMatchIndex - 1);
            String likedUserId = match.getId();
            handleLike(likedUserId);
        }

        swipeCount++;
        showNextMatch();
    }

    private void handleLike(String likedUserId) {
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference currentUserRef = usersRef.document(userId);
        DocumentReference likedUserRef = usersRef.document(likedUserId);

        // Add the liked user to the current user's list of liked users
        currentUserRef.update("likedUsers", FieldValue.arrayUnion(likedUserId));

        // Check if the liked user has liked the current user
        likedUserRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                if (likedUsers != null && likedUsers.contains(userId)) {
                    // The liked user has liked the current user, add to matches
                    currentUserRef.update("matches", FieldValue.arrayUnion(likedUserId));
                    likedUserRef.update("matches", FieldValue.arrayUnion(userId));

                    Toast.makeText(MatchingActivity.this, "It's a match!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_payment, null);
        builder.setView(dialogView);

        builder.setTitle("Payment Information")
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText cardNumberEditText = dialogView.findViewById(R.id.card_number);
                        EditText cardExpiryEditText = dialogView.findViewById(R.id.card_expiry);
                        EditText cardCvvEditText = dialogView.findViewById(R.id.card_cvv);

                        String cardNumber = cardNumberEditText.getText().toString();
                        String cardExpiry = cardExpiryEditText.getText().toString();
                        String cardCvv = cardCvvEditText.getText().toString();

                        // Validate payment information
                        if (validatePaymentInfo(cardNumber, cardExpiry, cardCvv)) {
                            processPayment(cardNumber, cardExpiry, cardCvv);
                        } else {
                            Toast.makeText(MatchingActivity.this, "Invalid payment information", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private boolean validatePaymentInfo(String cardNumber, String cardExpiry, String cardCvv) {
        // Add your payment validation logic here
        return !cardNumber.isEmpty() && !cardExpiry.isEmpty() && !cardCvv.isEmpty();
    }

    private void processPayment(String cardNumber, String cardExpiry, String cardCvv) {
        // Simulate payment processing
        Toast.makeText(this, "Payment successful! You now have unlimited swipes.", Toast.LENGTH_SHORT).show();

        // Update user profile to grant unlimited swipes
        String userId = mAuth.getCurrentUser().getUid();
        usersRef.document(userId).update("has_unlimited_swipes", true)
                .addOnSuccessListener(aVoid -> hasUnlimitedSwipes = true)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating user profile for unlimited swipes", e));
    }
}
