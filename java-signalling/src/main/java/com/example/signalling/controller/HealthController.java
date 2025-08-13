package com.example.signalling.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {

    @GetMapping("/health-check")
    public Mono<String> healthCheck() {
        return Mono.just("OK");
    }
}