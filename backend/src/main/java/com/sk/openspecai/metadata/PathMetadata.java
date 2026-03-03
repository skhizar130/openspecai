package com.sk.openspecai.metadata;

public record PathMetadata(
                String specId,
                String chunkType,
                String path,
                String method) implements ChunkMetadata {

}
