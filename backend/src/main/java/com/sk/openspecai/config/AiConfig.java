package com.sk.openspecai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

@Configuration
public class AiConfig {

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(@Value("${langchain4j.chroma.url}") String baseUrl,
            @Value("${langchain4j.chroma.collection}") String collectionName) {
        return ChromaEmbeddingStore.builder()
                .baseUrl(baseUrl)
                .apiVersion(ChromaApiVersion.V2)
                .collectionName(collectionName)
                .tenantName("default_tenant")
                .databaseName("default_database")
                .build();
    }
}
