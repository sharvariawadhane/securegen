package com.securigen.backend.controller;

import com.securigen.backend.model.Honeypot;
import com.securigen.backend.repository.HoneypotRepository;
import com.securigen.backend.service.DockerDeploymentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/honeypots")
public class HoneypotDeploymentController {

    private final DockerDeploymentService dockerDeploymentService;
    private final HoneypotRepository honeypotRepository;

    public HoneypotDeploymentController(
            DockerDeploymentService dockerDeploymentService,
            HoneypotRepository honeypotRepository) {

        this.dockerDeploymentService = dockerDeploymentService;
        this.honeypotRepository = honeypotRepository;
    }

    @PostMapping("/{id}/deploy")
    public Map<String, String> deployHoneypot(
            @PathVariable Long id) throws Exception {

        Honeypot honeypot = honeypotRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Honeypot not found"
                        )
                );

        if (!"WEB".equalsIgnoreCase(honeypot.getBackendType())) {
            throw new RuntimeException(
                    "Only WEB honeypots are supported currently"
            );
        }

        honeypot.setStatus("DEPLOYING");
        honeypotRepository.save(honeypot);

        try {

            String containerId =
                    dockerDeploymentService.deployWebHoneypot();

            honeypot.setContainerId(containerId);
            honeypot.setStatus("RUNNING");
            honeypot.setPort(8081);
            honeypot.setCreatedAt(
                    honeypot.getCreatedAt() == null
                            ? LocalDateTime.now()
                            : honeypot.getCreatedAt()
            );

            honeypotRepository.save(honeypot);

            return Map.of(
                    "message",
                    "Honeypot deployed successfully",
                    "containerId",
                    containerId
            );

        } catch (Exception e) {

            honeypot.setStatus("STOPPED");
            honeypotRepository.save(honeypot);

            throw e;
        }
    }
}