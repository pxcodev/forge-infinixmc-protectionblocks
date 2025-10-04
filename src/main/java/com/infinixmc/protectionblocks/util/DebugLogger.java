package com.infinixmc.protectionblocks.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogger {
    private static final String LOG_DIR = "config/protectionblocks";
    private static final String LOG_FILE = "debug.log";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static PrintWriter writer;
    private static boolean initialized = false;
    
    public static void init() {
        if (initialized) return;
        
        try {
            // Crear directorio si no existe
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            // Crear archivo de log
            Path logFile = logDir.resolve(LOG_FILE);
            writer = new PrintWriter(new FileWriter(logFile.toFile(), false)); // false = sobrescribir
            
            // Escribir cabecera
            log("=".repeat(80));
            log("PROTECTION BLOCKS DEBUG LOG - " + LocalDateTime.now());
            log("=".repeat(80));
            
            initialized = true;
            System.out.println("Debug logger initialized at: " + LOG_DIR + "/" + LOG_FILE);
            System.out.println("Full path: " + logFile.toAbsolutePath().toString());
            
        } catch (IOException e) {
            System.err.println("Error initializing debug logger: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void log(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String logLine = "[" + timestamp + "] " + message;
        
        // Escribir al archivo
        if (writer != null) {
            writer.println(logLine);
            writer.flush(); // Asegurar que se escriba inmediatamente
        }
        
        // También escribir a consola para debug
        System.out.println(logLine);
    }
    
    public static void logSeparator() {
        log("-".repeat(60));
    }
    
    public static void logSection(String sectionName) {
        log("");
        log("=== " + sectionName + " ===");
    }
    
    public static void logError(String message, Exception e) {
        log("ERROR: " + message);
        if (e != null) {
            log("Exception: " + e.getMessage());
        }
    }
    
    public static void logPlayerData(String playerName, int blocksUsed, int maxBlocks) {
        log("PLAYER DATA - " + playerName + ": " + blocksUsed + "/" + maxBlocks + " bloques");
    }
    
    public static void logPersistence(String operation, boolean success, String details) {
        String status = success ? "SUCCESS" : "FAILED";
        log("PERSISTENCE [" + operation + "] " + status + ": " + details);
    }
    
    public static void logWorldSync(String operation, int blocksProcessed) {
        log("WORLD SYNC [" + operation + "] Procesados: " + blocksProcessed + " bloques");
    }
    
    public static void close() {
        if (writer != null) {
            log("=".repeat(80));
            log("DEBUG LOG CERRADO - " + LocalDateTime.now());
            log("=".repeat(80));
            writer.close();
            writer = null;
            initialized = false;
        }
    }
}