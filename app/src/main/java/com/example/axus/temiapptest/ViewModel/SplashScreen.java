package com.example.axus.temiapptest.ViewModel;

import android.content.Intent;
import android.os.Bundle;

import com.example.axus.temiapptest.R;
import com.example.axus.temiapptest.ViewModel.MainActivity;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen  extends AppCompatActivity {


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }



}
