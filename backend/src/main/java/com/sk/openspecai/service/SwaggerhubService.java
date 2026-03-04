package com.sk.openspecai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.sk.openspecai.auth.TokenProvider;

@Service
public class SwaggerhubService {

    private HttpClient httpClient;
    private TokenProvider tokenProvider;

    public SwaggerhubService(
            HttpClient httpClient,
            TokenProvider tokenProvider) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
    }

    public void connect() throws Exception {
        String apiKey = tokenProvider.getToken();
        String owner = tokenProvider.getOwner();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.swaggerhub.com/apis/" + owner))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode == 401 || statusCode == 403) {
            throw new Exception("Invalid SwaggerHub API key");
        }

        if (statusCode != 200 && statusCode != 204) {
            throw new Exception("SwaggerHub API unavailable");
        }
    }
}
