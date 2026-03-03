package com.sk.openspecai.metadata;

public sealed interface ChunkMetadata permits SchemaMetadata, PathMetadata, GlobalMetadata {

    String specId();

    String chunkType();
}
