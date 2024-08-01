package com.example.tripper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tripper.Util.Match;
import com.example.tripper.Util.MatchesListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MatchesListActivity extends AppCompatActivity {

    private static final String TAG = "MatchesListActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView matchesRecyclerView;
    private MatchesListAdapter adapter;
    private List<Match> matchesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches_list);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        matchesRecyclerView = findViewById(R.id.matches_list_view);
        matchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        matchesList = new ArrayList<>();
        adapter = new MatchesListAdapter(this, matchesList, match -> {
            Intent intent = new Intent(MatchesListActivity.this, ChatActivity.class);
            intent.putExtra("chatUserId", match.getId());
            intent.putExtra("chatUserName", match.getName());
            startActivity(intent);
        });
        matchesRecyclerView.setAdapter(adapter);

        loadMatches();
    }

    private void loadMatches() {
        Log.d(TAG, "loadMatches: Loading matches");
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> matches = (List<String>) documentSnapshot.get("matches");
                if (matches != null && !matches.isEmpty()) {
                    Log.d(TAG, "loadMatches: User matches found: " + matches);
                    db.collection("users")
                            .whereIn(FieldPath.documentId(), matches)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String matchName = document.getString("name");
                                        String matchImageUrl = document.getString("imageUrl");
                                        String matchId = document.getId();
                                        matchesList.add(new Match(matchId, matchName, matchImageUrl));
                                    }
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Log.e(TAG, "Error getting matches: ", task.getException());
                                }
                            });
                } else {
                    Log.d(TAG, "loadMatches: No matches found");
                    Toast.makeText(MatchesListActivity.this, "No matches found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "loadMatches: User document does not exist");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error getting user document: ", e));
    }
}
