package com.sk.openspecai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.springframework.stereotype.Service;

@Service
public class SwaggerhubService {

    private HttpClient httpClient;

    public SwaggerhubService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void connect(String apiKey, String owner) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.swaggerhub.com/apis/" + owner))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        int statusCode = response.statusCode();

        System.out.println("statusCode: " + statusCode);
        System.out.println("message" + response.body());

        if (statusCode == 401 || statusCode == 403) {
            throw new Exception("Invalid SwaggerHub API key");
        }

        if (statusCode != 200 && statusCode != 204) {
            throw new Exception("SwaggerHub API unavailable");
        }

        // encryptAndPersist(apiKey)

    }
}
