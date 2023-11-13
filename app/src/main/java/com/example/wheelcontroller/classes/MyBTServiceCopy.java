package com.example.wheelcontroller.classes;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class MyBTServiceCopy {
    private static final String TAG = "MY_APP_DEBUG_TAG";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ConnectedThread connectedThread = null;
    private MessageListener messageListener = null;

    interface MessageListener{
        void onMessageReceived(String message);
    }
    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;
    }

    public MyBTServiceCopy(BluetoothServerSocket socket){
        AcceptThread thread = new AcceptThread(socket); // server
        thread.start();
//        thread = new ConnectedThread(socket);
        //thread.start();
    }

    public MyBTServiceCopy(BluetoothDevice device, MessageListener listener){
        ConnectThread thread = new ConnectThread(device);
        thread.start();
        this.messageListener = listener;
    }

    public void write(String s){
        if(connectedThread != null) {
            connectedThread.write(s.getBytes());
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(BluetoothServerSocket socket) {
            mmServerSocket = socket;
        }

        public void run() {
            BluetoothSocket socket;

            while (true) {
                try { socket = mmServerSocket.accept(); } catch (IOException e) { break; }

                if (socket != null) {
                    connectedThread = new ConnectedThread(socket);
                    try { mmServerSocket.close(); } catch (IOException e) { throw new RuntimeException(e); }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];

            while (true) {
                try {
                    int numBytes = mmInStream.read(mmBuffer);
                    String writeMessage = new String(mmBuffer,0, numBytes, StandardCharsets.UTF_8);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if(messageListener == null) return;
                        messageListener.onMessageReceived(writeMessage);
                    });
                    Thread.sleep(3000);
                }
                catch (IOException | InterruptedException e) {
                    Log.d(TAG, "Input stream was disconnected", e);

                    try{
                        Thread.sleep(2000);

                    }catch (InterruptedException ignored){
                        System.out.println("Slept");
                    }
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public synchronized void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                System.out.println("Message sent "+ Arrays.toString(bytes));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                System.out.println("Successfully created");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            try {
                mmSocket.connect();
                System.out.println("Socket connected");
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

}
