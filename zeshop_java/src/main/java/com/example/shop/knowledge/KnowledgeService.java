package com.example.shop.knowledge;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("!local-demo")
@Service
public class KnowledgeService {
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public KnowledgeService(VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    public int sync(String sourceType, String sourceId, String storeId, String title, String content) {
        // PgVectorStore invokes the configured EmbeddingModel when documents are added.
        embeddingModel.embed(content);
        Document source = new Document(content, Map.of(
                "source_type", sourceType,
                "source_id", sourceId,
                "store_id", storeId,
                "active", true,
                "title", title));
        List<Document> chunks = splitter.split(List.of(source));
        vectorStore.add(chunks);
        return chunks.size();
    }
}
