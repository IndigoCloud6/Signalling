package com.example.signalling;

import com.example.signalling.config.SignallingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(SignallingProperties.class)
@EnableScheduling
public class SignallingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignallingApplication.class, args);
    }
}