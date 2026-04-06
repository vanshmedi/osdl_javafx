package com.hotel.service;

import com.hotel.model.*;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {
    private RoomService roomService;
    private BookingService bookingService;

    public ReportService(RoomService roomService, BookingService bookingService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
    }

    public int getTotalRooms() { return roomService.getAllRooms().size(); }

    public int getOccupiedRooms() {
        return (int) roomService.getAllRooms().stream().filter(r -> !r.isAvailable()).count();
    }

    public int getAvailableRooms() {
        return (int) roomService.getAllRooms().stream().filter(Room::isAvailable).count();
    }

    public double getOccupancyRate() {
        int total = getTotalRooms();
        return total == 0 ? 0 : (getOccupiedRooms() * 100.0) / total;
    }

    public double getEstimatedDailyRevenue() {
        return roomService.getAllRooms().stream()
            .filter(r -> !r.isAvailable())
            .mapToDouble(Room::getEffectiveRate)
            .sum();
    }

    public Map<RoomType, Long> getRoomCountByType() {
        return roomService.getAllRooms().stream()
            .collect(Collectors.groupingBy(Room::getType, Collectors.counting()));
    }

    public Map<RoomType, Long> getOccupiedCountByType() {
        return roomService.getAllRooms().stream()
            .filter(r -> !r.isAvailable())
            .collect(Collectors.groupingBy(Room::getType, Collectors.counting()));
    }

    public int getRoomsWithAC() {
        return (int) roomService.getAllRooms().stream().filter(Room::isHasAC).count();
    }

    public int getRoomsWithWifi() {
        return (int) roomService.getAllRooms().stream().filter(Room::isHasWifi).count();
    }

    public int getRoomsWithBothAmenities() {
        return (int) roomService.getAllRooms().stream()
            .filter(r -> r.isHasAC() && r.isHasWifi()).count();
    }

    public double getTotalCheckoutRevenue() {
        return bookingService.getCheckoutHistory().stream()
            .mapToDouble(CheckoutRecord::getGrandTotal).sum();
    }

    public String generateFullReport() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        sb.append("=============================================\n");
        sb.append("       THE LUXE HOTEL - SYSTEM REPORT\n");
        sb.append("       Generated: ").append(dtf.format(LocalDateTime.now())).append("\n");
        sb.append("=============================================\n\n");

        sb.append("-- OCCUPANCY SUMMARY --\n");
        sb.append(String.format("  Total Rooms:     %d\n", getTotalRooms()));
        sb.append(String.format("  Occupied:        %d\n", getOccupiedRooms()));
        sb.append(String.format("  Available:       %d\n", getAvailableRooms()));
        sb.append(String.format("  Occupancy Rate:  %.1f%%\n\n", getOccupancyRate()));

        sb.append("-- REVENUE SUMMARY --\n");
        sb.append(String.format("  Est. Daily Revenue:     ₹%.2f\n", getEstimatedDailyRevenue()));
        sb.append(String.format("  Total Checkout Revenue: ₹%.2f\n\n", getTotalCheckoutRevenue()));

        sb.append("-- ROOM TYPE DISTRIBUTION --\n");
        Map<RoomType, Long> counts = getRoomCountByType();
        Map<RoomType, Long> occupied = getOccupiedCountByType();
        for (RoomType type : RoomType.values()) {
            long total = counts.getOrDefault(type, 0L);
            long occ = occupied.getOrDefault(type, 0L);
            sb.append(String.format("  %-10s  Total: %d  |  Occupied: %d  |  Available: %d\n",
                type.name(), total, occ, total - occ));
        }
        sb.append("\n");

        sb.append("-- AMENITY SUMMARY --\n");
        sb.append(String.format("  Rooms with AC:      %d\n", getRoomsWithAC()));
        sb.append(String.format("  Rooms with WiFi:    %d\n", getRoomsWithWifi()));
        sb.append(String.format("  Rooms with Both:    %d\n\n", getRoomsWithBothAmenities()));

        sb.append("-- CURRENT GUESTS --\n");
        List<Customer> guests = bookingService.getAllCustomers();
        if (guests.isEmpty()) {
            sb.append("  No guests currently checked in.\n");
        } else {
            sb.append(String.format("  %-12s %-20s %-15s %-8s %-20s\n",
                "ID", "Name", "Contact", "Room #", "Check-In"));
            for (Customer c : guests) {
                sb.append(String.format("  %-12s %-20s %-15s %-8d %-20s\n",
                    c.getCustomerId(), c.getName(), c.getContact(),
                    c.getAllocatedRoomNumber(), c.getCheckInTime()));
            }
        }

        sb.append("\n-- CHECKOUT HISTORY --\n");
        List<CheckoutRecord> history = bookingService.getCheckoutHistory();
        if (history.isEmpty()) {
            sb.append("  No checkout records.\n");
        } else {
            sb.append(String.format("  %-12s %-18s %-8s %-8s %-12s\n",
                "ID", "Name", "Room #", "Nights", "Total"));
            for (CheckoutRecord r : history) {
                sb.append(String.format("  %-12s %-18s %-8d %-8d ₹%-11.2f\n",
                    r.getCustomerId(), r.getCustomerName(), r.getRoomNumber(),
                    r.getNightsStayed(), r.getGrandTotal()));
            }
        }

        sb.append("\n=============================================\n");
        sb.append("              END OF REPORT\n");
        sb.append("=============================================\n");
        return sb.toString();
    }

    public void exportReport(String filepath) throws IOException {
        try (FileWriter writer = new FileWriter(filepath)) {
            writer.write(generateFullReport());
        }
    }
}
