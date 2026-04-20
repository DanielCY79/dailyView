package com.daniel.dailyView.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.daniel.dailyView.exception.ExternalDataAccessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonHttpClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JsonHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String url, TypeReference<T> responseType) {
        return get(url, Map.of(), responseType);
    }

    public <T> T get(String url, Map<String, String> headers, TypeReference<T> responseType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "dailyView/0.0.1");

        headers.forEach(builder::header);

        HttpRequest request = builder.GET().build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalDataAccessException(
                        "External API request failed with status %s for %s".formatted(response.statusCode(), url));
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalDataAccessException("External API request interrupted for " + url, ex);
        } catch (JsonProcessingException ex) {
            throw new ExternalDataAccessException("Failed to parse API response for " + url, ex);
        } catch (IOException ex) {
            throw new ExternalDataAccessException("External API request failed for " + url, ex);
        }
    }
}
