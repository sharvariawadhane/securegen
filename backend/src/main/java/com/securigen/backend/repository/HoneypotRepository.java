package com.securigen.backend.repository;

import com.securigen.backend.model.Honeypot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoneypotRepository extends JpaRepository<Honeypot, Long> {
}