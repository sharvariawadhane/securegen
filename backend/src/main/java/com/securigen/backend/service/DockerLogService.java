package com.securigen.backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class DockerLogService {

    public List<String> getHoneypotLogs(String containerName) throws Exception {

        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker",
                "logs",
                "--tail",
                "100",
                containerName
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        List<String> logs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        }

        process.waitFor();

        return logs;
    }
}