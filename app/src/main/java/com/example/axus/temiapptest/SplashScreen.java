package com.example.axus.temiapptest;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.robotemi.sdk.Robot;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen  extends AppCompatActivity {


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }



}
