package com.example.tripper.Util;

import com.example.tripper.MatchesListActivity;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.tripper.R;

import java.util.List;

public class MatchesListAdapter extends RecyclerView.Adapter<MatchesListAdapter.ViewHolder> {

    private List<Match> matches;
    private Context context;
    private OnItemClickListener listener;

    public MatchesListAdapter(Context context, List<Match> matches, OnItemClickListener listener) {
        this.context = context;
        this.matches = matches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Match match = matches.get(position);
        holder.matchName.setText(match.getName());
        Glide.with(context).load(match.getImageUrl()).into(holder.matchImage);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(match));
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView matchImage;
        public TextView matchName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            matchImage = itemView.findViewById(R.id.match_image);
            matchName = itemView.findViewById(R.id.match_name);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Match match);
    }
}
