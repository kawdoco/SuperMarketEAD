package com.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lightweight POS terminal application.
 * <p>
 * Runs as a pure local server:
 * <ul>
 *   <li>SQLite file (<code>data/pos.db</code>) on the local machine</li>
 *   <li>HTTP only on 127.0.0.1:8080 (the POS terminal browser connects here)</li>
 *   <li>No cloud, no external DB, no session store</li>
 * </ul>
 * <p>
 * Boot with: <code>mvn spring-boot:run</code> or
 * <code>java -jar target/pos-terminal-1.0.0.jar</code>
 */
@SpringBootApplication
public class PosTerminalApplication {

    public static void main(String[] args) {
        ensureDataDirectory();
        SpringApplication.run(PosTerminalApplication.class, args);
    }

    /**
     * SQLite does not create missing parent directories, so a fresh install
     * on the terminal would fail to open {@code data/pos.db}. Create the
     * folder up front, before Spring touches the datasource.
     */
    private static void ensureDataDirectory() {
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot create data directory for the SQLite database", e);
        }
    }
}
