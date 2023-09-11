package com.example.wheelcontroller;

import static com.example.wheelcontroller.enums.Command.BACKWARD;
import static com.example.wheelcontroller.enums.Command.CONNECT;
import static com.example.wheelcontroller.enums.Command.FORWARD;
import static com.example.wheelcontroller.enums.Command.LEFT;
import static com.example.wheelcontroller.enums.Command.RIGHT;
import static com.example.wheelcontroller.enums.Command.SHUT_DOWN;
import static com.example.wheelcontroller.enums.Command.STOP;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.example.wheelcontroller.classes.DataSaver;
import com.example.wheelcontroller.classes.Utility;
import com.example.wheelcontroller.databinding.ActivityMainBinding;
import com.example.wheelcontroller.enums.Command;

import org.jetbrains.annotations.TestOnly;

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
        binding.ivPower.setOnClickListener((View view) -> switchPowerMode(isConnected));

        binding.ivLeft.setOnClickListener((View view) -> startExecution(LEFT));
        binding.ivTop.setOnClickListener((View view) -> startExecution(FORWARD));
        binding.ivRight.setOnClickListener((View view) -> startExecution(RIGHT));
        binding.ivBottom.setOnClickListener((View view) -> startExecution(BACKWARD));
        binding.ivStartStop.setOnClickListener((View view) -> startExecution(STOP));
    }

    private void startExecution(Command toExecute){
        if(isProcessing) return;
        showOrHideProgress(true);

        updateBackground(toExecute);




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

    private void switchPowerMode(boolean curConnected) {
        if(isProcessing) return;
        showOrHideProgress(true);
        if (curConnected) { // will disconnect
            binding.ivPower.setImageResource(R.drawable.baseline_power_settings_new_24);
            // disconnect function
            showOrHideProgress(false);
            prevCommand = SHUT_DOWN;
        }
        else { // will connect
            if(DataSaver.getInstance(this).isIDPassNotSet()){
                takeInputAndContinue();
            }
            else {
                binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                //connect function
                showOrHideProgress(false);
                prevCommand = CONNECT;
            }
        }
        isConnected = !curConnected;
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

            dialog.dismiss();
            checkIDPass(id,pass);
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void checkIDPass(String id, String pass){
        //check if database
        // if successful then

        Utility.showSafeToast(this,"Login successful");
        binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
        showOrHideProgress(false);
        DataSaver.getInstance(this).saveIdPass(id,pass);
        prevCommand = CONNECT;

    }


    private void showOrHideProgress(boolean show){
        if(binding == null) return;

        if(show){
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
        isProcessing = show;
    }

}
