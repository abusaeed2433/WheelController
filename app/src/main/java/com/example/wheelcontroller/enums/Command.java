package com.example.wheelcontroller.enums;

public enum Command {
    LEFT(1), RIGHT(2), FORWARD(3), BACKWARD(4), STOP(5), SHUT_DOWN(6), CONNECT(7);
    private final int id;
    private final String commandInText;

    Command(int id) {
        this.id = id;
        if(id == 1) commandInText = "LEFT";
        else if(id == 2) commandInText = "RIGHT";
        else if(id == 3) commandInText = "FORWARD";
        else if(id == 4) commandInText = "BACKWARD";
        else if(id == 5) commandInText = "STOP";
        else if(id == 6) commandInText = "CONNECT";
        else if(id == 7) commandInText = "SHUT_DOWN";
        else commandInText = "UNKNOWN";
    }

    public int getId() {
        return id;
    }

    public String getCommandInText() {
        return commandInText;
    }
}
