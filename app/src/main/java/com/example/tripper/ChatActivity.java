package com.example.tripper;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.tripper.Util.ChatMessage;
import com.example.tripper.Util.ChatAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference messagesRef;

    private TextView chatTitle;
    private RecyclerView chatListView;
    private EditText messageInput;
    private Button sendButton;

    private String currentUserId;
    private String chatUserId;
    private String chatUserName;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        chatUserId = getIntent().getStringExtra("chatUserId");
        chatUserName = getIntent().getStringExtra("chatUserName");

        messagesRef = db.collection("chats").document(currentUserId)
                .collection("messages").document(chatUserId).collection("messages");

        chatTitle = findViewById(R.id.chat_title);
        chatTitle.setText("Chat with " + chatUserName);

        chatListView = findViewById(R.id.chat_list_view);
        chatListView.setLayoutManager(new LinearLayoutManager(this));
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        chatMessageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessageList);
        chatListView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString();
        if (messageText.isEmpty()) {
            Toast.makeText(ChatActivity.this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("receiverId", chatUserId);
        message.put("message", messageText);
        message.put("timestamp", System.currentTimeMillis());

        messagesRef.add(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");
            } else {
                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        messagesRef.orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DocumentChange documentChange : queryDocumentSnapshots.getDocumentChanges()) {
                    if (documentChange.getType() == DocumentChange.Type.ADDED) {
                        ChatMessage chatMessage = documentChange.getDocument().toObject(ChatMessage.class);
                        chatMessageList.add(chatMessage);
                        chatAdapter.notifyDataSetChanged();
                        chatListView.smoothScrollToPosition(chatMessageList.size() - 1);
                    }
                }
            }
        });
    }
}
//todo check why failed to send message! testgirl10
//todo check why failed to load messages! testgirl10