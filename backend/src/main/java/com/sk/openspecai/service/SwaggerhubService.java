package com.sk.openspecai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.sk.openspecai.auth.TokenProvider;
import com.sk.openspecai.model.SpecInfo;

@Service
public class SwaggerhubService {

    private HttpClient httpClient;
    private TokenProvider tokenProvider;
    private SpecStorageService specStorageService;
    private ParsingService parsingService;

    public SwaggerhubService(
            HttpClient httpClient,
            TokenProvider tokenProvider,
            SpecStorageService specStorageService,
            EmbeddingService embeddingService,
            ParsingService parsingService) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.specStorageService = specStorageService;
        this.parsingService = parsingService;
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

    public void publish(String specId) throws Exception {
        String yaml = specStorageService.readYaml(specId, true);

        SpecInfo specInfo = parsingService.retriveInfo(specId);

        String owner = tokenProvider.getOwner();
        String apiKey = tokenProvider.getToken();
        String apiName = specInfo.name();
        String version = specInfo.version();
        URI uri = URI.create("https://api.swaggerhub.com/apis/" + owner + "/" +
                apiName + "?isPrivate=true&version=" + version);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/yaml")
                .POST(HttpRequest.BodyPublishers.ofString(yaml))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode == 200 || statusCode == 201) {
            System.out.println("statusCode: " + statusCode);
            System.out.println("Response: " + response.body());
        } else {
            // throw meaningful exception
            throw new Exception("SwaggerHub publish failed: " + statusCode + " " +
                    response.body());
        }
    }
}
