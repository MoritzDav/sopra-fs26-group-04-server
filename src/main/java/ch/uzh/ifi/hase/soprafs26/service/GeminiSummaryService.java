package ch.uzh.ifi.hase.soprafs26.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GeminiSummaryService {

    private static final int MAX_INPUT_CHARS = 15000;
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String geminiModel;

    public GeminiSummaryService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String summarizeText(String text) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Gemini API key is not configured");
        }

        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No extractable text found in PDF");
        }

        String normalizedText = text.length() > MAX_INPUT_CHARS
                ? text.substring(0, MAX_INPUT_CHARS)
                : text;

        String prompt = "Summarize the following PDF content in clear concise paragraphs and short bullet points. " +
                "Keep it factual and easy to read.\n\n" + normalizedText;

        try {
            ObjectNode requestRoot = objectMapper.createObjectNode();
            ObjectNode contentNode = objectMapper.createObjectNode();
            contentNode.putArray("parts")
                .add(objectMapper.createObjectNode().put("text", prompt));
            requestRoot.putArray("contents").add(contentNode);

            String requestBody = objectMapper.writeValueAsString(requestRoot);

                String endpoint = String.format(
                    "%s/models/%s:generateContent",
                    GEMINI_BASE_URL,
                    geminiModel);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                String errorDetail = extractGeminiErrorMessage(response.body());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini API request failed with status " + response.statusCode() +
                        (errorDetail.isBlank() ? "" : ": " + errorDetail));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Gemini API returned no summary text");
            }

            StringBuilder summary = new StringBuilder();
            for (JsonNode part : parts) {
                String partText = part.path("text").asText("");
                if (!partText.isBlank()) {
                    if (!summary.isEmpty()) {
                        summary.append("\n\n");
                    }
                    summary.append(partText.trim());
                }
            }

            if (summary.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Gemini API returned empty summary text");
            }

            return summary.toString();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to call Gemini API");
        }
    }

    private String extractGeminiErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("").trim();
            if (!message.isBlank()) {
                return message;
            }
        } catch (IOException ignored) {
        }

        String compact = responseBody.replaceAll("\\s+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "..." : compact;
    }
}
