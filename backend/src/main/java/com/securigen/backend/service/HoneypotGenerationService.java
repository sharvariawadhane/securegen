package com.securigen.backend.service;

import com.securigen.backend.model.Vulnerability;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class HoneypotGenerationService {

    private final GeminiService geminiService;
    private final VulnerabilityCatalogService vulnerabilityCatalog;

    public HoneypotGenerationService(
            GeminiService geminiService,
            VulnerabilityCatalogService vulnerabilityCatalog
    ) {
        this.geminiService = geminiService;
        this.vulnerabilityCatalog = vulnerabilityCatalog;
    }

    public String generateHoneypot(
            String backendType,
            String vulnerabilityId
    ) throws Exception {

        Vulnerability vulnerability =
                vulnerabilityCatalog.getById(vulnerabilityId);

        if (vulnerability == null) {
            throw new IllegalArgumentException(
                    "Unknown vulnerability ID: " + vulnerabilityId
            );
        }

        String prompt = """
        You are generating a SAFE cybersecurity web honeypot
        for a defensive cybersecurity research project.

        Generate ONE complete Node.js Express server.js file.

        ==================================================
        ABSOLUTE OUTPUT RULES
        ==================================================

        - Output JavaScript source code ONLY.
        - Do NOT use Markdown.
        - Do NOT use ``` fences.
        - Do NOT provide explanations.
        - Do NOT put text before the JavaScript.
        - Do NOT put text after the JavaScript.
        - The result must be directly executable using:

          node server.js

        ==================================================
        APPLICATION INFORMATION
        ==================================================

        Backend type:
        %s

        Vulnerability ID:
        %s

        Vulnerability name:
        %s

        Category:
        %s

        Severity:
        %s

        Description:
        %s

        Simulation endpoint:
        %s

        Detection concept:
        %s

        ==================================================
        NODE.JS REQUIREMENTS
        ==================================================

        1. Use Node.js and Express.

        2. The application MUST listen on port 8080.

        3. The application MUST contain:

           const express = require("express");

        4. Create the Express application.

        5. Include GET /.

        6. Include the selected vulnerability simulation endpoint:

           %s

        7. The application must start successfully using:

           node server.js

        ==================================================
        REALISTIC FRONTEND REQUIREMENTS
        ==================================================

        The root "/" page MUST look like a genuine professional
        production web application.

        IMPORTANT:

        DO NOT display text such as:

        - "fake login page"
        - "honeypot"
        - "cybersecurity honeypot"
        - "security lab"
        - "attack simulation"
        - "vulnerability simulation"
        - "this is a fake website"

        An external visitor should see a believable application.

        Create a polished interface containing:

        - professional navigation/header
        - realistic company or product branding
        - responsive layout
        - realistic forms
        - buttons
        - cards or panels
        - professional typography
        - realistic placeholder data
        - realistic success/error messages
        - footer
        - hover effects where appropriate
        - clean modern CSS
        - mobile-friendly design

        The website should look like a real enterprise application.

        ALL frontend HTML and CSS MUST be embedded directly
        inside server.js.

        Do NOT depend on:

        - external CSS files
        - external JavaScript files
        - CDN resources
        - external fonts
        - external images
        - external APIs
        - internet connectivity

        The application must work completely offline.

        ==================================================
        VULNERABILITY-SPECIFIC FRONTEND
        ==================================================

        The frontend should naturally expose the selected
        simulation endpoint through a realistic interface.

        Examples:

        SQL Injection:
        Provide a realistic login/account interface that sends
        username and password information to the login endpoint.

        Cross-Site Scripting:
        Provide a realistic search interface using the search
        endpoint.

        Path Traversal:
        Provide a realistic document/download interface.

        Command Injection:
        Provide a realistic network diagnostic or ping interface.

        Authentication Bypass:
        Provide a realistic administrator login or admin portal.

        File Upload:
        Provide a realistic document upload interface.

        SSRF:
        Provide a realistic URL fetching/integration interface.

        Admin Panel:
        Provide a realistic enterprise administration dashboard.

        The selected endpoint must actually be reachable from
        the frontend.

        ==================================================
        SAFETY REQUIREMENTS
        ==================================================

        This is a defensive honeypot.

        The vulnerability MUST ONLY be simulated.

        NEVER:

        - execute operating system commands
        - use child_process
        - use exec
        - use execSync
        - use spawn
        - use spawnSync
        - execute shell commands
        - read arbitrary files
        - write arbitrary files
        - access real credentials
        - access environment secrets
        - connect to a real database
        - make outbound network requests
        - perform real SSRF
        - attack another system
        - access the Docker socket
        - execute attacker supplied JavaScript
        - execute attacker supplied code
        - store attacker uploaded files permanently

        All vulnerability behavior must be simulated.

        For example:

        SQL injection:
        Return a fake database error or fake authentication
        response.

        Path traversal:
        Return a fake file listing or fake file content.

        XSS:
        Return a safe simulated reflected response.

        Command injection:
        Return fake command output without executing anything.

        SSRF:
        Return a fake response without making a network request.

        File upload:
        Pretend that a file was uploaded without actually
        storing the attacker file.

        ==================================================
        REQUEST INPUT SAFETY
        ==================================================

        NEVER call string methods directly on an untrusted
        request parameter.

        BAD:

        req.query.userInput.includes(";")

        GOOD:

        const userInput = String(
            req.query.userInput ||
            req.query.username ||
            ""
        );

        For request body parameters use:

        const userInput = String(
            req.body.userInput ||
            req.body.username ||
            ""
        );

        Every user-controlled value MUST have a safe default.

        The application MUST NOT crash when parameters are missing.

        ==================================================
        REQUEST BODY SUPPORT
        ==================================================

        If POST forms are used, include:

        app.use(express.urlencoded({ extended: false }));
        app.use(express.json());

        ==================================================
        MANDATORY REQUEST LOGGING
        ==================================================

        EVERY incoming HTTP request MUST produce EXACTLY ONE
        JSON log line using console.log().

        The JSON object MUST contain:

        {
          "type": "http_request",
          "timestamp": "...",
          "sourceIp": "...",
          "method": "...",
          "path": "...",
          "query": "...",
          "userAgent": "...",
          "statusCode": 200
        }

        The timestamp MUST use:

        new Date().toISOString()

        The source IP MUST come from:

        req.ip

        The path MUST come from:

        req.path

        The query MUST contain the complete query string.

        The User-Agent MUST come from:

        req.headers["user-agent"]

        Use Express middleware so that ALL routes are logged.

        Use this implementation pattern:

        app.use((req, res, next) => {
            const originalSend = res.send;

            res.send = function(body) {

                console.log(JSON.stringify({
                    type: "http_request",
                    timestamp: new Date().toISOString(),
                    sourceIp: req.ip,
                    method: req.method,
                    path: req.path,
                    query: req.originalUrl.includes("?")
                        ? req.originalUrl.substring(
                            req.originalUrl.indexOf("?") + 1
                          )
                        : "",
                    userAgent:
                        req.headers["user-agent"] || "",
                    statusCode: res.statusCode
                }));

                return originalSend.call(this, body);
            };

            next();
        });

        The final implementation may be equivalent to this
        pattern, but every request must produce exactly one
        structured JSON log line.

        ==================================================
        HTML REQUIREMENT
        ==================================================

        The generated server.js MUST contain a real HTML
        interface.

        It MUST contain:

        <!DOCTYPE html>

        and:

        <html>

        <head>

        <body>

        Include meaningful visible content.

        Do NOT simply return:

        "Welcome to the fake login page"

        ==================================================
        FINAL VALIDATION REQUIREMENTS
        ==================================================

        The generated JavaScript MUST contain:

        - require("express") or require('express')
        - express()
        - port 8080
        - GET /
        - the selected endpoint
        - console.log
        - JSON.stringify
        - HTML
        - CSS
        - safe vulnerability simulation

        The generated code must be valid JavaScript.

        The generated application must be safe to execute
        inside a restricted Docker container.

        ==================================================
        FINAL OUTPUT
        ==================================================

        Return ONLY the complete server.js source code.

        """.formatted(
                backendType,
                vulnerabilityId,
                vulnerability.getName(),
                vulnerability.getCategory(),
                vulnerability.getSeverity(),
                vulnerability.getDescription(),
                vulnerability.getSimulationEndpoint(),
                vulnerability.getDetectionPattern(),
                vulnerability.getSimulationEndpoint()
        );

        // Generate server.js using Gemini
        String generated = geminiService.generate(prompt);

        generated = cleanGeneratedCode(generated);

        // ==================================================
        // BASIC VALIDATION
        // ==================================================

        if (generated.isBlank()) {
            throw new RuntimeException(
                    "Gemini returned empty JavaScript."
            );
        }

        if (!generated.contains("require(")) {
            throw new RuntimeException(
                    "Generated honeypot does not appear to be valid Node.js."
            );
        }

        if (!generated.contains("express")) {
            throw new RuntimeException(
                    "Generated honeypot does not use Express."
            );
        }

        if (!generated.contains("8080")) {
            throw new RuntimeException(
                    "Generated honeypot does not listen on port 8080."
            );
        }

        if (!generated.contains("console.log")) {
            throw new RuntimeException(
                    "Generated honeypot does not contain request logging."
            );
        }

        if (!generated.contains("JSON.stringify")) {
            throw new RuntimeException(
                    "Generated honeypot does not produce structured JSON logs."
            );
        }

        if (!generated.contains("res.send")) {
            throw new RuntimeException(
                    "Generated honeypot does not appear to return HTTP responses."
            );
        }

        // Make sure Gemini actually generated a frontend
        String lowerCode = generated.toLowerCase();

        if (!lowerCode.contains("<html")) {
            throw new RuntimeException(
                    "Generated honeypot does not contain an HTML frontend."
            );
        }

        if (!lowerCode.contains("<body")) {
            throw new RuntimeException(
                    "Generated honeypot does not contain a webpage body."
            );
        }

        if (!lowerCode.contains("<style")) {
            throw new RuntimeException(
                    "Generated honeypot does not contain embedded CSS."
            );
        }

        // Make sure the selected endpoint appears in the code
        String endpoint =
                vulnerability.getSimulationEndpoint();

        if (!generated.contains(endpoint)) {
            throw new RuntimeException(
                    "Generated honeypot does not contain the required vulnerability endpoint: "
                            + endpoint
            );
        }

        // ==================================================
        // SAVE SERVER.JS
        // ==================================================

        Path directory =
                Path.of("generated_honeypots")
                        .resolve(vulnerabilityId);

        Files.createDirectories(directory);

        Path server =
                directory.resolve("server.js");

        Files.writeString(
                server,
                generated
        );

        return server.toString();
    }

    /**
     * Remove accidental Markdown code fences or obvious
     * explanatory text from Gemini's response.
     */
    private String cleanGeneratedCode(
            String generated
    ) {

        if (generated == null) {
            return "";
        }

        String code = generated.trim();

        // Remove Markdown fences
        code = code
                .replace("```javascript", "")
                .replace("```js", "")
                .replace("```node", "")
                .replace("```JavaScript", "")
                .replace("```", "")
                .trim();

        /*
         * Occasionally an AI model may add an explanation
         * before the actual JavaScript.
         *
         * If we see a common explanation prefix, try to
         * locate the beginning of the JavaScript.
         */

        String[] badPrefixes = {
                "Here is the",
                "Here’s the",
                "Here is a",
                "Here is your",
                "This server.js file",
                "This code",
                "The following code",
                "Below is"
        };

        for (String prefix : badPrefixes) {

            int index = code.indexOf(prefix);

            if (index == 0) {

                int jsStart = code.indexOf("const express");

                if (jsStart >= 0) {
                    code = code.substring(jsStart);
                }
            }
        }

        return code.trim();
    }

    /**
     * Backward-compatible method.
     */
    public String generateHoneypot(
            String backendType
    ) throws Exception {

        return generateHoneypot(
                backendType,
                "WEB-ADMIN-001"
        );
    }
}