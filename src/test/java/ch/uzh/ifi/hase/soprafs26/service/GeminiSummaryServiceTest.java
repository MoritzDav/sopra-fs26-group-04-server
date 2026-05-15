package ch.uzh.ifi.hase.soprafs26.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiSummaryServiceTest {

    @Test
    void summarizeText_missingApiKey_throwsInternalServerError() {
        GeminiSummaryService service = new GeminiSummaryService(mock(HttpClient.class), new ObjectMapper());
        ReflectionTestUtils.setField(service, "geminiApiKey", "");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.summarizeText("Some text"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    void summarizeText_validGeminiResponse_returnsSummary() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);

        String geminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "Short summary."},
                          {"text": "Second paragraph."}
                        ]
                      }
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(geminiResponse);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);

        GeminiSummaryService service = new GeminiSummaryService(httpClient, new ObjectMapper());
        ReflectionTestUtils.setField(service, "geminiApiKey", "test-key");
        ReflectionTestUtils.setField(service, "geminiModel", "gemini-flash-latest");

        String result = service.summarizeText("This is input text.");

        assertEquals("Short summary.\n\nSecond paragraph.", result);
    }

    @Test
    void summarizeText_geminiErrorResponse_throwsBadGateway() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);

        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\":{\"message\":\"Invalid request\"}}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);

        GeminiSummaryService service = new GeminiSummaryService(httpClient, new ObjectMapper());
        ReflectionTestUtils.setField(service, "geminiApiKey", "test-key");
        ReflectionTestUtils.setField(service, "geminiModel", "gemini-flash-latest");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.summarizeText("Input text"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }
}
