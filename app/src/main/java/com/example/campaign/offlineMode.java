package com.example.campaign;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class offlineMode extends Application {
    public void onCreate(){
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

}