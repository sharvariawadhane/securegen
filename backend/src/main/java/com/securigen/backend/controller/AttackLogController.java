package com.securigen.backend.controller;

import com.securigen.backend.model.AttackLog;
import com.securigen.backend.repository.AttackLogRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/attack-logs")
public class AttackLogController {

    private final AttackLogRepository attackLogRepository;

    public AttackLogController(AttackLogRepository attackLogRepository) {
        this.attackLogRepository = attackLogRepository;
    }

    @PostMapping
    public AttackLog createAttackLog(@RequestBody AttackLog attackLog) {

        if (attackLog.getTimestamp() == null) {
            attackLog.setTimestamp(LocalDateTime.now());
        }

        return attackLogRepository.save(attackLog);
    }

    @GetMapping
    public List<AttackLog> getAllAttackLogs() {
        return attackLogRepository.findAll();
    }
}