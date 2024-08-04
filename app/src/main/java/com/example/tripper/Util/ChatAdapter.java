package com.example.tripper.Util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.tripper.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {


    private List<ChatMessage> userMessageList;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    public ChatAdapter(List<ChatMessage> userMessageList) {
        this.userMessageList = userMessageList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView senderMessageText, receiverMessageText;
        public ImageView receiverImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            receiverImageView = itemView.findViewById(R.id.message_profile_image);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);

        mAuth = FirebaseAuth.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String messageSenderId = mAuth.getCurrentUser().getUid();
        ChatMessage chatMessages = userMessageList.get(position);
        String fromUserId = chatMessages.getFrom();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserId);
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChild("image")){
                    String receiverImage = snapshot.child("image").getValue().toString();
                    Picasso.get().load(receiverImage).placeholder(R.drawable.ic_person).into(holder.receiverImageView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.receiverMessageText.setVisibility(View.INVISIBLE);
        holder.receiverImageView.setVisibility(View.INVISIBLE);
        if(fromUserId.equals(messageSenderId)){
            holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
            holder.senderMessageText.setText(chatMessages.getMessage());
        }
        else{
            holder.senderMessageText.setVisibility(View.INVISIBLE);
            holder.receiverMessageText.setVisibility(View.VISIBLE);
            holder.receiverImageView.setVisibility(View.VISIBLE);
            holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
            holder.receiverMessageText.setText(chatMessages.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return userMessageList.size();
    }

}
