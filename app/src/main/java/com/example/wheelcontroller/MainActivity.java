package com.example.wheelcontroller;

import static com.example.wheelcontroller.enums.Command.BACKWARD;
import static com.example.wheelcontroller.enums.Command.CONNECT;
import static com.example.wheelcontroller.enums.Command.FORWARD;
import static com.example.wheelcontroller.enums.Command.LEFT;
import static com.example.wheelcontroller.enums.Command.RIGHT;
import static com.example.wheelcontroller.enums.Command.SHUT_DOWN;
import static com.example.wheelcontroller.enums.Command.STOP;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.example.wheelcontroller.classes.DataSaver;
import com.example.wheelcontroller.classes.Utility;
import com.example.wheelcontroller.databinding.ActivityMainBinding;
import com.example.wheelcontroller.enums.Command;
import com.example.wheelcontroller.listener.DatabaseListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding = null;
    private boolean isConnected = false;
    private boolean isProcessing = false;
    private Command prevCommand = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setClickListener();
    }

    private void setClickListener() {
        binding.ivPower.setOnClickListener((View view) -> switchPowerMode());
        binding.buttonShowConnection.setOnClickListener((View v) -> hideConnectionView(false));

        binding.ivLeft.setOnClickListener((View view) -> startExecution(LEFT));
        binding.ivTop.setOnClickListener((View view) -> startExecution(FORWARD));
        binding.ivRight.setOnClickListener((View view) -> startExecution(RIGHT));
        binding.ivBottom.setOnClickListener((View view) -> startExecution(BACKWARD));
        binding.ivStartStop.setOnClickListener((View view) -> startExecution(STOP));
    }

    private void startExecution(Command toExecute){

        if(isProcessing) return;
        if(!isConnected){
            Utility.showSafeToast(this,"Connect first");
            return;
        }

        showOrHideProgress(true);

        updateBackground(toExecute);


        showOrHideProgress(true);
        saveCommand(toExecute, error -> {
            showOrHideProgress(false);
            if(error != null){
                Utility.showSafeToast(this,error);
            }
        });

    }

    private void saveCommand(Command command,DatabaseListener listener){
        String id = DataSaver.getInstance(this).getId();


        Map<String,Object> map = new HashMap<>();

        long utc = Instant.now().getEpochSecond();
        map.put("time",utc);
        map.put("command",command.getId());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("commands")
                .child(id).child("my_command");
        ref.setValue(map).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
               listener.onProcessDone(null);
            }
            else{
                listener.onProcessDone("Failed");
            }
        });
    }

    private void updateBackground(Command command){
        if(binding == null) return;
        binding.ivLeft.setBackgroundResource(R.drawable.shadow_up_ripple);
        binding.ivTop.setBackgroundResource(R.drawable.shadow_up_ripple);
        binding.ivRight.setBackgroundResource(R.drawable.shadow_up_ripple);
        binding.ivBottom.setBackgroundResource(R.drawable.shadow_up_ripple);

        binding.ivStartStop.setImageResource(R.drawable.baseline_pause_24);

        if(command == LEFT)
            binding.ivLeft.setBackgroundResource(R.drawable.shadow_up_selected_ripple);
        if(command == FORWARD)
            binding.ivTop.setBackgroundResource(R.drawable.shadow_up_selected_ripple);

        if(command == RIGHT)
            binding.ivRight.setBackgroundResource(R.drawable.shadow_up_selected_ripple);
        if(command == BACKWARD)
            binding.ivBottom.setBackgroundResource(R.drawable.shadow_up_selected_ripple);

        if(command == STOP)
            binding.ivStartStop.setImageResource(R.drawable.baseline_play_arrow_24);
    }

    private void switchPowerMode() {
        if(isProcessing) return;

        binding.myProgress.startProgress();
        showOrHideProgress(true);
        if (isConnected) { // will disconnect
            binding.ivPower.setImageResource(R.drawable.baseline_power_settings_new_24);
            // disconnect function
            showOrHideProgress(false);
            binding.myProgress.hideView();
            prevCommand = SHUT_DOWN;
            isConnected = false;
        }
        else { // will connect
            if(DataSaver.getInstance(this).isIDPassNotSet()){
                takeInputAndContinue();
            }
            else {
                showOrHideProgress(true);
                String id = DataSaver.getInstance(this).getId();
                String pass = DataSaver.getInstance(this).getMyPass();
                checkIDPass(id, pass, error -> {
                    binding.myProgress.resetProgress();
                    hideConnectionView(true);
                    showOrHideProgress(false);
                    if(error == null){ // successful
                        Utility.showSafeToast(this,"Login successful");
                        binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                        DataSaver.getInstance(this).saveIdPass(id,pass);
                        prevCommand = CONNECT;
                    }
                    else{
                        Utility.showSafeToast(this,error);
                    }
                    isConnected = (error == null);
                });
                //binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                //connect function
                //showOrHideProgress(false);
                //prevCommand = CONNECT;
            }
        }

    }

    private void takeInputAndContinue(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.id_pass_taker_layout);

        Window window = dialog.getWindow();
        if(window != null){
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText editTextID, editTextPass;

        editTextID = dialog.findViewById(R.id.editTextID);
        editTextPass = dialog.findViewById(R.id.editTextPass);

        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        Button buttonContinue = dialog.findViewById(R.id.buttonContinue);

        buttonCancel.setOnClickListener(view -> {
            showOrHideProgress(false);
            dialog.dismiss();
        });

        buttonContinue.setOnClickListener(view -> {
            String id = String.valueOf(editTextID.getText()).trim();
            String pass = String.valueOf(editTextPass.getText()).trim();
            if(id.isEmpty() || pass.isEmpty()) return;

            showOrHideProgress(true);
            dialog.dismiss();
            checkIDPass(id, pass, error -> {
                showOrHideProgress(false);
                binding.myProgress.resetProgress();
                hideConnectionView(true);
                if(error == null){ // successful
                    Utility.showSafeToast(this,"Login successful");
                    binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                    DataSaver.getInstance(this).saveIdPass(id,pass);
                    prevCommand = CONNECT;
                }
                else{
                    Utility.showSafeToast(this,error);
                }
                isConnected = (error == null);
            });
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void checkIDPass(String id, String pass, DatabaseListener listener){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("commands").child(id);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String originalPass = null;
                if(snapshot.exists()){
                    originalPass = String.valueOf(snapshot.child("pass").getValue());
                }

                if(pass.equals(originalPass)){
                    listener.onProcessDone(null);
                }
                else{
                    listener.onProcessDone("Wrong password. Re-enter again");
                    DataSaver.getInstance(MainActivity.this).clearIdPass();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onProcessDone(error.getMessage());
            }
        });

    }

    private void hideConnectionView(boolean shouldHide){
        ObjectAnimator animator;
        if(shouldHide){ // will hide
            animator = ObjectAnimator.ofFloat(binding.rlConnection,View.Y,
                    -binding.rlConnection.getHeight()-20f);
        }
        else{ // will show
            animator = ObjectAnimator.ofFloat(binding.rlConnection,View.Y,
                    0);
        }
        animator.setDuration(1500);
        animator.start();
    }

    private void showOrHideProgress(boolean show){
        if(binding == null) return;

//        if(show){
//            binding.progressBar.setVisibility(View.VISIBLE);
//        }
//        else{
//            binding.progressBar.setVisibility(View.INVISIBLE);
//        }
        isProcessing = show;
    }

}
