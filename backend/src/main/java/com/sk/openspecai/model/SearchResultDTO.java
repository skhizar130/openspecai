package com.sk.openspecai.model;

import java.util.Map;

// DTO for Chroma search results
public record SearchResultDTO(String embeddingId, String content, Map<String, Object> metadata) {
}