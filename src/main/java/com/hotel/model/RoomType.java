package com.hotel.model;

public enum RoomType {
    STANDARD(100.0),
    DELUXE(250.0),
    SUITE(500.0);

    private final double defaultPrice;

    RoomType(double defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }
}
