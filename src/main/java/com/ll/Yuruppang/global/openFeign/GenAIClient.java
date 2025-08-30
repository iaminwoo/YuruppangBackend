package com.ll.Yuruppang.global.openFeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "geminiClient", url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent")
public interface GenAIClient {

    @PostMapping(consumes = "application/json")
    AiResponse generateRecipe(
            @RequestParam("key") String apiKey,
            @RequestBody Map<String, Object> request
    );
}

