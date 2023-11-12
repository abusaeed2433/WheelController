package com.example.wheelcontroller.classes;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyBTService {
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothDevice bluetoothDevice;

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    public MyBTService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Replace "YourDeviceName" with the name of your Rock Pi Bluetooth device
        bluetoothDevice = bluetoothAdapter.getRemoteDevice("08:FB:EA:2B:96:E9");
    }

    public boolean isBTEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    public void sendMessage(String message){
        executorService.execute(() -> {

            BluetoothSocket bluetoothSocket = null;
            OutputStream outputStream = null;
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(message.getBytes());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {

                    if (outputStream != null) outputStream.close();

                    if (bluetoothSocket != null) bluetoothSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
