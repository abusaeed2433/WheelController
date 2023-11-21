package com.example.wheelcontroller;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wheelcontroller.classes.DataSaver;
import com.example.wheelcontroller.databinding.ActivityLoginBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding = null;
    private Dialog mainDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setClickListener();
        addTextWatcher();
    }

    private void addTextWatcher(){
        binding.etWheelChairID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.tilWheelChair.setError(null);
            }
        });
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.tilPassword.setError(null);
            }
        });
    }

    private void setClickListener(){

        binding.buttonLogin.setOnClickListener((View v)->{
            String id = String.valueOf(binding.etWheelChairID.getText());
            String pass = String.valueOf(binding.etPassword.getText());

            login(id,pass);
        });

    }

    private void login(String id, String pass){
        if(id.isEmpty()){
            binding.tilWheelChair.setError(getString(R.string.cant_be_empty));
            return;
        }

        if(pass.isEmpty()){
            binding.tilPassword.setError(getString(R.string.cant_be_empty));
            return;
        }

        showProgress();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child(id);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String originalPass = null;
                final String[] name = {null};
                if (snapshot.exists()) {
                    originalPass = String.valueOf(snapshot.child("password").getValue());
                    name[0] = String.valueOf(snapshot.child("name").getValue());
                }

                safeDismiss();
                if (pass.equals(originalPass)) {
                    DataSaver.getInstance(LoginActivity.this).saveIdPass(id,pass,name[0]);
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
                else{
                    binding.tilPassword.setError("Invalid password");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                safeDismiss();
                binding.tilPassword.setError(error.getMessage());
            }
        });

    }

    private void safeDismiss(){
        try{
            mainDialog.dismiss();
        }
        catch (Exception ignored){}
    }

    public void showProgress() {
        mainDialog = new Dialog(this);
        mainDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mainDialog.setContentView(R.layout.progress_layout);

        Window window = mainDialog.getWindow();
        if(window != null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        mainDialog.setCanceledOnTouchOutside(false);
        mainDialog.setCancelable(false);
        mainDialog.show();
    }

}
