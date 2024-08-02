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
    private TextView profileNameTextView;
    private ImageButton dislikeButton, likeButton;
    private Button unlimitedSwipesButton;
    private List<DocumentSnapshot> potentialMatches;
    List<String> seenUsers;
    private int currentMatchIndex;
    private int swipeCount;
    private boolean hasUnlimitedSwipes;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);

        Log.d(TAG, "onCreate: Initializing Firebase Auth and Firestore");
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        Log.d(TAG, "onCreate: Initializing UI components");
        profileImageView = findViewById(R.id.profile_image);
        bioTextView = findViewById(R.id.bio_text);
        profileNameTextView = findViewById(R.id.profile_name);
        dislikeButton = findViewById(R.id.dislike_button);
        likeButton = findViewById(R.id.like_button);
        unlimitedSwipesButton = findViewById(R.id.unlimited_swipes_button);

        potentialMatches = new ArrayList<>();
        currentMatchIndex = 0;
        swipeCount = 0;
        hasUnlimitedSwipes = false;

        loadUserProfile();
        loadPotentialMatches();

        dislikeButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Dislike button clicked");
            swipe(false);
        });
        likeButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Like button clicked");
            swipe(true);
        });
        unlimitedSwipesButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Unlimited Swipes button clicked");
            showPaymentDialog();
        });
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


                if (documentSnapshot.contains("seenUsers")) {
                    seenUsers = (List<String>) documentSnapshot.get("seenUsers");
                } else {
                    seenUsers = new ArrayList<>();
                    usersRef.document(userId).update("seenUsers", seenUsers);
                }
            }
        });
    }

    private void loadPotentialMatches() {
        Log.d(TAG, "loadPotentialMatches: Start loading potential matches");
        String userId = mAuth.getCurrentUser().getUid();
        usersRef.document(userId).get().addOnSuccessListener(documentSnapshot -> {
            Log.d(TAG, "loadPotentialMatches: Retrieved current user document");
            if (documentSnapshot.exists()) {
                List<String> seenUsers = (List<String>) documentSnapshot.get("seenUsers");
                if (seenUsers == null) {
                    seenUsers = new ArrayList<>();
                }
                final List<String> finalSeenUsers = seenUsers;
                Log.d(TAG, "loadPotentialMatches: Retrieved seen users: " + finalSeenUsers);

                List<String> interests = new ArrayList<>();
                for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                    if (entry.getKey().startsWith("filter_interest_") && (boolean) entry.getValue()) {
                        interests.add(entry.getKey().substring("filter_".length()));
                    }
                }

                if (interests.isEmpty()) {
                    Log.d(TAG, "loadPotentialMatches: Interests are empty");
                    Toast.makeText(MatchingActivity.this, "Please set your interests in search filters.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Log.d(TAG, "loadPotentialMatches: Querying with interests: " + interests);

                String ageRange = documentSnapshot.getString("ageRange");
                Log.d(TAG, "loadPotentialMatches: Retrieved age range: " + ageRange);

                String[] ageRangeArray = ageRange != null ? ageRange.split(" - ") : new String[0];
                final int minAge = ageRangeArray.length > 0 ? Integer.parseInt(ageRangeArray[0]) : 0;
                final int maxAge = ageRangeArray.length > 1 ? Integer.parseInt(ageRangeArray[1]) : 0;
                Log.d(TAG, "loadPotentialMatches: Parsed age range: " + minAge + " - " + maxAge);

                final String travelWith = documentSnapshot.getString("travelWith");
                Log.d(TAG, "loadPotentialMatches: Retrieved travelWith: " + travelWith);

                usersRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Log.d(TAG, "loadPotentialMatches: Checking potential match: " + document.getId());
                            if (!document.getId().equals(userId) && !finalSeenUsers.contains(document.getId())) {
                                String gender = document.getString("gender");
                                String ageString = document.getString("age");
                                boolean hasMatchingInterest = false;

                                for (String interest : interests) {
                                    Boolean interestValue = document.getBoolean(interest);
                                    if (interestValue != null && interestValue) {
                                        hasMatchingInterest = true;
                                        break;
                                    }
                                }

                                if (hasMatchingInterest) {
                                    Log.d(TAG, "loadPotentialMatches: Found matching interest for: " + document.getId());
                                    if (ageString != null && !ageString.isEmpty()) {
                                        int age = Integer.parseInt(ageString);
                                        Log.d(TAG, "loadPotentialMatches: Retrieved age: " + age);
                                        boolean genderMatches = travelWith != null && (travelWith.equals("Both") || (gender != null && gender.equals(travelWith)));
                                        Log.d(TAG, "loadPotentialMatches: Gender matches: " + genderMatches + ", Age matches: " + (age >= minAge && age <= maxAge));
                                        if (age >= minAge && age <= maxAge && genderMatches) {
                                            potentialMatches.add(document);
                                            Log.d(TAG, "loadPotentialMatches: Added potential match: " + document.getId());
                                        } else {
                                            Log.d(TAG, "loadPotentialMatches: Gender or age did not match for: " + document.getId());
                                        }
                                    } else {
                                        Log.d(TAG, "loadPotentialMatches: Age not available for: " + document.getId());
                                    }
                                } else {
                                    Log.d(TAG, "loadPotentialMatches: No matching interests for: " + document.getId());
                                }
                            } else {
                                Log.d(TAG, "loadPotentialMatches: User has already seen: " + document.getId());
                            }
                        }

                        if (potentialMatches.isEmpty()) {
                            profileNameTextView.setText("No more matches left. Consider changing your filters.");
                        } else {
                            showNextMatch();
                        }
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
            profileNameTextView.setVisibility(View.INVISIBLE);
            dislikeButton.setVisibility(View.INVISIBLE);
            likeButton.setVisibility(View.INVISIBLE);
            unlimitedSwipesButton.setVisibility(View.VISIBLE);

            return;
        }

        DocumentSnapshot match = potentialMatches.get(currentMatchIndex);
        String imageUrl = match.getString("imageUrl");
        String bio = match.getString("bio");
        String name = match.getString("name");

        Glide.with(this).load(imageUrl).into(profileImageView);
        bioTextView.setText(bio);
        profileNameTextView.setText(name);
        profileNameTextView.setVisibility(View.VISIBLE);

        currentMatchIndex++;
        Log.d(TAG, "showNextMatch: Displayed match index " + currentMatchIndex);
    }

    private void swipe(boolean liked) {
        if (!hasUnlimitedSwipes && swipeCount >= 15) {
            Toast.makeText(MatchingActivity.this, "No more swipes left. Get unlimited swipes!", Toast.LENGTH_SHORT).show();
            dislikeButton.setVisibility(View.GONE);
            likeButton.setVisibility(View.GONE);
            unlimitedSwipesButton.setVisibility(View.VISIBLE);
            return;
        }

        DocumentSnapshot match = potentialMatches.get(currentMatchIndex - 1);
        String likedUserId = match.getId();
        seenUsers.add(likedUserId);
        usersRef.document(mAuth.getCurrentUser().getUid())
                .update("seenUsers", FieldValue.arrayUnion(likedUserId));

        if (liked) {
            handleLike(likedUserId);
        }

        swipeCount++;
        showNextMatch();
    }


    private void handleLike(String likedUserId) {
        Log.d(TAG, "handleLike: Liked user ID " + likedUserId);
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference currentUserRef = usersRef.document(userId);
        DocumentReference likedUserRef = usersRef.document(likedUserId);

        // Add the liked user to the current user's list of liked users
        currentUserRef.update("likedUsers", FieldValue.arrayUnion(likedUserId))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "handleLike: Liked user added to current user's list"))
                .addOnFailureListener(e -> Log.e(TAG, "handleLike: Error adding liked user", e));

        // Check if the liked user has liked the current user
        likedUserRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                if (likedUsers != null && likedUsers.contains(userId)) {
                    // The liked user has liked the current user, add to matches
                    currentUserRef.update("matches", FieldValue.arrayUnion(likedUserId))
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "handleLike: Match added to current user"))
                            .addOnFailureListener(e -> Log.e(TAG, "handleLike: Error adding match to current user", e));

                    likedUserRef.update("matches", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "handleLike: Match added to liked user"))
                            .addOnFailureListener(e -> Log.e(TAG, "handleLike: Error adding match to liked user", e));

                    Toast.makeText(MatchingActivity.this, "It's a match!", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "handleLike: Error checking liked user", e));
    }

    private void showPaymentDialog() {
        Log.d(TAG, "showPaymentDialog: Showing payment dialog");
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
        Log.d(TAG, "processPayment: Processing payment");
        // Simulate payment processing
        Toast.makeText(this, "Payment successful! You now have unlimited swipes.", Toast.LENGTH_SHORT).show();

        // Update user profile to grant unlimited swipes
        String userId = mAuth.getCurrentUser().getUid();
        usersRef.document(userId).update("has_unlimited_swipes", true)
                .addOnSuccessListener(aVoid -> {
                    hasUnlimitedSwipes = true;
                    Log.d(TAG, "processPayment: Unlimited swipes granted");
                })
                .addOnFailureListener(e -> Log.e(TAG, "processPayment: Error updating user profile for unlimited swipes", e));
    }
}
//todo in production dont show users i liked or disliked, they only for testing
// fix the users i like.