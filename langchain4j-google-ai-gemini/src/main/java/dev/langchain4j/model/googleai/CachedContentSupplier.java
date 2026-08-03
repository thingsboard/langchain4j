package dev.langchain4j.model.googleai;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

class CachedContentSupplier implements Supplier<String> {

    private final GeminiCacheManager cacheManager;
    private final String cacheKey;
    private final Duration ttl;
    private final GeminiContent systemInstruction;
    private final List<GeminiTool> tools;
    private final GeminiToolConfig toolConfig;
    private final String model;

    CachedContentSupplier(GeminiCacheManager cacheManager, String cacheKey, Duration ttl, GeminiContent systemInstruction,
                          List<GeminiTool> tools, GeminiToolConfig toolConfig, String model) {
        this.cacheManager = cacheManager;
        this.cacheKey = cacheKey;
        this.ttl = ttl;
        this.systemInstruction = systemInstruction;
        this.tools = tools;
        this.toolConfig = toolConfig;
        this.model = model;
    }

    @Override
    public String get() {
        return cacheManager.getOrCreateCached(cacheKey, ttl, systemInstruction, tools, toolConfig, model);
    }

    void evict() {
        cacheManager.evict(cacheKey, systemInstruction, tools, toolConfig, model);
    }

}
