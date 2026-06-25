package com.example.orquestadorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OrquestadorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrquestadorServiceApplication.class, args);
    }
}
