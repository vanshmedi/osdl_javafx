package com.hotel.model;

import java.io.Serializable;

public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private String customerId;
    private String name;
    private String contact;
    private int allocatedRoomNumber;
    private String checkInTime;
    private String checkedInBy;

    public Customer(String customerId, String name, String contact, int allocatedRoomNumber, String checkInTime, String checkedInBy) {
        this.customerId = customerId;
        this.name = name;
        this.contact = contact;
        this.allocatedRoomNumber = allocatedRoomNumber;
        this.checkInTime = checkInTime;
        this.checkedInBy = checkedInBy;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public int getAllocatedRoomNumber() {
        return allocatedRoomNumber;
    }

    public void setAllocatedRoomNumber(int allocatedRoomNumber) {
        this.allocatedRoomNumber = allocatedRoomNumber;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(String checkedInBy) {
        this.checkedInBy = checkedInBy;
    }

    @Override
    public String toString() {
        return name + " (ID: " + customerId + ") - Room " + allocatedRoomNumber;
    }
}
