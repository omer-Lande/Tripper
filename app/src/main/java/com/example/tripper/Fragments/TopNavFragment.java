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
import com.example.tripper.BioActivity;
import com.example.tripper.MatchingActivity;
import com.example.tripper.MatchesListActivity;
import com.example.tripper.R;
import com.example.tripper.SettingsActivity;

public class TopNavFragment extends Fragment {

    private ImageView appIcon, settingsIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top_nav, container, false);

        appIcon = view.findViewById(R.id.app_icon);
        settingsIcon = view.findViewById(R.id.settings_icon);

        appIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), MatchingActivity.class)));
        settingsIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));

        return view;
    }
}
