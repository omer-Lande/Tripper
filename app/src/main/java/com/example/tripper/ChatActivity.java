package com.example.tripper;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.example.tripper.Util.ChatMessage;
import com.example.tripper.Util.ChatAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference messagesRef;
    private TextView userName;
    private ImageView userImage;
    private DatabaseReference RootRef;

    private TextView chatTitle;
    private RecyclerView chatListView;
    private EditText messageInput;
    private Button sendButton;

    private String messageRefference;
    private String chatUserImage;
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
        RootRef = FirebaseDatabase.getInstance().getReference();

        chatUserId = getIntent().getStringExtra("chatUserId");
        chatUserName = getIntent().getStringExtra("chatUserName");
        chatUserImage = getIntent().getStringExtra("userImage");
        Toast.makeText(ChatActivity.this, "Welcome",Toast.LENGTH_SHORT).show();

       // InitializeControllers();

        if (chatUserId == null || chatUserName == null) {
            Toast.makeText(this, "Chat user not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Chatting with: " + chatUserId + " (" + chatUserName + ")");

        // Collection for storing messages between users
       // String chatId = generateChatId(currentUserId, chatUserId);
       // messagesRef = db.collection("chats").document(chatId).collection("messages");

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

        //loadMessages();
    }

    private void InitializeControllers() {
        userName = findViewById(R.id.custom_profile_name);
        userImage = findViewById(R.id.custom_profile_IMG);
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString();
        if (messageText.isEmpty()) {
            Toast.makeText(ChatActivity.this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageSenderReference = "Messages/" + currentUserId + "/" + chatUserId;
        String messageRecReference = "Messages/" + chatUserId + "/" + currentUserId;
        DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(currentUserId).child(chatUserId).push();
        String messagePushID = userMessageKeyRef.getKey();

        Map <String, Object> messageTextBody = new HashMap<>();
        messageTextBody.put("message", messageText);
        messageTextBody.put("from", currentUserId);

        Log.d(TAG,"messageText: " + messageText + " currentUserId: " + currentUserId);
        Log.d(TAG,"RootRef: " + RootRef);

        Map <String, Object> messageBodyDetails = new HashMap<>();
        messageBodyDetails.put(messageSenderReference + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put(messageRecReference + "/" + messagePushID, messageTextBody);
        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(ChatActivity.this, "message sent successfully",Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(ChatActivity.this,"Error",Toast.LENGTH_SHORT).show();
            }
            messageInput.setText("");
        });

     /*   Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("receiverId", chatUserId);
        message.put("message", messageText);
        message.put("timestamp", System.currentTimeMillis());

        messagesRef.add(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");
                Log.d(TAG, "Message sent successfully");
            } else {
                Log.e(TAG, "Failed to send message", task.getException());
            }
        });*/
    }
/*
    private void loadMessages() {
        messagesRef.orderBy("timestamp").addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Failed to load messages", e);
                return;
            }
            if (queryDocumentSnapshots != null) {
                for (DocumentChange documentChange : queryDocumentSnapshots.getDocumentChanges()) {
                    if (documentChange.getType() == DocumentChange.Type.ADDED) {
                        ChatMessage chatMessage = documentChange.getDocument().toObject(ChatMessage.class);
                        chatMessageList.add(chatMessage);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                chatListView.smoothScrollToPosition(chatMessageList.size() - 1);
            }
        });
    }

    private String generateChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }*/
}
