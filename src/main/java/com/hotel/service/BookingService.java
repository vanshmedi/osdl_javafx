package com.hotel.service;

import com.hotel.model.CheckoutRecord;
import com.hotel.model.Customer;
import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.util.FileHandler;
import com.hotel.util.SerializationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BookingService {
    private static final String CUSTOMERS_FILE = "customers_data.dat";
    private static final String CHECKOUT_HISTORY_FILE = "checkout_history.dat";
    private List<Customer> customers;
    private List<CheckoutRecord> checkoutHistory;
    private Map<Integer, Customer> roomToCustomerMap; // Fast lookup mapping
    private RoomService roomService;

    public BookingService(RoomService roomService) {
        this.roomService = roomService;
        this.customers = new ArrayList<>();
        this.checkoutHistory = new ArrayList<>();
        this.roomToCustomerMap = new HashMap<>();
        loadCustomers();
        loadCheckoutHistory();
    }

    @SuppressWarnings("unchecked")
    private void loadCustomers() {
        Object data = SerializationUtil.deserialize(CUSTOMERS_FILE);
        if (data instanceof List) {
            customers = (List<Customer>) data;
            System.out.println("Loaded " + customers.size() + " customers.");
            // Rebuild Map
            for (Customer c : customers) {
                roomToCustomerMap.put(c.getAllocatedRoomNumber(), c);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadCheckoutHistory() {
        Object data = SerializationUtil.deserialize(CHECKOUT_HISTORY_FILE);
        if (data instanceof List) {
            checkoutHistory = (List<CheckoutRecord>) data;
            System.out.println("Loaded " + checkoutHistory.size() + " checkout records.");
        }
    }

    public void saveCustomers() {
        SerializationUtil.serialize(customers, CUSTOMERS_FILE);
    }

    private void saveCheckoutHistory() {
        SerializationUtil.serialize(checkoutHistory, CHECKOUT_HISTORY_FILE);
    }

    public void bookRoom(User user, String customerId, String name, String contact, int roomNumber) {
        Room room = roomService.getRoomByNumber(roomNumber);

        if (room == null) {
            throw new IllegalArgumentException("Room does not exist.");
        }
        if (!room.isAvailable()) {
            throw new IllegalStateException("Room is already booked (Double booking prevention caught).");
        }

        // Create and save customer
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String checkInTime = dtf.format(LocalDateTime.now());
        String checkedInBy = user != null ? user.getUsername() : "SYSTEM";
        Customer newCustomer = new Customer(customerId, name, contact, roomNumber, checkInTime, checkedInBy);
        customers.add(newCustomer);
        roomToCustomerMap.put(roomNumber, newCustomer);
        
        // Update room status
        room.setAvailable(false);
        roomService.saveRooms(); // persist changes
        saveCustomers();
        
        FileHandler.logAction(user, "Room " + roomNumber + " booked by " + name + " (ID: " + customerId + ")");
    }

    public Customer checkout(User user, int roomNumber, int daysStayed) {
        Room room = roomService.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room does not exist.");
        }
        
        Customer customerToCheckout = roomToCustomerMap.get(roomNumber);
        if (customerToCheckout == null) {
             throw new IllegalStateException("Room is already empty.");
        }

        customers.remove(customerToCheckout);
        roomToCustomerMap.remove(roomNumber);
        
        // Calculate charges with amenity surcharges
        double baseCharge = daysStayed * room.getPrice();
        double acCharge = daysStayed * room.getAcSurcharge();
        double wifiCharge = daysStayed * room.getWifiSurcharge();
        double subtotal = baseCharge + acCharge + wifiCharge;
        double tax = subtotal * 0.12;
        double grandTotal = subtotal + tax;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String checkOutTime = dtf.format(LocalDateTime.now());
        String processedBy = user != null ? user.getUsername() : "SYSTEM";

        // Create checkout record for history
        CheckoutRecord record = new CheckoutRecord(
            customerToCheckout.getCustomerId(),
            customerToCheckout.getName(),
            customerToCheckout.getContact(),
            roomNumber,
            room.getType().name(),
            customerToCheckout.getCheckInTime(),
            checkOutTime,
            daysStayed,
            room.getPrice(),
            acCharge,
            wifiCharge,
            subtotal,
            tax,
            grandTotal,
            processedBy
        );
        checkoutHistory.add(record);

        String checkedInAt = customerToCheckout.getCheckInTime() != null ? customerToCheckout.getCheckInTime() : "N/A";
        
        saveCustomers();
        saveCheckoutHistory();
        FileHandler.logAction(user,
            "CHECKOUT - Room " + roomNumber + " | Guest: " + customerToCheckout.getName() +
            " (ID: " + customerToCheckout.getCustomerId() + ")" +
            " | Check-In: " + checkedInAt +
            " | Days: " + daysStayed +
            " | Rate: ₹" + String.format("%.2f", room.getPrice()) + "/night" +
            (acCharge > 0 ? " | AC: ₹" + String.format("%.2f", acCharge) : "") +
            (wifiCharge > 0 ? " | WiFi: ₹" + String.format("%.2f", wifiCharge) : "") +
            " | TOTAL CHARGED: ₹" + String.format("%.2f", grandTotal));
        
        return customerToCheckout;
    }

    /** Legacy overload kept for compatibility */
    public Customer checkout(User user, int roomNumber) {
        return checkout(user, roomNumber, 1);
    }

    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers);
    }

    public List<CheckoutRecord> getCheckoutHistory() {
        return new ArrayList<>(checkoutHistory);
    }
}
