package com.example.tripper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
    private TextView bioTextView, profileNameTextView;
    private ImageButton dislikeButton, likeButton;
    private Button unlimitedSwipesButton;
    private List<DocumentSnapshot> potentialMatches;
    private int currentMatchIndex;
    private int swipeCount;
    private boolean hasUnlimitedSwipes;
    private List<String> seenUsers;

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
        profileNameTextView = findViewById(R.id.profile_name);
        dislikeButton = findViewById(R.id.dislike_button);
        likeButton = findViewById(R.id.like_button);
        unlimitedSwipesButton = findViewById(R.id.unlimited_swipes_button);

        potentialMatches = new ArrayList<>();
        currentMatchIndex = 0;
        swipeCount = 0;
        hasUnlimitedSwipes = false;
        seenUsers = new ArrayList<>();

        loadUserProfile();
        checkForMatches();
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

                if (documentSnapshot.contains("seenUsers")) {
                    seenUsers = (List<String>) documentSnapshot.get("seenUsers");
                } else {
                    seenUsers = new ArrayList<>();
                }
            }
        });
    }

    private void loadPotentialMatches() {
        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "got userId: " + userId);
        usersRef.document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d(TAG, "got documentSnapshot:");
                List<String> interests = new ArrayList<>();
                for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                    if (entry.getKey().startsWith("filter_interest_") && (boolean) entry.getValue()) {
                        interests.add(entry.getKey());
                        Log.d(TAG, "got interests:");
                    }
                }

                if (interests.isEmpty()) {
                    Toast.makeText(MatchingActivity.this, "Please set your interests in search filters.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String ageRange = documentSnapshot.getString("ageRange");
                String[] ageRangeArray = ageRange != null ? ageRange.split(" - ") : new String[0];
                final int minAge = ageRangeArray.length > 0 ? Integer.parseInt(ageRangeArray[0]) : 0;
                final int maxAge = ageRangeArray.length > 1 ? Integer.parseInt(ageRangeArray[1]) : 0;
                final String travelWith = documentSnapshot.getString("travelWith");

                usersRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Task Successful:");
                        QuerySnapshot querySnapshot = task.getResult();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            if (!document.getId().equals(userId) && (seenUsers == null || !seenUsers.contains(document.getId()))) {
                                Boolean isMan = document.getBoolean("male");
                                Boolean isWoman = document.getBoolean("female");
                                String ageString = document.getString("age");
                                Log.d(TAG, "got isMan: " + isMan);
                                Log.d(TAG, "got isWoman: " + isWoman);
                                Log.d(TAG, "got age: " + ageString);

                                String gender = null;
                                if (isMan != null && isMan) {
                                    gender = "men";
                                } else if (isWoman != null && isWoman) {
                                    gender = "women";
                                }

                                // Check if this user matches any of the interests
                                boolean interestMatches = false;
                                for (String interest : interests) {
                                    if (document.contains(interest) && document.getBoolean(interest)) {
                                        interestMatches = true;
                                        break;
                                    }
                                }

                                if (ageString != null && !ageString.isEmpty() && interestMatches) {
                                    int age = Integer.parseInt(ageString);
                                    if ("both".equalsIgnoreCase(travelWith) || (gender != null && gender.equalsIgnoreCase(travelWith))) {
                                        boolean genderMatches = "both".equalsIgnoreCase(travelWith) || gender.equalsIgnoreCase(travelWith);
                                        if (age >= minAge && age <= maxAge && genderMatches) {
                                            potentialMatches.add(document);
                                        }
                                    }
                                }
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
        Log.d(TAG, "handleLike: Liked user ID " + likedUserId);
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference currentUserRef = usersRef.document(userId);
        DocumentReference likedUserRef = usersRef.document(likedUserId);

        // First, add the liked user to the current user's list of liked users
        currentUserRef.update("likedUsers", FieldValue.arrayUnion(likedUserId))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "handleLike: Liked user added to current user's list"))
                .addOnFailureListener(e -> Log.e(TAG, "handleLike: Error adding liked user", e));

        // Now, check if the liked user has already liked the current user
        likedUserRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> likedUsersOfLikedUser = (List<String>) documentSnapshot.get("likedUsers");
                if (likedUsersOfLikedUser != null && likedUsersOfLikedUser.contains(userId)) {
                    // The liked user has already liked the current user, so add them to the matches
                    currentUserRef.update("matches", FieldValue.arrayUnion(likedUserId))
                            .addOnSuccessListener(aVoid -> {
                                showMatchIconAnimation();
                                Log.d(TAG, "handleLike: Match added to current user");
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "handleLike: Error adding match to current user", e));

                    likedUserRef.update("matches", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "handleLike: Match added to liked user"))
                            .addOnFailureListener(e -> Log.e(TAG, "handleLike: Error adding match to liked user", e));
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "handleLike: Error checking liked user", e));

        seenUsers.add(likedUserId);
        currentUserRef.update("seenUsers", seenUsers)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "handleLike: Seen users updated"))
                .addOnFailureListener(e -> Log.e(TAG, "handleLike: Error updating seen users", e));
    }

    private void checkForMatches() {
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference currentUserRef = usersRef.document(userId);

        currentUserRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                List<String> currentMatches = (List<String>) documentSnapshot.get("matches");

                if (likedUsers != null && !likedUsers.isEmpty()) {
                    for (String likedUserId : likedUsers) {
                        usersRef.document(likedUserId).get().addOnSuccessListener(likedUserSnapshot -> {
                            if (likedUserSnapshot.exists()) {
                                List<String> likedUsersOfLikedUser = (List<String>) likedUserSnapshot.get("likedUsers");
                                if (likedUsersOfLikedUser != null && likedUsersOfLikedUser.contains(userId)) {
                                    // Check if the match already exists
                                    if (currentMatches != null && currentMatches.contains(likedUserId)) {
                                        Log.d(TAG, "checkForMatches: Match already exists, not adding again");
                                    } else {
                                        // The liked user has liked the current user, add to matches
                                        currentUserRef.update("matches", FieldValue.arrayUnion(likedUserId))
                                                .addOnSuccessListener(aVoid -> {
                                                    showMatchIconAnimation();
                                                    Log.d(TAG, "checkForMatches: Match added to current user");
                                                })
                                                .addOnFailureListener(e -> Log.e(TAG, "checkForMatches: Error adding match to current user", e));

                                        usersRef.document(likedUserId).update("matches", FieldValue.arrayUnion(userId))
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "checkForMatches: Match added to liked user"))
                                                .addOnFailureListener(e -> Log.e(TAG, "checkForMatches: Error adding match to liked user", e));
                                    }
                                }
                            }
                        });
                    }
                }
            }
        });
    }


    private void showMatchIconAnimation() {
        ImageView matchIcon = findViewById(R.id.match_icon);
        matchIcon.setVisibility(View.VISIBLE);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(matchIcon, "scaleX", 0f, 1.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(matchIcon, "scaleY", 0f, 1.5f, 1f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(1000);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                matchIcon.setVisibility(View.GONE);
            }
        });

        animatorSet.start();
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
