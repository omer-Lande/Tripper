package com.example.tripper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFilterActivity extends AppCompatActivity {

    private EditText locationInput;
    private RadioGroup radioGroupTravelWith;
    private Spinner ageRangeMinSpinner, ageRangeMaxSpinner;
    private CheckBox checkboxHiking, checkboxParties, checkboxCasualFun, checkboxRestaurants, checkboxMonuments, checkboxExploring, checkboxMusic, checkboxArt, checkboxSports;
    private Button applyFiltersButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<String> ageRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_filters);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        locationInput = findViewById(R.id.location_input);
        radioGroupTravelWith = findViewById(R.id.radio_group_travel_with);
        ageRangeMinSpinner = findViewById(R.id.age_range_min_spinner);
        ageRangeMaxSpinner = findViewById(R.id.age_range_max_spinner);
        checkboxHiking = findViewById(R.id.checkbox_hiking);
        checkboxParties = findViewById(R.id.checkbox_parties);
        checkboxCasualFun = findViewById(R.id.checkbox_casual_fun);
        checkboxRestaurants = findViewById(R.id.checkbox_restaurants);
        checkboxMonuments = findViewById(R.id.checkbox_monuments);
        checkboxExploring = findViewById(R.id.checkbox_exploring);
        checkboxMusic = findViewById(R.id.checkbox_music);
        checkboxArt = findViewById(R.id.checkbox_art);
        checkboxSports = findViewById(R.id.checkbox_sports);
        applyFiltersButton = findViewById(R.id.apply_filters_button);

        ageRange = new ArrayList<>();
        for (int i = 16; i <= 90; i++) {
            ageRange.add(String.valueOf(i));
        }

        ArrayAdapter<String> minAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ageRange);
        minAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageRangeMinSpinner.setAdapter(minAdapter);

        ageRangeMinSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMaxAgeSpinner(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ageRangeMinSpinner.setSelection(0); // Set initial value

        applyFiltersButton.setOnClickListener(v -> saveFilters());
    }

    private void updateMaxAgeSpinner(int minAgePosition) {
        List<String> maxAgeList = new ArrayList<>(ageRange.subList(minAgePosition, ageRange.size()));
        ArrayAdapter<String> maxAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, maxAgeList);
        maxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageRangeMaxSpinner.setAdapter(maxAdapter);
    }

    private void saveFilters() {
        String location = locationInput.getText().toString();
        String travelWith = "";
        int selectedId = radioGroupTravelWith.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            travelWith = selectedRadioButton.getText().toString();
        }

        String minAge = ageRangeMinSpinner.getSelectedItem().toString();
        String maxAge = ageRangeMaxSpinner.getSelectedItem().toString();

        Map<String, Object> filters = new HashMap<>();
        filters.put("location", location);
        filters.put("travelWith", travelWith);
        filters.put("ageRange", minAge + " - " + maxAge);
        filters.putAll(getFilterInterests());

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .set(filters, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SearchFilterActivity.this, "Filters saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SearchFilterActivity.this, MatchingActivity.class));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchFilterActivity.this, "Failed to save filters: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, Boolean> getFilterInterests() {
        Map<String, Boolean> interests = new HashMap<>();
        interests.put("filter_interest_hiking", checkboxHiking.isChecked());
        interests.put("filter_interest_parties", checkboxParties.isChecked());
        interests.put("filter_interest_casual_fun", checkboxCasualFun.isChecked());
        interests.put("filter_interest_restaurants", checkboxRestaurants.isChecked());
        interests.put("filter_interest_monuments", checkboxMonuments.isChecked());
        interests.put("filter_interest_exploring", checkboxExploring.isChecked());
        interests.put("filter_interest_music", checkboxMusic.isChecked());
        interests.put("filter_interest_art", checkboxArt.isChecked());
        interests.put("filter_interest_sports", checkboxSports.isChecked());
        return interests;
    }
}

//todo attend upload photos