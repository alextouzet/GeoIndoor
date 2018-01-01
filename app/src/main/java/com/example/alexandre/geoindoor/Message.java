package com.example.alexandre.geoindoor;
public class Message {


    //public Boolean asked;
    public String receiver, sender, title, message, lamp = "", latitude = "", longitude = "", asked = "";

    public Message(String receiver, String sender, String title, String message) {
        this.asked = "false";
        this.receiver = receiver;
        this.sender = sender;
        this.title = title;
        this.message = message;
    }

    public Message(String receiver, String sender, String title, String message, String lamp, Double latitude, Double longitude) {
        this.asked = "true";
        this.receiver = receiver;
        this.sender = sender;
        this.title = title;
        this.message = message;
        this.lamp = lamp;
        this.longitude = longitude.toString();
        this.latitude = latitude.toString();
    }
}