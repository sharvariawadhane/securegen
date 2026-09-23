package com.securigen.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securigen.backend.model.AttackLog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DockerLogParserService {

    private final ObjectMapper objectMapper;

    public DockerLogParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AttackLog parse(String logLine, Long honeypotId) {

        if (logLine == null || logLine.isBlank()) {
            return null;
        }

        try {

            /*
             * Ignore normal Docker/application messages such as:
             *
             * Server started on port 8080
             *
             * We only want our structured HTTP logs.
             */
            if (!logLine.contains("\"type\":\"http_request\"")) {
                return null;
            }

            JsonNode json = objectMapper.readTree(logLine);

            if (!"http_request".equals(
                    json.path("type").asText())) {
                return null;
            }

            AttackLog attackLog = new AttackLog();

            attackLog.setHoneypotId(honeypotId);

            attackLog.setSourceIp(
                    cleanIp(
                            json.path("sourceIp")
                                    .asText("")
                    )
            );

            attackLog.setMethod(
                    json.path("method")
                            .asText("")
            );

            String path =
                    json.path("path")
                            .asText("");

            String query =
                    json.path("query")
                            .asText("");

            /*
             * Store the query together with the path.
             *
             * Example:
             *
             * /login?userInput=<script>
             */
            if (!query.isBlank()) {
                path = path + "?" + query;
            }

            attackLog.setPath(path);

            attackLog.setUserAgent(
                    json.path("userAgent")
                            .asText("")
            );

            attackLog.setStatusCode(
                    json.path("statusCode")
                            .asInt(0)
            );

            String timestamp =
                    json.path("timestamp")
                            .asText("");

            if (!timestamp.isBlank()) {

                try {

                    attackLog.setTimestamp(
                            LocalDateTime.parse(
                                    timestamp.substring(
                                            0,
                                            19
                                    )
                            )
                    );

                } catch (Exception ignored) {

                    attackLog.setTimestamp(
                            LocalDateTime.now()
                    );
                }

            } else {

                attackLog.setTimestamp(
                        LocalDateTime.now()
                );
            }

            return attackLog;

        } catch (Exception e) {

            System.out.println(
                    "Could not parse honeypot log: "
                            + logLine
            );

            return null;
        }
    }

    private String cleanIp(String ip) {

        if (ip == null) {
            return "";
        }

        if (ip.startsWith("::ffff:")) {
            return ip.substring(7);
        }

        return ip;
    }
}