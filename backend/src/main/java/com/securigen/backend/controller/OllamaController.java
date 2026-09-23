package com.securigen.backend.controller;

import com.securigen.backend.service.OllamaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class OllamaController {

    private final OllamaService ollamaService;

    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/test")
    public Map<String, String> testAI(@RequestBody Map<String, String> request) {

        String prompt = request.get("prompt");

        String response = ollamaService.generate(prompt);

        return Map.of("response", response);
    }
}