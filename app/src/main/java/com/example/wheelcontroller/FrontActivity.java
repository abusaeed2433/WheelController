package com.example.wheelcontroller;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wheelcontroller.databinding.ActivityFrontBinding;

public class FrontActivity extends AppCompatActivity {

    private int progress = 0;
    private ActivityFrontBinding binding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFrontBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.myProgress.setProgressListener(() -> {
            //startActivity(new Intent(FrontActivity.this,MainActivity.class));
            startActivity(new Intent(FrontActivity.this,LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

    }

}
