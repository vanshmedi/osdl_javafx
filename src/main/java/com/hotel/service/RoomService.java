package com.hotel.service;

import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.model.User;
import com.hotel.util.FileHandler;
import com.hotel.util.SerializationUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class RoomService {
    private static final String ROOMS_FILE = "rooms_data.dat";
    private List<Room> rooms;

    public RoomService() {
        rooms = new ArrayList<>();
        loadRooms();
    }

    @SuppressWarnings("unchecked")
    private void loadRooms() {
        Object data = SerializationUtil.deserialize(ROOMS_FILE);
        if (data instanceof List) {
            rooms = (List<Room>) data;
            System.out.println("Loaded " + rooms.size() + " rooms.");
        } else {
            // Default rooms with varied amenities
            rooms.add(new Room(101, RoomType.STANDARD, RoomType.STANDARD.getDefaultPrice(), false, false));
            rooms.add(new Room(102, RoomType.STANDARD, RoomType.STANDARD.getDefaultPrice(), true, false));
            rooms.add(new Room(201, RoomType.DELUXE, RoomType.DELUXE.getDefaultPrice(), true, true));
            rooms.add(new Room(202, RoomType.DELUXE, RoomType.DELUXE.getDefaultPrice(), true, true));
            rooms.add(new Room(301, RoomType.SUITE, RoomType.SUITE.getDefaultPrice(), true, true));
            saveRooms();
        }
    }

    public void saveRooms() {
        SerializationUtil.serialize(rooms, ROOMS_FILE);
    }

    public void addRoom(User user, Room room) {
        // Prevent duplicate room numbers
        for (Room r : rooms) {
            if (r.getRoomNumber() == room.getRoomNumber()) {
                throw new IllegalArgumentException("Room number " + room.getRoomNumber() + " already exists.");
            }
        }
        rooms.add(room);
        saveRooms();
        FileHandler.logAction(user, "Added new " + room.getType() + " room " + room.getRoomNumber()
            + (room.isHasAC() ? " [AC]" : "") + (room.isHasWifi() ? " [WiFi]" : ""));
        FileHandler.saveRoomIndex(room.getRoomNumber(), rooms.size() - 1);
    }

    public void removeRoom(User user, int roomNumber) {
        // Using Iterator for safe removal
        Iterator<Room> iterator = rooms.iterator();
        while (iterator.hasNext()) {
            Room room = iterator.next();
            if (room.getRoomNumber() == roomNumber) {
                if (!room.isAvailable()) {
                    throw new IllegalStateException("Cannot remove currently booked room!");
                }
                iterator.remove();
                saveRooms();
                FileHandler.logAction(user, "Removed room " + roomNumber);
                return;
            }
        }
        throw new IllegalArgumentException("Room not found!");
    }

    public void updateRoomRate(User user, int roomNumber, double newRate) {
        Room room = getRoomByNumber(roomNumber);
        if (room == null) throw new IllegalArgumentException("Room not found!");
        double oldRate = room.getPrice();
        room.setPrice(newRate);
        saveRooms();
        FileHandler.logAction(user, "Updated room " + roomNumber + " rate from ₹"
            + String.format("%.2f", oldRate) + " to ₹" + String.format("%.2f", newRate));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public List<Room> getAvailableRooms() {
        return rooms.stream()
                .filter(Room::isAvailable)
                .collect(Collectors.toList());
    }

    public List<Room> searchRoomsByType(RoomType type) {
        return rooms.stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
    }

    public void sortRoomsByPrice() {
        rooms.sort(Comparator.comparingDouble(Room::getPrice));
    }
    
    public Room getRoomByNumber(int roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber() == roomNumber) return r;
        }
        return null;
    }
}
