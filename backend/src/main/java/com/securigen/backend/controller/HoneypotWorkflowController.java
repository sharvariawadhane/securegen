package com.securigen.backend.controller;

import com.securigen.backend.model.Honeypot;
import com.securigen.backend.service.HoneypotDeploymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/honeypots")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173"
})
public class HoneypotWorkflowController {

    private final HoneypotDeploymentService deploymentService;

    public HoneypotWorkflowController(
            HoneypotDeploymentService deploymentService
    ) {
        this.deploymentService =
                deploymentService;
    }

    @PostMapping(
            "/{id}/generate-and-deploy"
    )
    public ResponseEntity<?> generateAndDeploy(
            @PathVariable Long id
    ) {

        try {

            Honeypot honeypot =
                    deploymentService
                            .generateAndDeploy(id);

            return ResponseEntity.ok(
                    honeypot
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }
}