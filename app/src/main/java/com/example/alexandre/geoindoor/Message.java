package com.example.alexandre.geoindoor;
public class Message {
    public String receiver, sender, title, message;

    public Message(String receiver, String sender, String title, String message) {
        this.receiver = receiver;
        this.sender = sender;
        this.title = title;
        this.message = message;
    }
}