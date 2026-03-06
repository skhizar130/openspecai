package com.sk.openspecai.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import com.sk.openspecai.dto.SearchResultDTO;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class EmbeddingService {

        private final EmbeddingModel embeddingModel;
        private final EmbeddingStore<TextSegment> embeddingStore;

        // Constructor: Spring injects the embedding model and store
        public EmbeddingService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
                this.embeddingModel = embeddingModel;
                this.embeddingStore = embeddingStore;
        }

        // Add a text chunk with metadata to the embedding store
        public void add(String text, Map<String, String> metadata) {
                var embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, TextSegment.from(text, Metadata.from(metadata)));
        }

        // Retrieve all chunks for a given specId
        public List<SearchResultDTO> retrieveSpecChunks(String specId) {
                Filter specIdFilter = metadataKey("specId").isEqualTo(specId);
                return retrieveChunksWithFilter(specIdFilter);
        }

        // Retrieve and join all chunks for a specific path and method
        public String retrievePathChunks(String specId, String path, String method) {
                Filter typeFilter = metadataKey("chunkType").isEqualTo("PATH_METHOD");
                Filter specIdFilter = metadataKey("specId").isEqualTo(specId);
                Filter pathFilter = metadataKey("path").isEqualTo(path);
                Filter methodFilter = metadataKey("method").isEqualTo(method.toLowerCase());

                // Combine filters using AND logic
                Filter combinedFilter = Filter.and(typeFilter,
                                path.isEmpty() ? specIdFilter
                                                : Filter.and(specIdFilter,
                                                                method.isEmpty() ? pathFilter
                                                                                : Filter.and(pathFilter,
                                                                                                methodFilter)));

                List<SearchResultDTO> result = retrieveChunksWithFilter(combinedFilter);

                // Join all chunks with "---" separator
                return result.stream()
                                .map(SearchResultDTO::content)
                                .collect(Collectors.joining("\n---\n"));
        }

        // Retrieve and join all chunks for a specific schema
        public String retrieveSchemaChunks(String specId, String schemaName) {
                Filter typeFilter = metadataKey("chunkType").isEqualTo("SCHEMA");
                Filter specIdFilter = metadataKey("specId").isEqualTo(specId);
                Filter schemaNameFilter = metadataKey("schemaName").isEqualTo(schemaName);

                Filter combinedFilter = Filter.and(typeFilter,
                                schemaName.isEmpty() ? specIdFilter : Filter.and(specIdFilter, schemaNameFilter));

                List<SearchResultDTO> result = retrieveChunksWithFilter(combinedFilter);

                return result.stream()
                                .map(SearchResultDTO::content)
                                .collect(Collectors.joining("\n---\n"));
        }

        public SearchResultDTO retrieveInfoChunk(String specId) throws Exception {
                Filter typeFilter = metadataKey("chunkType").isEqualTo("INFO");
                Filter specIdFilter = metadataKey("specId").isEqualTo(specId);

                Filter combinedFilter = Filter.and(typeFilter, specIdFilter);

                List<SearchResultDTO> results = retrieveChunksWithFilter(combinedFilter);

                if (results.size() == 0) {
                        throw new Exception("Info not found");
                }

                return results.get(0);
        }

        // Internal method: search embeddings using a filter
        private List<SearchResultDTO> retrieveChunksWithFilter(Filter filter) {
                float[] zeroVector = new float[1536]; // dummy vector for text-embedding-3-small
                Embedding dummyEmbedding = Embedding.from(zeroVector);

                EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                                .queryEmbedding(dummyEmbedding)
                                .maxResults(10)
                                .filter(filter)
                                .build();

                EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

                // Map the results to DTO objects
                return searchResult.matches()
                                .stream().map(match -> new SearchResultDTO(
                                                match.embeddingId(),
                                                match.embedded().text(),
                                                match.embedded().metadata().toMap()))
                                .toList();
        }

        public void deletaAllChunks(String specId) {
                Filter specIdFilter = metadataKey("specId").isEqualTo(specId);

                List<SearchResultDTO> resultDTOs = retrieveChunksWithFilter(specIdFilter);

                resultDTOs.stream().forEach(result -> {
                        embeddingStore.remove(result.embeddingId());
                });
        }
}