package com.securigen.backend.repository;

import com.securigen.backend.model.AttackLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttackLogRepository extends JpaRepository<AttackLog, Long> {

    Optional<AttackLog> findByRawLog(String rawLog);
}