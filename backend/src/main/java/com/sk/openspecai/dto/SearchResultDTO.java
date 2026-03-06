package com.sk.openspecai.dto;

import java.util.Map;

// DTO for Chroma search results
public record SearchResultDTO(String embeddingId, String content, Map<String, Object> metadata) {
}