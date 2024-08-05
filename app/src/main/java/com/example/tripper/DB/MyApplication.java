package com.example.tripper.DB;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}

// TODO: 8/4/2024 remove un necessary toasts 
// TODO: 8/5/2024 add in the login password minimum requierments 