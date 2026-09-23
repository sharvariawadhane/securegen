package com.securigen.backend.controller;

import com.securigen.backend.service.HoneypotGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/honeypots")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173"
})
public class HoneypotGenerationController {

    private final HoneypotGenerationService generationService;

    public HoneypotGenerationController(
            HoneypotGenerationService generationService
    ) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateHoneypot(
            @RequestParam String backendType,
            @RequestParam String vulnerabilityId
    ) {

        try {

            String generatedFile =
                    generationService.generateHoneypot(
                            backendType,
                            vulnerabilityId
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "generatedFile", generatedFile
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "success", false,
                                    "error",
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "Honeypot generation failed."
                            )
                    );
        }
    }
}
