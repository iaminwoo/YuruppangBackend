package com.ll.Yuruppang.global.healthCheck;

import com.ll.Yuruppang.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheck {

    @GetMapping
    public RsData<String> healthCheck() {
        return RsData.success(HttpStatus.OK, "Success");
    }
}
