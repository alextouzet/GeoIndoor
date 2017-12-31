package com.example.alexandre.geoindoor;
public class Message {


    public boolean asked;
    public String receiver, sender, title, message, lamp;
    public double latitude, longitude;

    public Message(String receiver, String sender, String title, String message) {
        this.asked = false;
        this.receiver = receiver;
        this.sender = sender;
        this.title = title;
        this.message = message;
    }

    public Message(String receiver, String sender, String title, String message, String lamp, double latitude, double longitude) {
        this.asked = true;
        this.receiver = receiver;
        this.sender = sender;
        this.title = title;
        this.message = message;
        this.lamp = lamp;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}