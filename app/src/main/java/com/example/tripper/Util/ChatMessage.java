package com.example.tripper.Util;

public class ChatMessage {

    private String from;
    private String message;

    public ChatMessage() {}

    public ChatMessage(String from, String message) {
        this.from = from;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
