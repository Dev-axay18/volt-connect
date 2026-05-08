package com.voltconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Volt-Connect P2P EV Charging Marketplace backend.
 */
@SpringBootApplication
@EnableScheduling
public class VoltConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoltConnectApplication.class, args);
    }
}
