package com.company.ticketservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketserviceApplication.class, args);
    }
}
