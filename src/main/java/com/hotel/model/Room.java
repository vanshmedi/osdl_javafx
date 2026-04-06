package com.hotel.model;

import java.io.Serializable;
import java.util.Objects;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roomNumber;
    private RoomType type;
    private double price;
    private boolean isAvailable;
    private boolean hasAC;
    private boolean hasWifi;

    public Room(int roomNumber, RoomType type, double price) {
        this(roomNumber, type, price, false, false);
    }

    public Room(int roomNumber, RoomType type, double price, boolean hasAC, boolean hasWifi) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.price = price;
        this.isAvailable = true;
        this.hasAC = hasAC;
        this.hasWifi = hasWifi;
    }

    // Getters and Setters
    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public boolean isHasAC() { return hasAC; }
    public void setHasAC(boolean hasAC) { this.hasAC = hasAC; }

    public boolean isHasWifi() { return hasWifi; }
    public void setHasWifi(boolean hasWifi) { this.hasWifi = hasWifi; }

    /** AC surcharge per night (₹15 if AC enabled) */
    public double getAcSurcharge() { return hasAC ? 15.0 : 0.0; }

    /** WiFi surcharge per night (₹10 if WiFi enabled) */
    public double getWifiSurcharge() { return hasWifi ? 10.0 : 0.0; }

    /** Total effective rate per night (base + surcharges) */
    public double getEffectiveRate() { return price + getAcSurcharge() + getWifiSurcharge(); }

    /** Display string for amenities column */
    public String getAmenitiesDisplay() {
        StringBuilder sb = new StringBuilder();
        if (hasAC) sb.append("AC");
        if (hasWifi) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append("WiFi");
        }
        if (sb.length() == 0) sb.append("—");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return roomNumber == room.roomNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }

    @Override
    public String toString() {
        return "Room " + roomNumber + " (" + type + ")";
    }
}
