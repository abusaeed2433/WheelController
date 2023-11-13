package com.example.wheelcontroller.classes;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyBTService {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    //private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private OutputStream mmOutStream = null;

//    public MyBTService() {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        // Replace "YourDeviceName" with the name of your Rock Pi Bluetooth device
//        bluetoothDevice = bluetoothAdapter.getRemoteDevice("08:FB:EA:2B:96:E9");
//        ConnectThread thread = new ConnectThread(bluetoothDevice);
//        thread.start();
//        //this.messageListener = listener;
//    }
    private ConnectThread thread;

    public MyBTService(BluetoothDevice device, MyBTServiceCopy.MessageListener listener){
        thread = new ConnectThread(device);
        //thread.start();
        //this.messageListener = listener;
    }

    public boolean isBTEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    public BluetoothAdapter getBluetoothAdapter(){
        return bluetoothAdapter;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
                System.out.println("Successfully created");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            this.start();
        }

        @SuppressLint("MissingPermission")
        public void run() {
            try {
                mmSocket.connect();
                System.out.println("Socket created");
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("TAG", "Could not close the client socket", closeException);
                }
                return;
            }

            OutputStream tmpOut = null;

            try {
                tmpOut = mmSocket.getOutputStream();
                System.out.println("Output stream ready");
            } catch (IOException e) {
                Log.e("TAG", "Error occurred when creating output stream", e);
            }
            mmOutStream = tmpOut;
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the client socket", e);
            }
        }
    }
    @SuppressLint("MissingPermission")
    public void sendMessage(String message){
        executorService.execute(() -> {

            BluetoothSocket bluetoothSocket = null;
            OutputStream outputStream = null;
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();

                System.out.println("Connected");
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
