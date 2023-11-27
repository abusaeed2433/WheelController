package com.example.wheelcontroller.classes;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient {
    //private static final String WEBSOCKET_URL = "ws://27.147.190.170:8000";
    private static final String WEBSOCKET_URL = "ws://192.168.29.150:8000";
    private WebSocket webSocket = null;

    public WebSocketClient() {
        createClientObject();
    }

    public void createClientObject() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(WEBSOCKET_URL)
                .build();

        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                System.out.println("WebSocket opened");
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                System.out.println("Received message: " + text);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
                System.out.println("Received bytes: " + bytes.hex());
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                System.out.println("WebSocket closing: " + code + " " + reason);
                createClientObject();
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                System.out.println("WebSocket closed: " + code + " " + reason);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                System.out.println("WebSocket failure: " + t.getMessage());
            }
        };

        webSocket = client.newWebSocket(request, webSocketListener);
    }

    public void sendMessage(String message){
        if(webSocket == null) return;
        webSocket.send(message);
    }
}
