package com.hotel.threads;

import com.hotel.model.Room;
import com.hotel.service.RoomService;

import javafx.application.Platform;

public class CleaningThread extends Thread {
    private Room room;
    private RoomService roomService;
    private Runnable guiCallback;

    public CleaningThread(Room room, RoomService roomService, Runnable guiCallback) {
        this.room = room;
        this.roomService = roomService;
        this.guiCallback = guiCallback;
    }

    @Override
    public void run() {
        try {
            System.out.println("Started cleaning room " + room.getRoomNumber() + "...");
            com.hotel.util.FileHandler.logCleaning("Started cleaning room " + room.getRoomNumber() + "...");
            Thread.sleep(5000); // Simulate cleaning taking 5 seconds
            
            // Mark room available after sleep
            room.setAvailable(true);
            roomService.saveRooms();
            System.out.println("Finished cleaning room " + room.getRoomNumber() + ". It is now available.");
            com.hotel.util.FileHandler.logCleaning("Finished cleaning room " + room.getRoomNumber() + ". It is now available.");
            
            // Update GUI safely on the FX app thread
            if (guiCallback != null) {
                Platform.runLater(guiCallback);
            }
            
        } catch (InterruptedException e) {
            System.err.println("Cleaning thread interrupted for room " + room.getRoomNumber());
        }
    }
}
