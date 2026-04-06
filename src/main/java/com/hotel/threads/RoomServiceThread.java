package com.hotel.threads;

import javafx.application.Platform;
import java.util.function.Consumer;

public class RoomServiceThread implements Runnable {
    private int roomNumber;
    private Consumer<String> updateMessageCallback;

    public RoomServiceThread(int roomNumber, Consumer<String> updateMessageCallback) {
        this.roomNumber = roomNumber;
        this.updateMessageCallback = updateMessageCallback;
    }

    @Override
    public void run() {
        try {
            Platform.runLater(() -> updateMessageCallback.accept("Preparing room service for Room " + roomNumber + "..."));
            com.hotel.util.FileHandler.logRoomService("Preparing room service for Room " + roomNumber + "...");
            Thread.sleep(2000); // Wait 2s
            
            Platform.runLater(() -> updateMessageCallback.accept("Delivering to Room " + roomNumber + "..."));
            com.hotel.util.FileHandler.logRoomService("Delivering to Room " + roomNumber + "...");
            Thread.sleep(3000); // Wait 3s
            
            Platform.runLater(() -> updateMessageCallback.accept("Room service delivered to Room " + roomNumber + "."));
            com.hotel.util.FileHandler.logRoomService("Room service delivered to Room " + roomNumber + ".");
            
        } catch (InterruptedException e) {
            Platform.runLater(() -> updateMessageCallback.accept("Room service interrupted for Room " + roomNumber + "."));
            com.hotel.util.FileHandler.logRoomService("Room service interrupted for Room " + roomNumber + ".");
        }
    }
}
