package com.securigen.backend.service;

import com.securigen.backend.model.Honeypot;
import com.securigen.backend.model.Vulnerability;
import com.securigen.backend.repository.HoneypotRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.UUID;

@Service
public class HoneypotDeploymentService {

    private final HoneypotRepository honeypotRepository;
    private final HoneypotGenerationService generationService;
    private final VulnerabilityCatalogService vulnerabilityCatalog;

    public HoneypotDeploymentService(
            HoneypotRepository honeypotRepository,
            HoneypotGenerationService generationService,
            VulnerabilityCatalogService vulnerabilityCatalog
    ) {
        this.honeypotRepository = honeypotRepository;
        this.generationService = generationService;
        this.vulnerabilityCatalog = vulnerabilityCatalog;
    }

    public Honeypot generateAndDeploy(
            Long honeypotId
    ) throws Exception {

        Honeypot honeypot =
                honeypotRepository.findById(honeypotId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Honeypot not found"
                                )
                        );

        honeypot.setStatus("GENERATING");
        honeypotRepository.save(honeypot);

        Vulnerability vulnerability =
                vulnerabilityCatalog.getById(
                        honeypot.getVulnerabilityId()
                );

        /*
         * THIS IS THE IMPORTANT LINE.
         *
         * We pass BOTH the backend type and
         * selected vulnerability.
         */
        String generatedFile =
                generationService.generateHoneypot(
                        honeypot.getBackendType(),
                        honeypot.getVulnerabilityId()
                );

        Path generatedPath =
                Paths.get(generatedFile)
                        .toAbsolutePath();

        if (!Files.exists(generatedPath)) {

            honeypot.setStatus(
                    "GENERATION_FAILED"
            );

            honeypotRepository.save(honeypot);

            throw new RuntimeException(
                    "Generated server.js was not created."
            );
        }

        String safeName =
                honeypot.getName()
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]+",
                                "-"
                        );

        String uniqueId =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        Path deploymentDirectory =
                Paths.get("generated_honeypots")
                        .resolve(
                                safeName +
                                "-" +
                                uniqueId
                        );

        Files.createDirectories(
                deploymentDirectory
        );

        Files.copy(
                generatedPath,
                deploymentDirectory.resolve(
                        "server.js"
                ),
                StandardCopyOption.REPLACE_EXISTING
        );

        String packageJson = """
                {
                  "name": "securigen-honeypot",
                  "version": "1.0.0",
                  "private": true,
                  "dependencies": {
                    "express": "^4.21.2"
                  }
                }
                """;

        Files.writeString(
                deploymentDirectory.resolve(
                        "package.json"
                ),
                packageJson
        );

        String dockerfile = """
                FROM node:22-alpine

                WORKDIR /app

                COPY package.json .

                RUN npm install --omit=dev

                COPY server.js .

                EXPOSE 8080

                CMD ["node", "server.js"]
                """;

        Files.writeString(
                deploymentDirectory.resolve(
                        "Dockerfile"
                ),
                dockerfile
        );

        String imageName =
                "securigen/" +
                safeName +
                ":" +
                uniqueId;

        String containerName =
                "securigen-" +
                safeName +
                "-" +
                uniqueId;

        /*
         * Build Docker image.
         */
        runCommand(
                "docker",
                "build",
                "-t",
                imageName,
                deploymentDirectory.toString()
        );

        int hostPort =
                findAvailablePort(8081);

        /*
         * Run container.
         */
        String containerId =
                runCommand(
                        "docker",
                        "run",
                        "-d",
                        "--name",
                        containerName,
                        "-p",
                        hostPort + ":8080",
                        "--restart",
                        "unless-stopped",
                        imageName
                ).trim();

        honeypot.setContainerId(
                containerId
        );

        honeypot.setPort(
                hostPort
        );

        honeypot.setGeneratedFile(
                generatedPath.toString()
        );

        honeypot.setStatus(
                "RUNNING"
        );

        honeypotRepository.save(honeypot);

        return honeypot;
    }

    private String runCommand(
            String... command
    ) throws IOException,
            InterruptedException {

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.redirectErrorStream(
                true
        );

        Process process =
                processBuilder.start();

        String output =
                new String(
                        process.getInputStream()
                                .readAllBytes()
                );

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            throw new RuntimeException(
                    "Docker command failed:\n\n" +
                    String.join(
                            " ",
                            command
                    ) +
                    "\n\n" +
                    output
            );
        }

        return output;
    }

    private int findAvailablePort(
            int startingPort
    ) {

        for (
                int port = startingPort;
                port < 9000;
                port++
        ) {

            try (
                    ServerSocket socket =
                            new ServerSocket(port)
            ) {

                return port;

            } catch (IOException ignored) {
            }
        }

        throw new RuntimeException(
                "No available port found."
        );
    }
}