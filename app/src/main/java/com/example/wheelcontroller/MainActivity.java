package com.example.wheelcontroller;

import static com.example.wheelcontroller.enums.Command.BACKWARD;
import static com.example.wheelcontroller.enums.Command.FORWARD;
import static com.example.wheelcontroller.enums.Command.LEFT;
import static com.example.wheelcontroller.enums.Command.RIGHT;
import static com.example.wheelcontroller.enums.Command.STOP;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.wheelcontroller.classes.BluetoothConnector;
import com.example.wheelcontroller.classes.DataSaver;
import com.example.wheelcontroller.classes.EachLog;
import com.example.wheelcontroller.classes.LogAdapter;
import com.example.wheelcontroller.classes.Utility;
import com.example.wheelcontroller.classes.WebSocketClient;
import com.example.wheelcontroller.databinding.ActivityMainBinding;
import com.example.wheelcontroller.enums.Command;
import com.example.wheelcontroller.listener.CommandListener;
import com.example.wheelcontroller.listener.DatabaseListener;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding = null;
    private ImageView ivPlayPause;
    private boolean isConnected = false;
    private boolean isProcessing = false;
    @SuppressWarnings("deprecation")
    private SimpleExoPlayer simpleExoPlayer = null;
    @SuppressWarnings("deprecation")
    private Player.Listener videoListener = null;
    private boolean isDoublePressedOnceWithinTime = false;
    private DatabaseReference historyRef = null;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isSpeechStarting = false;
    //private final List<String> VOICE_COMMANDS = Arrays.asList("go left","go right","go forward","go backward","terminate");
    private final List<String> VOICE_COMMANDS = Arrays.asList("বামে যাও","ডানে যাও","সামনে যাও","পিছনে যাও","থামো");
    private final List<String> COMMANDS_MESSAGE  = Arrays.asList("Moving left", "Moving forward", "Moving right", "Moving backward", "Not moving" );
    private boolean stopSpeechInput = false;

    //private MyBTService myBTService = null;
    private BluetoothConnector bluetoothConnector = null;
    private ActivityResultLauncher<Intent> btLauncher = null;
    private boolean isBTConnected = false;
    private final List<EachLog> allLogs = new ArrayList<>();
    private LogAdapter logAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeVideoListener();
        initializeViews();
        setClickListener();

        initializeReferences();
        initializeSpeechPart();
        initBTLauncher();

        initLogAdapter();
        readLogsIfPossible();
    }

    private void initLogAdapter(){
        logAdapter = new LogAdapter(this,allLogs);
        binding.rvLogs.setAdapter(logAdapter);
    }

    private void readLogsIfPossible(){
        String id = DataSaver.getInstance(this).getId();

        DatabaseReference logRef = FirebaseDatabase.getInstance().getReference()
                .child("chats").child(id).child("rock/logs");
        logRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                String type = String.valueOf(snapshot.child("type").getValue());
                String message = String.valueOf(snapshot.child("message").getValue());
                String ts = String.valueOf(snapshot.child("timestamp").getValue());

                String timestamp = "-- -- ---";
                try{
                    long realTs = (long)(Double.parseDouble(ts) * 1000);
                    timestamp = getFormattedTS(realTs);
                }catch (Exception ignored){}

                EachLog log = new EachLog(type,message,timestamp);
                updateLogInAdapter(log);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initializeViews(){
        ivPlayPause = findViewById(R.id.ivPlayPause);
    }

    private void setClickListener() {
        binding.ivPower.setOnClickListener((View view) -> requestPowerSwitch());
        binding.buttonShowConnection.setOnClickListener((View v) -> hideConnectionView(false));

        binding.llLeft.setOnClickListener((View view) -> startExecution(LEFT));
        binding.llTop.setOnClickListener((View view) -> startExecution(FORWARD));
        binding.llRight.setOnClickListener((View view) -> startExecution(RIGHT));
        binding.llBottom.setOnClickListener((View view) -> startExecution(BACKWARD));
        binding.llStartStop.setOnClickListener((View view) -> startExecution(STOP));

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

        // speech
        binding.ivSpeech.setOnClickListener((View v)-> startAudio());
        binding.llSpeechRunning.setOnClickListener((View v) -> stopAudio());

    }

    private void initBTLauncher() {
        btLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(this, "Ready to connect", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @SuppressWarnings("deprecation")
    private void initializeVideoListener(){
        videoListener = new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Player.Listener.super.onPlayerStateChanged(playWhenReady, playbackState);
                if(playbackState == simpleExoPlayer.STATE_READY){
                    //todo
                    //binding.myProgressLog.setVisibility(View.GONE);
                    //binding.exoPlayer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                Player.Listener.super.onPlayWhenReadyChanged(playWhenReady, reason);
                //todo
//                binding.rvLogs.setVisibility(View.GONE);
//                binding.exoPlayer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                //Player.Listener.super.onPlayerError(error);
                error.printStackTrace();
//                binding.rvLogs.setVisibility(View.VISIBLE);
//                binding.exoPlayer.setVisibility(View.GONE);
            }
        };
    }

    private void initializeReferences(){
        historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    }

    @SuppressWarnings("deprecation")
    private void startPlayer() {

        //binding.myProgressLog.setVisibility(View.VISIBLE);
        //binding.exoPlayer.setVisibility(View.INVISIBLE);

        if(simpleExoPlayer == null) {
            simpleExoPlayer = new SimpleExoPlayer.Builder(this).build();
            binding.exoPlayer.setKeepScreenOn(true);
            binding.exoPlayer.setPlayer(simpleExoPlayer);
            simpleExoPlayer.addListener(videoListener);
        }

        //String url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
        //String url = "http://192.168.29.150:8081/";
        String url = "https://open-lately-muskox.ngrok-free.app";

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
        if(isProcessing || toExecute == null) return;
        if(!isConnected){
            Utility.showSafeToast(this,"Connect first");
            return;
        }

        showOrHideProgress(true);

        updateBackgroundAndMessage(toExecute);

        if(toExecute == STOP){
            binding.tvStartStop.setText(getString(R.string.stopped));
        }
        else{
            binding.tvStartStop.setText(getString(R.string.running));
        }

        showOrHideProgress(true);
        sendInBluetooth(toExecute);
        saveCommand(toExecute, error -> {
            showOrHideProgress(false);
            if(error == null) {
                showMessageText(toExecute);
            }
            else{
                Utility.showSafeToast(this,error);
            }
        });

    }

    private void sendInBluetooth(Command toExecute){
        bluetoothConnector.sendData(toExecute.getId()+"");
        sendViaSocket(toExecute.getId()+"");
    }

    private synchronized void saveCommand(Command command, CommandListener listener){
        if(isBTConnected) {
            listener.onProcessDone(null);
            return;
        }

        String id = DataSaver.getInstance(this).getId();

        Map<String,Object> map = new HashMap<>();

        long utc = Instant.now().getEpochSecond();
        map.put("time",utc);
        map.put("command",command.getId());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("commands")
                .child(id).child("my_command");
        ref.setValue(map).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                saveHistory(command);
                listener.onProcessDone(null);
            }
            else{
                listener.onProcessDone("Failed");
            }
        });
    }

    private synchronized void saveHistory(Command command){
        Map<String,Object> map = new HashMap<>();

        long utc = Instant.now().getEpochSecond();
        map.put("time",utc);
        map.put("command",command.getId());
        map.put("command_in_text",command.getCommandInText());

        String id = DataSaver.getInstance(this).getId();
        String date = getFormattedDate();
        historyRef.child(id).child(date).push().setValue(map);

    }


    private String getFormattedDate(){
        try {
            LocalDate localDate = LocalDate.now();
            String pattern = "dd-MM-yyyy";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return formatter.format(localDate);
        }catch (Exception ignored){
            return "not_found";
        }

    }

    private void updateBackgroundAndMessage(Command command){
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

        if(command == STOP) {
            binding.ivStartStop.setImageResource(R.drawable.baseline_play_arrow_24);
            binding.tvStartStop.setText(getString(R.string.stopped));
            showMessageText(STOP);
        }
    }

    private void showMessageText(Command command){
        if(binding == null) return;

        String text = null;

        if(command == LEFT) text  = "Moving left";
        else if(command == FORWARD) text = "Moving forward";
        else if(command == RIGHT) text = "Moving right";
        else if(command == BACKWARD) text = "Moving backward";
        else if(command == STOP) text = "Not moving";

        binding.tvMessage.setTextColor(getColor(R.color.black));
        binding.tvMessage.setText(text);
    }


    private void showMessageText(Command command,String defText){
        if(binding == null) return;

        String text = null;

        if(command == LEFT) text  = "Moving left";
        else if(command == FORWARD) text = "Moving forward";
        else if(command == RIGHT) text = "Moving right";
        else if(command == BACKWARD) text = "Moving backward";
        else if(command == STOP) text = "Not moving";

        if(text == null) {
            text = defText;
            binding.tvMessage.setTextColor(getColor(R.color.light_red));
        }
        else{
            binding.tvMessage.setTextColor(getColor(R.color.black));
        }
        binding.tvMessage.setText(text);
    }

    private void requestPowerSwitch(){
        if (isConnected) { // will disconnect
            switchPowerMode();
        }
        else{
            if(isAllPermissionGranted()){

                BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

                if(bluetoothAdapter.isEnabled()) {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice("08:FB:EA:2B:96:E9");
                    bluetoothConnector = new BluetoothConnector(device,true,bluetoothAdapter,null);
                    ExecutorService service = Executors.newSingleThreadExecutor();
                    service.execute(() -> {
                        try {
                            bluetoothConnector.connect();
                            isBTConnected = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    switchPowerMode();
                }
                else {
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    btLauncher.launch(intent);
                }
            }
        }
    }

    private boolean isAllPermissionGranted(){
        if (
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        )
        {
            return true;
        }

        if(
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    },
                    101);
            return false;
        }


        if (
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 101);
            }
            else {
                return true;
            }
        }
        return false;

    }

    private void switchPowerMode() {
        if(isProcessing) return;

        if (isConnected) { // will disconnect
            binding.rlGesture.setVisibility(View.GONE);
            binding.myProgress.startProgress();
            showOrHideProgress(true);

            disconnectAndCleanUp( error ->{
                if(error != null){
                    Utility.showSafeToast(this,"Logged out without cleanup");
                }
                showOrHideProgress(false);
                binding.ivPower.setImageResource(R.drawable.baseline_power_settings_new_24);
                binding.tvConnectionStatus.setText(getString(R.string.not_connected));

                binding.myProgress.hideView();
                isConnected = false;
            });


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
                        updateBackgroundAndMessage(STOP);
                        startPlayer();
                        binding.tvConnectionStatus.setText(getString(R.string.connected));
                        Utility.showSafeToast(this,"Login successful");
                        binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                        DataSaver.getInstance(this).saveIdPass(id,pass,name);
                    }
                    else{
                        Utility.showSafeToast(this,error);
                        binding.myProgress.resetProgress();
                    }
                    isConnected = (error == null);
                });
            }
        }
    }

    private void disconnectAndCleanUp(CommandListener listener){
        String id = DataSaver.getInstance(this).getId();

        DatabaseReference ref = historyRef.child(id).child(getFormattedDate());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                long total = dataSnapshot.getChildrenCount();
                long cur = 0;

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String key = childSnapshot.getKey();

                    if( key == null) continue;
                    if( total - cur > 10 ){
                        ref.child(key).removeValue();
                    }
                    cur++;
                }

                listener.onProcessDone(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onProcessDone("Failed to cleanup");
            }
        });

    }

    private void updateLogInAdapter(EachLog log){
        allLogs.add(log);
        binding.myProgressLog.setVisibility(View.GONE);

        logAdapter.notifyItemInserted(allLogs.size()-1);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
           if(binding == null) return;
           binding.rvLogs.smoothScrollToPosition(allLogs.size()-1);
        },120);

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
                    updateBackgroundAndMessage(STOP);
                    startPlayer();
                    binding.tvConnectionStatus.setText(getString(R.string.connected));

                    Utility.showSafeToast(this,"Login successful");

                    binding.ivPower.setImageResource(R.drawable.baseline_power_active_settings_new_24);
                    DataSaver.getInstance(this).saveIdPass(id,pass,name);
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
                final String[] name = {null};
                if(snapshot.exists()){
                    originalPass = String.valueOf(snapshot.child("password").getValue());
                    name[0] = String.valueOf(snapshot.child("name").getValue());
                }

                if(pass.equals(originalPass)){

                    DatabaseReference activeRef = FirebaseDatabase.getInstance().getReference()
                                    .child("chats").child(id).child("rock/last_active");

                    activeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()) {
                                try {
                                    double lastActive = Double.parseDouble( String.valueOf(snapshot.getValue()) );
                                    double curTimeStamp = Instant.now().toEpochMilli() / 1000f;

                                    if (curTimeStamp - lastActive <= 5000) { // 5s
                                        listener.onProcessDone(null, name[0]);
                                        binding.tvID.setText(name[0]);
                                    }
                                    else {
                                        listener.onProcessDone("Controller is not ready in wheelchair", null);
                                    }
                                }
                                catch (Exception e){
                                    listener.onProcessDone("Something went wrong", null);
                                }
                            }
                            else{
                                listener.onProcessDone("Controller is not ready in wheelchair", null);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            listener.onProcessDone("Something went wrong", null);
                        }
                    });

                }
                else{
                    listener.onProcessDone("Wrong password. Re-enter again",null);
                    DataSaver.getInstance(MainActivity.this).clearIdPass();
                    startActivity(new Intent(MainActivity.this,LoginActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onProcessDone(error.getMessage(),null);
                DataSaver.getInstance(MainActivity.this).clearIdPass();
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

    public static int levenshteinRecursive(String str1, String str2, int m, int n) {
        if (m == 0) return n;

        if (n == 0) return m;

        if (str1.charAt(m - 1) == str2.charAt(n - 1)) {
            return levenshteinRecursive(str1, str2, m - 1, n - 1);
        }

        return 1 + Math.min(
                // Insert
                levenshteinRecursive(str1, str2, m, n - 1),
                Math.min(
                        // Remove
                        levenshteinRecursive(str1, str2, m - 1, n),

                        // Replace
                        levenshteinRecursive(str1, str2, m - 1, n - 1)
                )
        );
    }

    private String getFormattedTS(long ts){

        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());

        String pattern = "EEE MMM dd'\n'hh:mm:ssa";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(ldt).toUpperCase();
    }


    private void processVoiceCommand(List<String> voices){

        if(voices == null || voices.isEmpty()) {
            showMessageText(null,"No command matched");
            return;
        }

        String strCommand = voices.get(0);
        Command realCommand = null;

        //dummy for log

        String timestamp =  getFormattedTS(System.currentTimeMillis());
        EachLog log = new EachLog("L","Voice command taken through app "+strCommand,timestamp);
        updateLogInAdapter(log);
        //dummy for log above


        Pair<Integer,Integer> minPoint = new Pair<>(Integer.MAX_VALUE,-1); // minDif, index

        for(int i=0; i < VOICE_COMMANDS.size(); i++) {

            String definedCommand = VOICE_COMMANDS.get(i);

            int minDif = Integer.MAX_VALUE;

            for(String cmd : voices) {
                cmd = cmd.toLowerCase().trim();
                minDif = Math.min( minDif,levenshteinRecursive(definedCommand, cmd, definedCommand.length(), cmd.length()) );
            }

            if( minDif < minPoint.first ){
                minPoint = new Pair<>( minDif , i );
            }
        }

        if( minPoint.second != -1 ){
            strCommand = COMMANDS_MESSAGE.get( minPoint.second );
            realCommand = getRealCommand( minPoint.second );
        }

        startExecution(realCommand);
        showMessageText(realCommand,strCommand);
    }


    private Command getRealCommand(int index){
        index++;

        if(index == 1) return LEFT;
        if(index == 2) return RIGHT;
        if(index == 3) return FORWARD;
        if(index == 4) return BACKWARD;

        return STOP;
    }

    private void stopAudio(){
        if(isSpeechStarting) {
            Utility.showSafeToast(this,getString(R.string.please_wait));
            return;
        }

        stopSpeechInput = true;
        speechRecognizer.stopListening();
        binding.llSpeechRunning.setVisibility(View.INVISIBLE);
        binding.pbSpeech.setVisibility(View.INVISIBLE);
        binding.ivSpeech.setVisibility(View.VISIBLE);
    }

    private void startAudio(){

        if(isSpeechStarting) {
            Utility.showSafeToast(this,getString(R.string.already_running));
            return;
        }
        isSpeechStarting = true;

        stopSpeechInput = false;
        binding.ivSpeech.setVisibility(View.INVISIBLE);
        binding.pbSpeech.setVisibility(View.VISIBLE);
        speechRecognizer.startListening(speechIntent);
    }

    private void initializeSpeechPart(){

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Utility.showSafeToast(this,"Please allow microphone permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn_IN");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isSpeechStarting = false;
                binding.pbSpeech.setVisibility(View.INVISIBLE);
                binding.llSpeechRunning.setVisibility(View.VISIBLE);
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                if(!stopSpeechInput)
                    speechRecognizer.startListening(speechIntent);
            }

            @Override
            public void onError(int error) {
                if(!stopSpeechInput)
                    speechRecognizer.startListening(speechIntent);
            }

            @Override
            public void onResults(Bundle results) {
                if(stopSpeechInput) return;

                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                processVoiceCommand(data);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSpeechPart();
            } else {
                Utility.showSafeToast(this,"Permission denied. Can't use audio command");
            }
        }
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

    private WebSocketClient mWebSocketClient = null;
    private void sendViaSocket(String message) {
        if(mWebSocketClient == null){
            mWebSocketClient = new WebSocketClient();
        }
        mWebSocketClient.sendMessage(message);
    }

}
