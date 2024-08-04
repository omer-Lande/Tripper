package com.example.tripper.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.tripper.BioActivity;
import com.example.tripper.MatchingActivity;
import com.example.tripper.MatchesListActivity;
import com.example.tripper.R;

public class BottomNavFragment extends Fragment {

    private ImageView homeIcon, personIcon, chatIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_nav, container, false);

        homeIcon = view.findViewById(R.id.home_icon);
        personIcon = view.findViewById(R.id.person_icon);
        chatIcon = view.findViewById(R.id.chat_icon);

        homeIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), MatchingActivity.class)));
        personIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), BioActivity.class)));
        chatIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), MatchesListActivity.class)));

        return view;
    }
}
