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
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.wheelcontroller.classes.DataSaver;
import com.example.wheelcontroller.classes.Utility;
import com.example.wheelcontroller.databinding.ActivityMainBinding;
import com.example.wheelcontroller.enums.Command;
import com.example.wheelcontroller.listener.CommandListener;
import com.example.wheelcontroller.listener.DatabaseListener;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageView ivPlayPause;

    private ActivityMainBinding binding = null;
    private boolean isConnected = false;
    private boolean isProcessing = false;
    private Command prevCommand = null;
    @SuppressWarnings("deprecation")
    private SimpleExoPlayer simpleExoPlayer = null;
    @SuppressWarnings("deprecation")
    private Player.Listener videoListener = null;

    private boolean isDoublePressedOnceWithinTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeVideoListener();
        initializeViews();
        setClickListener();
    }

    private void initializeViews(){
        ivPlayPause = findViewById(R.id.ivPlayPause);
    }

    private void setClickListener() {
        binding.ivPower.setOnClickListener((View view) -> switchPowerMode());
        binding.buttonShowConnection.setOnClickListener((View v) -> hideConnectionView(false));

        binding.llLeft.setOnClickListener((View view) -> startExecution(LEFT));
        binding.llTop.setOnClickListener((View view) -> startExecution(FORWARD));
        binding.llRight.setOnClickListener((View view) -> startExecution(RIGHT));
        binding.llBottom.setOnClickListener((View view) -> startExecution(BACKWARD));
        binding.ivStartStop.setOnClickListener((View view) -> startExecution(STOP));

        // video player
        ivPlayPause.setOnClickListener((View v)->{
            if(simpleExoPlayer.isPlaying()){
                simpleExoPlayer.setPlayWhenReady(false);
                ivPlayPause.setImageResource(R.drawable.ic_video_play_circle_filled_24);
                binding.tvStartStop.setText(getString(R.string.start));
            }
            else{
                simpleExoPlayer.setPlayWhenReady(true);
                ivPlayPause.setImageResource(R.drawable.ic_video_pause_24);
                binding.tvStartStop.setText(getString(R.string.stop));
            }

        });

        //
        binding.rlGesture.setOnClickListener(v -> hideConnectionView(true));

    }

    @SuppressWarnings("deprecation")
    private void initializeVideoListener(){
        videoListener = new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Player.Listener.super.onPlayerStateChanged(playWhenReady, playbackState);
                if(playbackState == simpleExoPlayer.STATE_READY){
                    binding.myProgressVideo.setVisibility(View.GONE);
                    binding.exoPlayer.setVisibility(View.VISIBLE);
                }
            }
        };
    }


    @SuppressWarnings("deprecation")
    private void startPlayer() {

        binding.myProgressVideo.setVisibility(View.VISIBLE);
        binding.exoPlayer.setVisibility(View.INVISIBLE);

        if(simpleExoPlayer == null) {
            simpleExoPlayer = new SimpleExoPlayer.Builder(this).build();
            binding.exoPlayer.setKeepScreenOn(true);
            binding.exoPlayer.setPlayer(simpleExoPlayer);
            simpleExoPlayer.addListener(videoListener);
        }

        String url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

        MediaSource mediaSource = getMediaSource( Uri.parse(url) );
        simpleExoPlayer.prepare(mediaSource);
        simpleExoPlayer.setPlayWhenReady(true);
    }

    @SuppressWarnings("deprecation")
    private MediaSource getMediaSource(Uri uri){
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this,"exoplayer"));

        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        return new ProgressiveMediaSource.Factory(dataSourceFactory,extractorsFactory).createMediaSource(MediaItem.fromUri(uri));
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

    private void saveCommand(Command command, CommandListener listener){
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
        binding.llLeft.setBackgroundResource(R.drawable.direction_background);
        binding.llTop.setBackgroundResource(R.drawable.direction_background);
        binding.llRight.setBackgroundResource(R.drawable.direction_background);
        binding.llBottom.setBackgroundResource(R.drawable.direction_background);

        binding.ivStartStop.setImageResource(R.drawable.baseline_pause_24);

        if(command == LEFT)
            binding.llLeft.setBackgroundResource(R.drawable.shadow_up_selected_ripple);
        if(command == FORWARD)
            binding.llTop.setBackgroundResource(R.drawable.shadow_up_selected_ripple);

        if(command == RIGHT)
            binding.llRight.setBackgroundResource(R.drawable.shadow_up_selected_ripple);
        if(command == BACKWARD)
            binding.llBottom.setBackgroundResource(R.drawable.shadow_up_selected_ripple);

        if(command == STOP)
            binding.ivStartStop.setImageResource(R.drawable.baseline_play_arrow_24);
    }

    private void switchPowerMode() {
        if(isProcessing) return;

        if (isConnected) { // will disconnect
            binding.rlGesture.setVisibility(View.GONE);
            binding.myProgress.startProgress();
            binding.ivPower.setImageResource(R.drawable.baseline_power_settings_new_24);

            showOrHideProgress(true);
            // disconnect function
            showOrHideProgress(false);

            binding.tvConnectionStatus.setText(getString(R.string.not_connected));

            binding.myProgress.hideView();
            prevCommand = SHUT_DOWN;
            isConnected = false;
        }
        else { // will connect
            if(DataSaver.getInstance(this).isIDPassNotSet()){
                takeInputAndContinue();
            }
            else {
                binding.myProgress.startProgress();
                showOrHideProgress(true);

                String id = DataSaver.getInstance(this).getId();
                String pass = DataSaver.getInstance(this).getMyPass();

                checkIDPass(id, pass, (error,name) -> {
                    showOrHideProgress(false);
                    if(error == null){ // successful
                        binding.rlGesture.setVisibility(View.VISIBLE);
                        binding.myProgress.resetProgress();
                        hideConnectionView(true);
                        startPlayer();
                        binding.tvConnectionStatus.setText(getString(R.string.connected));
                        Utility.showSafeToast(this,"Login successful");
                        binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                        DataSaver.getInstance(this).saveIdPass(id,pass,name);
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
            window.setWindowAnimations(R.style.dialogAnimation);
            window.setBackgroundDrawable(new ColorDrawable(0));
        }

        ProgressBar progressBar = dialog.findViewById(R.id.progressBarDialog);
        TextView tvMessage = dialog.findViewById(R.id.tvMessageDialog);

        EditText editTextID = dialog.findViewById(R.id.editTextID);
        EditText editTextPass = dialog.findViewById(R.id.editTextPass);

        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        Button buttonContinue = dialog.findViewById(R.id.buttonContinue);

        boolean[] isProcessing = {false};

        buttonCancel.setOnClickListener(view -> dialog.dismiss());

        buttonContinue.setOnClickListener(view -> {

            if(isProcessing[0]) return;

            String id = String.valueOf(editTextID.getText()).trim();
            String pass = String.valueOf(editTextPass.getText()).trim();
            if(id.isEmpty() || pass.isEmpty()) return;

            isProcessing[0] = true;
            showOrHideProgress(true);
            progressBar.setVisibility(View.VISIBLE);
            binding.tvID.setText(getString(R.string.logging_in));

            checkIDPass(id, pass, (error,name) -> {
                isProcessing[0] = false;

                showOrHideProgress(false);
                isConnected = (error == null);

                if(error == null){ // successful

                    dialog.dismiss();
                    binding.rlGesture.setVisibility(View.VISIBLE);
                    binding.myProgress.resetProgress();
                    hideConnectionView(true);
                    startPlayer();
                    binding.tvConnectionStatus.setText(getString(R.string.connected));

                    Utility.showSafeToast(this,"Login successful");

                    binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                    DataSaver.getInstance(this).saveIdPass(id,pass,name);
                    prevCommand = CONNECT;
                }
                else{
                    progressBar.setVisibility(View.INVISIBLE);
                    tvMessage.setText(error);
                }

            });
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void checkIDPass(String id, String pass, DatabaseListener listener){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child(id);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String originalPass = null;
                String name = null;
                if(snapshot.exists()){
                    originalPass = String.valueOf(snapshot.child("password").getValue());
                    name = String.valueOf(snapshot.child("name").getValue());
                }

                if(pass.equals(originalPass)){
                    listener.onProcessDone(null,name);
                    binding.tvID.setText(name);
                }
                else{
                    listener.onProcessDone("Wrong password. Re-enter again",null);
                    DataSaver.getInstance(MainActivity.this).clearIdPass();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onProcessDone(error.getMessage(),null);
            }
        });

    }

    private void hideConnectionView(boolean shouldHide){
        ObjectAnimator animator;
        if(shouldHide){ // will hide
            animator = ObjectAnimator.ofFloat(binding.rlConnection,View.Y,
                    -binding.rlConnection.getHeight()-20f);
            binding.clRoot.setVisibility(View.VISIBLE);
            animator.setDuration(1500);
        }
        else{ // will show
            animator = ObjectAnimator.ofFloat(binding.rlConnection,View.Y,
                    0);
            animator.setDuration(750);
        }
        animator.start();
    }

    private void showOrHideProgress(boolean show){
        if(binding == null) return;
        isProcessing = show;
    }

    @Override
    public void onBackPressed() {
        if(isDoublePressedOnceWithinTime){
            super.onBackPressed();
        }
        else{
            Utility.showSafeToast(this,"Press again to exit");
            isDoublePressedOnceWithinTime = true;
            new Handler(Looper.getMainLooper()).postDelayed(() -> isDoublePressedOnceWithinTime = false,2000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        simpleExoPlayer.release();
    }

}
