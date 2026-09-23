package com.securigen.backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class DockerDeploymentService {

    public String deployWebHoneypot() throws Exception {

        // 1. Build the Docker image
        runCommand(
                "docker",
                "build",
                "-t",
                "securigen-honeypot:web",
                "honeypots/web"
        );

        // 2. Remove an old container if it exists
        runCommandAllowFailure(
                "docker",
                "rm",
                "-f",
                "securigen-web-honeypot"
        );

        // 3. Start the honeypot container
        String containerId = runCommand(
                "docker",
                "run",
                "-d",
                "--name",
                "securigen-web-honeypot",
                "--network",
                "securigen-network",
                "securigen-honeypot:web"
        );

        return containerId.trim();
    }

    private String runCommand(String... command) throws Exception {

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        List<String> output = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {

            throw new RuntimeException(
                    "Docker command failed: "
                            + String.join("\n", output)
            );
        }

        return String.join("\n", output);
    }

    private void runCommandAllowFailure(String... command)
            throws Exception {

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        process.waitFor();
    }
}