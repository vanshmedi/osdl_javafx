package com.hotel.model;

import java.io.Serializable;

public class CheckoutRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String customerId;
    private String customerName;
    private String contact;
    private int roomNumber;
    private String roomType;
    private String checkInTime;
    private String checkOutTime;
    private int nightsStayed;
    private double roomRate;
    private double acCharge;
    private double wifiCharge;
    private double subtotal;
    private double tax;
    private double grandTotal;
    private String processedBy;

    public CheckoutRecord(String customerId, String customerName, String contact,
                          int roomNumber, String roomType, String checkInTime,
                          String checkOutTime, int nightsStayed, double roomRate,
                          double acCharge, double wifiCharge, double subtotal,
                          double tax, double grandTotal, String processedBy) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.contact = contact;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.nightsStayed = nightsStayed;
        this.roomRate = roomRate;
        this.acCharge = acCharge;
        this.wifiCharge = wifiCharge;
        this.subtotal = subtotal;
        this.tax = tax;
        this.grandTotal = grandTotal;
        this.processedBy = processedBy;
    }

    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getContact() { return contact; }
    public int getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public String getCheckInTime() { return checkInTime; }
    public String getCheckOutTime() { return checkOutTime; }
    public int getNightsStayed() { return nightsStayed; }
    public double getRoomRate() { return roomRate; }
    public double getAcCharge() { return acCharge; }
    public double getWifiCharge() { return wifiCharge; }
    public double getSubtotal() { return subtotal; }
    public double getTax() { return tax; }
    public double getGrandTotal() { return grandTotal; }
    public String getProcessedBy() { return processedBy; }
}
