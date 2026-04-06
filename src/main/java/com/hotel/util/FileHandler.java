package com.hotel.util;

import com.hotel.model.User;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;

public class FileHandler {
    private static final String LOG_FILE = "system_log.txt";
    private static final String ROOM_INDEX_FILE = "rooms_index.dat";
    private static final String ROOM_SERVICE_LOG = "room_service_logs.txt";
    private static final String CLEANING_LOG = "cleaning_logs.txt";

    // Appends an action log using FileWriter
    public static void logAction(User user, String action) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            String username = (user != null) ? user.getUsername() : "SYSTEM";
            writer.write(LocalDateTime.now() + " - [" + username + "] - " + action + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    public static void logRoomService(String action) {
        logSpecific(ROOM_SERVICE_LOG, action);
    }

    public static void logCleaning(String action) {
        logSpecific(CLEANING_LOG, action);
    }

    private static void logSpecific(String file, String action) {
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(LocalDateTime.now() + " - " + action + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write to file: " + e.getMessage());
        }
    }

    // Reads full system logs
    public static String readLogs() {
        return readGenericLog(LOG_FILE);
    }

    public static String readRoomServiceLogs() {
        return readGenericLog(ROOM_SERVICE_LOG);
    }

    public static String readCleaningLogs() {
        return readGenericLog(CLEANING_LOG);
    }

    private static String readGenericLog(String filename) {
        StringBuilder logs = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.append(line).append("\n");
            }
        } catch (IOException e) {
            return "No logs found or error reading log file: " + filename;
        }
        return logs.toString();
    }

    // Demonstrates RandomAccessFile for direct byte-level storage mock
    // This maintains a quick lookup reference for Room IDs
    public static void saveRoomIndex(int roomId, long filePointer) {
        try (RandomAccessFile raf = new RandomAccessFile(ROOM_INDEX_FILE, "rw")) {
            raf.seek(raf.length());
            raf.writeInt(roomId);
            raf.writeLong(filePointer);
        } catch (IOException e) {
            System.err.println("Failed to write RandomAccessFile: " + e.getMessage());
        }
    }
    
    // Reads from RandomAccessFile to find pointer
    public static long getRoomPointer(int targetRoomId) {
        try (RandomAccessFile raf = new RandomAccessFile(ROOM_INDEX_FILE, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                int id = raf.readInt();
                long pointer = raf.readLong();
                if (id == targetRoomId) {
                    return pointer;
                }
            }
        } catch (IOException e) {
             System.err.println("Failed to read RandomAccessFile: " + e.getMessage());
        }
        return -1;
    }
}
