package com.example.wheelcontroller.enums;

public enum Command {
    LEFT(1), RIGHT(2), FORWARD(3), BACKWARD(4), STOP(5), SHUT_DOWN(6), CONNECT(7);
    private final int id;

    Command(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
