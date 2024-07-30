package com.example.tripper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView profileImageView;
    private TextView bioTextView;
    private ImageView swipeLeft, swipeRight;
    private Button unlimitedSwipesButton, noMoreSwipesButton;
    private RelativeLayout swipeButtons, noMoreSwipesLayout;

    private List<DocumentSnapshot> users;
    private int currentIndex = 0;
    private int swipesRemaining = 15;
    private boolean hasUnlimitedSwipes = false;
    private long lastSwipeTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileImageView = findViewById(R.id.profile_image);
        bioTextView = findViewById(R.id.bio_text);
        swipeLeft = findViewById(R.id.swipe_left);
        swipeRight = findViewById(R.id.swipe_right);
        unlimitedSwipesButton = findViewById(R.id.unlimited_swipes_button);
        noMoreSwipesButton = findViewById(R.id.no_more_swipes_button);
        swipeButtons = findViewById(R.id.swipe_buttons);
        noMoreSwipesLayout = findViewById(R.id.no_more_swipes_layout);

        checkSwipeLimit();
        loadUsers();

        swipeLeft.setOnClickListener(v -> swipeLeft());
        swipeRight.setOnClickListener(v -> swipeRight());
        unlimitedSwipesButton.setOnClickListener(v -> showPaymentDialog());
        noMoreSwipesButton.setOnClickListener(v -> showPaymentDialog());
    }

    private void checkSwipeLimit() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            swipesRemaining = document.getLong("swipeCount").intValue();
                            lastSwipeTimestamp = document.getLong("lastSwipeTimestamp");
                            hasUnlimitedSwipes = document.getBoolean("hasUnlimitedSwipes");

                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastSwipeTimestamp > 86400000) { // 24 hours in milliseconds
                                resetSwipeLimit();
                            } else if (!hasUnlimitedSwipes && swipesRemaining >= 15) {
                                showNoMoreSwipes();
                            }
                        }
                    }
                });
    }

    private void resetSwipeLimit() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .update("swipeCount", 0, "lastSwipeTimestamp", System.currentTimeMillis());
        swipesRemaining = 15;
    }

    private void loadUsers() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .whereArrayContainsAny("interests", getCurrentUserInterests())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            users = querySnapshot.getDocuments();
                            showNextUser();
                        }
                    }
                });
    }

    private List<String> getCurrentUserInterests() {
        // Fetch current user's interests from Firestore or local storage
        // Example return value: return Arrays.asList("hiking", "music", "sports");
        return new ArrayList<>(); // Replace with actual implementation
    }

    private void showNextUser() {
        if (currentIndex < users.size() && (swipesRemaining > 0 || hasUnlimitedSwipes)) {
            DocumentSnapshot userDoc = users.get(currentIndex);
            String bio = userDoc.getString("bio");
            String imageUrl = userDoc.getString("imageUrl");

            bioTextView.setText(bio);
            Glide.with(this).load(imageUrl).into(profileImageView);

            currentIndex++;
        } else {
            showNoMoreSwipes();
        }
    }

    private void swipeLeft() {
        if (swipesRemaining > 0 || hasUnlimitedSwipes) {
            if (!hasUnlimitedSwipes) {
                swipesRemaining--;
                updateSwipeCount();
            }
            showNextUser();
        }
    }

    private void swipeRight() {
        if (swipesRemaining > 0 || hasUnlimitedSwipes) {
            if (!hasUnlimitedSwipes) {
                swipesRemaining--;
                updateSwipeCount();
            }
            // Mark user as travel-option
            markTravelOption(users.get(currentIndex - 1).getId());
            showNextUser();
        }
    }

    private void updateSwipeCount() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("swipeCount", FieldValue.increment(1));
        updates.put("lastSwipeTimestamp", System.currentTimeMillis());
        db.collection("users").document(currentUserId).update(updates);
    }

    private void markTravelOption(String targetUserId) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .update("travelOptions", FieldValue.arrayUnion(targetUserId))
                .addOnCompleteListener(task -> checkMutualTravelOption(currentUserId, targetUserId));
    }

    private void checkMutualTravelOption(String currentUserId, String targetUserId) {
        db.collection("users").document(targetUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> travelOptions = (List<String>) document.get("travelOptions");
                            if (travelOptions != null && travelOptions.contains(currentUserId)) {
                                // Mutual travel-option found, add to matching list activity
                                addToMatchingList(currentUserId, targetUserId);
                            }
                        }
                    }
                });
    }

    private void addToMatchingList(String currentUserId, String targetUserId) {
        // Implement logic to add to the matching list activity
        Toast.makeText(this, "Mutual travel-option found! Added to matching list.", Toast.LENGTH_SHORT).show();
    }

    private void showNoMoreSwipes() {
        swipeButtons.setVisibility(View.GONE);
        noMoreSwipesLayout.setVisibility(View.VISIBLE);
    }

    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unlimited Swipes");
        builder.setMessage("Please enter your credit card details to get unlimited swipes.");

        // Inflate custom layout for payment form
        LayoutInflater inflater = this.getLayoutInflater();
        View paymentView = inflater.inflate(R.layout.dialog_payment, null);
        builder.setView(paymentView);

        builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle payment submission
                // For now, we simulate a successful payment
                Toast.makeText(MatchingActivity.this, "Payment Successful! You now have unlimited swipes.", Toast.LENGTH_SHORT).show();
                grantUnlimitedSwipes();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void grantUnlimitedSwipes() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .update("hasUnlimitedSwipes", true)
                .addOnCompleteListener(task -> {
                    hasUnlimitedSwipes = true;
                    swipeButtons.setVisibility(View.VISIBLE);
                    noMoreSwipesLayout.setVisibility(View.GONE);
                    swipesRemaining = Integer.MAX_VALUE;
                    showNextUser();
                });
    }
}
