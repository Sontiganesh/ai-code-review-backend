package com.example.aicode;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AICodeApplication {
    public static void main(String[] args) {
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "C:\\Users\\sonti\\.keys\\gen-lang-client-0126908008-ac0f91126fcf.json");
        SpringApplication.run(AICodeApplication.class, args);
    }
}

