package com.securigen.backend.controller;

import com.securigen.backend.model.AttackLog;
import com.securigen.backend.model.Honeypot;
import com.securigen.backend.repository.AttackLogRepository;
import com.securigen.backend.repository.HoneypotRepository;
import com.securigen.backend.service.DockerLogParserService;
import com.securigen.backend.service.DockerLogService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/docker")
public class DockerLogController {

    private final DockerLogService dockerLogService;
    private final DockerLogParserService parserService;
    private final AttackLogRepository attackLogRepository;
    private final HoneypotRepository honeypotRepository;

    public DockerLogController(
            DockerLogService dockerLogService,
            DockerLogParserService parserService,
            AttackLogRepository attackLogRepository,
            HoneypotRepository honeypotRepository) {

        this.dockerLogService = dockerLogService;
        this.parserService = parserService;
        this.attackLogRepository = attackLogRepository;
        this.honeypotRepository = honeypotRepository;
    }

    /*
     * Get raw Docker logs for a specific honeypot.
     *
     * Example:
     * GET /api/docker/logs/1
     */
    @GetMapping("/logs/{honeypotId}")
    public List<String> getLogs(
            @PathVariable Long honeypotId) throws Exception {

        Honeypot honeypot =
                getHoneypot(honeypotId);

        return dockerLogService.getHoneypotLogs(
                honeypot.getContainerId()
        );
    }

    /*
     * Parse Docker logs for a specific honeypot.
     *
     * Example:
     * GET /api/docker/parsed-logs/1
     */
    @GetMapping("/parsed-logs/{honeypotId}")
    public List<AttackLog> getParsedLogs(
            @PathVariable Long honeypotId) throws Exception {

        Honeypot honeypot =
                getHoneypot(honeypotId);

        List<String> rawLogs =
                dockerLogService.getHoneypotLogs(
                        honeypot.getContainerId()
                );

        List<AttackLog> parsedLogs =
                new ArrayList<>();

        for (String line : rawLogs) {

            AttackLog attackLog =
                    parserService.parse(
                            line,
                            honeypotId
                    );

            if (attackLog != null) {

                attackLog.setRawLog(line);

                parsedLogs.add(attackLog);
            }
        }

        return parsedLogs;
    }

    /*
     * Import logs into MySQL.
     *
     * Example:
     * POST /api/docker/import-logs/1
     */
    @PostMapping("/import-logs/{honeypotId}")
    public String importLogs(
            @PathVariable Long honeypotId) throws Exception {

        Honeypot honeypot =
                getHoneypot(honeypotId);

        List<String> rawLogs =
                dockerLogService.getHoneypotLogs(
                        honeypot.getContainerId()
                );

        int imported = 0;
        int skipped = 0;

        for (String line : rawLogs) {

            AttackLog attackLog =
                    parserService.parse(
                            line,
                            honeypotId
                    );

            if (attackLog == null) {
                continue;
            }

            attackLog.setRawLog(line);

            if (attackLogRepository
                    .findByRawLog(line)
                    .isPresent()) {

                skipped++;
                continue;
            }

            attackLogRepository.save(
                    attackLog
            );

            imported++;
        }

        return "Import complete. Imported: "
                + imported
                + ", Skipped duplicates: "
                + skipped;
    }

    private Honeypot getHoneypot(
            Long honeypotId) {

        return honeypotRepository
                .findById(honeypotId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Honeypot not found: "
                                        + honeypotId
                        )
                );
    }
}