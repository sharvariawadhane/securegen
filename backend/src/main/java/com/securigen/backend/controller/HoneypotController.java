package com.securigen.backend.controller;

import com.securigen.backend.model.Honeypot;
import com.securigen.backend.repository.HoneypotRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/honeypots")
public class HoneypotController {

    private final HoneypotRepository honeypotRepository;

    public HoneypotController(HoneypotRepository honeypotRepository) {
        this.honeypotRepository = honeypotRepository;
    }

    @GetMapping
    public List<Honeypot> getAllHoneypots() {
        return honeypotRepository.findAll();
    }

    @PostMapping
    public Honeypot createHoneypot(@RequestBody Honeypot honeypot) {

        if (honeypot.getStatus() == null) {
            honeypot.setStatus("STOPPED");
        }

        if (honeypot.getCreatedAt() == null) {
            honeypot.setCreatedAt(LocalDateTime.now());
        }

        return honeypotRepository.save(honeypot);
    }
}