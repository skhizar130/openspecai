package com.sk.openspecai.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.sk.openspecai.auth.TokenProvider;
import com.sk.openspecai.excpetion.InvalidSwaggerHubTokenException;
import com.sk.openspecai.excpetion.OwnerNotFoundException;
import com.sk.openspecai.excpetion.SwaggerHubPublishException;
import com.sk.openspecai.excpetion.SwaggerHubUnavailableException;
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

    public void connect() {
        String apiKey = tokenProvider.getToken();
        String owner = tokenProvider.getOwner();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.swaggerhub.com/apis/" + owner))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 401 || statusCode == 403) {
                throw new InvalidSwaggerHubTokenException("Invalid SwaggerHub API key");
            }

            if (statusCode == 404) {
                throw new OwnerNotFoundException("Owner not found");
            }

            if (statusCode != 200 && statusCode != 204) {
                throw new SwaggerHubUnavailableException("SwaggerHub API unavailable");
            }
        } catch (IOException | InterruptedException e) {
            throw new SwaggerHubUnavailableException("Failed to connect to SwaggerHub: " + e.getMessage());
        }
    }

    public void publish(String specId) {
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

        HttpResponse<String> response;
        try {
            response = httpClient.send(request,
                    BodyHandlers.ofString());

            int statusCode = response.statusCode();

            if (statusCode == 200 || statusCode == 201) {
                System.out.println("statusCode: " + statusCode);
                System.out.println("Response: " + response.body());
            } else {
                throw new SwaggerHubPublishException("Failed to publish to SwaggerHub: " + statusCode + " " +
                        response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new SwaggerHubUnavailableException("Failed to connect to SwaggerHub: " + e.getMessage());
        }

    }

    public String preview(String specId) {
        SpecInfo specInfo = parsingService.retriveInfo(specId);

        String owner = tokenProvider.getOwner();
        String apiName = specInfo.name();
        String version = specInfo.version();
        URI uri = URI.create("https://app.swaggerhub.com/apis/" + owner + "/" +
                apiName + "/" + version);

        return uri.toString();
    }
}
