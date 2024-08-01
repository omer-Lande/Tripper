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
        Log.d(TAG, "loadUserProfile: Loading user profile");
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
                Log.d(TAG, "loadUserProfile: User profile loaded successfully");
            } else {
                Log.d(TAG, "loadUserProfile: User profile does not exist");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "loadUserProfile: Error loading user profile", e));
    }

    private void loadPotentialMatches() {
        Log.d(TAG, "loadPotentialMatches: Loading potential matches");
        String userId = mAuth.getCurrentUser().getUid();

        usersRef.document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d(TAG, "loadPotentialMatches: User document exists");

                // Retrieve the user's filter interests
                List<String> filterInterests = new ArrayList<>();
                for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                    if (entry.getKey().startsWith("filter_interest_") && (boolean) entry.getValue()) {
                        filterInterests.add(entry.getKey().substring("filter_".length()));
                    }
                }
                Log.d(TAG, "loadPotentialMatches: Filter interests: " + filterInterests);

                // Check if interests are empty
                if (filterInterests.isEmpty()) {
                    Log.d(TAG, "loadPotentialMatches: No filter interests found");
                    Toast.makeText(MatchingActivity.this, "Please set your interests in search filters.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Retrieve gender preference and age range
                String travelWith = documentSnapshot.getString("travelWith");
                String ageRange = documentSnapshot.getString("ageRange");

                if (travelWith == null || ageRange == null) {
                    Log.d(TAG, "loadPotentialMatches: Gender preference or age range not set");
                    Toast.makeText(MatchingActivity.this, "Please set your gender preference and age range in search filters.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String[] ageRangeSplit = ageRange.split(" - ");
                int minAge = Integer.parseInt(ageRangeSplit[0]);
                int maxAge = Integer.parseInt(ageRangeSplit[1]);

                Log.d(TAG, "loadPotentialMatches: Travel with: " + travelWith);
                Log.d(TAG, "loadPotentialMatches: Age range: " + minAge + " - " + maxAge);

                // Firestore query to find potential matches based on interests
                usersRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            if (!document.getId().equals(userId)) {
                                // Check interests
                                boolean matchesInterests = false;
                                for (String interest : filterInterests) {
                                    if (document.getBoolean(interest) != null && document.getBoolean(interest)) {
                                        matchesInterests = true;
                                        break;
                                    }
                                }

                                // Check gender
                                boolean matchesGender = false;
                                if (travelWith.equals("Both")) {
                                    matchesGender = true;
                                } else if (travelWith.equals("Men") && document.getBoolean("male") != null && document.getBoolean("male")) {
                                    matchesGender = true;
                                } else if (travelWith.equals("Women") && document.getBoolean("female") != null && document.getBoolean("female")) {
                                    matchesGender = true;
                                }

                                // Check age range
                                String ageString = document.getString("age");
                                boolean matchesAge = false;
                                if (ageString != null) {
                                    try {
                                        int age = Integer.parseInt(ageString);
                                        matchesAge = age >= minAge && age <= maxAge;
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "loadPotentialMatches: Invalid age format for user: " + document.getId(), e);
                                    }
                                }

                                if (matchesInterests && matchesGender && matchesAge) {
                                    potentialMatches.add(document);
                                }
                            }
                        }
                        Log.d(TAG, "loadPotentialMatches: Number of potential matches found: " + potentialMatches.size());
                        if (potentialMatches.isEmpty()) {
                            Toast.makeText(MatchingActivity.this, "No matches found.", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "swipe: Swiping " + (liked ? "right" : "left"));
        if (!hasUnlimitedSwipes && swipeCount >= 15) {
            Log.d(TAG, "swipe: No more swipes left");
            Toast.makeText(MatchingActivity.this, "No more swipes left. Get unlimited swipes!", Toast.LENGTH_SHORT).show();
            unlimitedSwipesButton.setVisibility(View.VISIBLE);
            dislikeButton.setVisibility(View.INVISIBLE);
            dislikeButton.setEnabled(false);
            likeButton.setVisibility(View.INVISIBLE);
            likeButton.setEnabled(false);
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