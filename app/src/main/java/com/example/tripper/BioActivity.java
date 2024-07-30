package com.example.tripper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class BioActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profileImageView;
    private Button uploadButton;
    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EditText bioText, ageText, nameText;
    private TextView charCountText;
    private CheckBox interestHiking, interestParties, interestCasualFun, interestRestaurants, interestMonuments, interestExploring, interestMusic, interestArt, interestSports;
    private CheckBox maleCheckbox, femaleCheckbox;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bio);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();

        profileImageView = findViewById(R.id.profile_image);
        uploadButton = findViewById(R.id.upload_button);
        bioText = findViewById(R.id.bio_text);
        ageText = findViewById(R.id.age);
        nameText = findViewById(R.id.name);
        charCountText = findViewById(R.id.char_count);
        interestHiking = findViewById(R.id.interest_hiking);
        interestParties = findViewById(R.id.interest_parties);
        interestCasualFun = findViewById(R.id.interest_casual_fun);
        interestRestaurants = findViewById(R.id.interest_restaurants);
        interestMonuments = findViewById(R.id.interest_monuments);
        interestExploring = findViewById(R.id.interest_exploring);
        interestMusic = findViewById(R.id.interest_music);
        interestArt = findViewById(R.id.interest_art);
        interestSports = findViewById(R.id.interest_sports);
        maleCheckbox = findViewById(R.id.male);
        femaleCheckbox = findViewById(R.id.female);
        nextButton = findViewById(R.id.next_button);

        uploadButton.setOnClickListener(v -> openFileChooser());

        bioText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCountText.setText(s.length() + "/300");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        nextButton.setOnClickListener(v -> saveProfile());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
            uploadImage();
        }
    }

    private void uploadImage() {
        if (imageUri != null) {
            StorageReference fileReference = storageReference.child("profile_images/" + mAuth.getCurrentUser().getUid() + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveImageUri(uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(BioActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveImageUri(String uri) {
        Map<String, Object> user = new HashMap<>();
        user.put("imageUrl", uri);

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BioActivity.this, "Image URL saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BioActivity.this, "Failed to save image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile() {
        String bio = bioText.getText().toString();
        String age = ageText.getText().toString();
        String name = nameText.getText().toString();
        boolean isMale = maleCheckbox.isChecked();
        boolean isFemale = femaleCheckbox.isChecked();

        if (bio.isEmpty() || age.isEmpty() || name.isEmpty() || (!isMale && !isFemale)) {
            Toast.makeText(BioActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("bio", bio);
        user.put("age", age);
        user.put("name", name);
        user.put("male", isMale);
        user.put("female", isFemale);
        user.put("interest_hiking", interestHiking.isChecked());
        user.put("interest_parties", interestParties.isChecked());
        user.put("interest_casual_fun", interestCasualFun.isChecked());
        user.put("interest_restaurants", interestRestaurants.isChecked());
        user.put("interest_monuments", interestMonuments.isChecked());
        user.put("interest_exploring", interestExploring.isChecked());
        user.put("interest_music", interestMusic.isChecked());
        user.put("interest_art", interestArt.isChecked());
        user.put("interest_sports", interestSports.isChecked());

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BioActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(BioActivity.this, SearchFilterActivity.class));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BioActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
