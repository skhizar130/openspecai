package com.sk.openspecai.metadata;

public record SchemaMetadata(
                String specId,
                String chunkType,
                String schemaName) implements ChunkMetadata {

}
