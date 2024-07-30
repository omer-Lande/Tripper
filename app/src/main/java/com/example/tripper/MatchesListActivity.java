package com.example.tripper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MatchesListActivity extends AppCompatActivity {

    private static final String TAG = "MatchesListActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListView matchesListView;
    private ArrayAdapter<String> adapter;
    private List<String> matchesList;
    private List<String> matchesIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches_list);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        matchesListView = findViewById(R.id.matches_list_view);
        matchesList = new ArrayList<>();
        matchesIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, matchesList);
        matchesListView.setAdapter(adapter);

        matchesListView.setOnItemClickListener((parent, view, position, id) -> {
            String chatUserId = matchesIds.get(position);
            String chatUserName = matchesList.get(position);
            Intent intent = new Intent(MatchesListActivity.this, ChatActivity.class);
            intent.putExtra("chatUserId", chatUserId);
            intent.putExtra("chatUserName", chatUserName);
            startActivity(intent);
        });

        loadMatches();
    }

    private void loadMatches() {
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> matches = (List<String>) documentSnapshot.get("matches");
                if (matches != null && !matches.isEmpty()) {
                    db.collection("users")
                            .whereIn("id", matches)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String matchName = document.getString("name");
                                        String matchId = document.getId();
                                        matchesList.add(matchName);
                                        matchesIds.add(matchId);
                                    }
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Log.e(TAG, "Error getting matches: ", task.getException());
                                }
                            });
                } else {
                    Toast.makeText(MatchesListActivity.this, "No matches found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
