package com.example.tripper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class SettingsActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private Button editBioButton, editFiltersButton, logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profileImageView = findViewById(R.id.profile_image);
        editBioButton = findViewById(R.id.edit_bio_button);
        editFiltersButton = findViewById(R.id.edit_filters_button);
        logoutButton = findViewById(R.id.logout_button);

        loadUserProfileImage();

        editBioButton.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, BioActivity.class)));
        editFiltersButton.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, SearchFilterActivity.class)));
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String imageUrl = documentSnapshot.getString("imageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Picasso.get().load(imageUrl).into(profileImageView);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to load profile image", Toast.LENGTH_SHORT).show());
        }
    }
}
