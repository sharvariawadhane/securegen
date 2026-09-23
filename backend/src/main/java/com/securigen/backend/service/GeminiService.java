package com.securigen.backend.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;

    private static final String MODEL = "gemini-3.8-flash";

    public GeminiService() {
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY environment variable is not set"
            );
        }

        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public String generate(String prompt) {

        GenerateContentResponse response =
                client.models.generateContent(
                        MODEL,
                        prompt,
                        null
                );

        String text = response.text();

        if (text == null || text.isBlank()) {
            throw new IllegalStateException(
                    "Gemini returned an empty response"
            );
        }

        return text;
    }
}