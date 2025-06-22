package com.ll.Yuruppang.global.healthCheck;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheck {
    private static final ResponseEntity<Void> ALIVE =
            ResponseEntity.ok().build();

    @GetMapping
    public ResponseEntity<Void> healthCheck() {
        return ALIVE;
    }
}
