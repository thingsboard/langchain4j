package dev.langchain4j.model.googleai;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeminiCacheManager {

    private static final Logger log = LoggerFactory.getLogger(GeminiCacheManager.class);

    private final GeminiService geminiService;
    private final ConcurrentMap<String, CachedContentMetadata> cachedContents;

    public GeminiCacheManager(GeminiService geminiService) {
        this.geminiService = geminiService;

        GoogleAiListCachedContentsRequest listCachedContentsRequest = new GoogleAiListCachedContentsRequest();
        listCachedContentsRequest.setPageSize(1000);
        this.cachedContents = new ConcurrentHashMap<>(Optional.ofNullable(geminiService.listCachedContents(listCachedContentsRequest)
                        .getCachedContents()).orElse(Collections.emptyList()).stream()
                .map(CachedContentMetadata::new)
                .collect(Collectors.toConcurrentMap(CachedContentMetadata::getKey, Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(CachedContentMetadata::getExpirationTime)))));
        log.debug("Loaded existing cached contents: {}", cachedContents);
    }

    public String getOrCreateCached(String key, Duration ttl, GeminiContent content,
                                    GeminiTool tools, GeminiToolConfig toolConfig, String model) {
        CachedContentMetadata result = cachedContents.compute(key, (__, cachedContent) -> {
            if (cachedContent != null) {
                if (cachedContent.hasExpired()) {
                    log.debug("Cached content for key '{}' has expired: {}", key, cachedContent);
                } else if (cachedContent.checksumMatches(content, tools, toolConfig)) {
                    if (cachedContent.hasAlmostExpired()) {
                        log.debug("Using existing cached content for key '{}' and extending TTL due to approaching expiration: {}", key, cachedContent);
                        try {
                            cachedContent = extendTtl(cachedContent, ttl);
                            log.debug("Extended TTL for cached content '{}': {}", key, cachedContent);
                            return cachedContent;
                        } catch (Exception e) {
                            log.error("Failed to extend TTL for cached content '{}': {}", key, cachedContent, e);
                            return null;
                        }
                    }
                    log.debug("Using existing cached content for key '{}': {}", key, cachedContent);
                    return cachedContent;
                } else {
                    log.debug("Cached content for key '{}' has different checksum, deleting: {}", key, cachedContent);
                    try {
                        deleteCachedContent(cachedContent);
                    } catch (Exception e) {
                        log.error("Failed to delete cached content for key '{}': {}", key, cachedContent, e);
                        return null;
                    }
                }
            }
            try {
                return createCachedContent(key, ttl, content, tools, toolConfig, model);
            } catch (Exception e) {
                log.error("Failed to create cached content for key '{}': {}", key, e.getMessage(), e);
                return null;
            }
        });
        return result != null ? result.getId() : null;
    }

    public void invalidate(String key) {
        CachedContentMetadata removed = cachedContents.remove(key);
        if (removed != null) {
            log.error("Invalidated cached content for key '{}': {}", key, removed);
        }
    }

    private CachedContentMetadata extendTtl(CachedContentMetadata cachedContent, Duration ttl) {
        GeminiCachedContent updated = GeminiCachedContent.builder()
                .ttl(ttl.toSeconds() + "s")
                .build();
        String cacheName = StringUtils.removeStart(cachedContent.getId(), "cachedContents/");
        updated = geminiService.updateCachedContent(cacheName, updated);
        CachedContentMetadata newMetadata = new CachedContentMetadata(updated);
        return newMetadata;
    }

    private void deleteCachedContent(CachedContentMetadata cachedContent) {
        log.debug("Deleting cached content for key '{}': {}", cachedContent.getKey(), cachedContent);
        geminiService.deleteCachedContent(StringUtils.removeStart(cachedContent.getId(), "cachedContents/"));
    }

    private CachedContentMetadata createCachedContent(String key, Duration ttl, GeminiContent content,
                                                      GeminiTool tools, GeminiToolConfig toolConfig, String model) {
        GeminiCachedContent cachedContent = GeminiCachedContent.builder()
                .systemInstruction(content)
                .tools(tools != null ? Collections.singletonList(tools) : null)
                .toolConfig(toolConfig)
                .ttl(ttl.toSeconds() + "s")
                .displayName(key + ":" + getChecksum(content, tools, toolConfig))
                .build();
        cachedContent = geminiService.createCachedContent(model, cachedContent);

        CachedContentMetadata newCachedContent = new CachedContentMetadata(cachedContent);
        log.debug("Created new cached content for key '{}': {}", key, cachedContent);
        return newCachedContent;
    }

    private static String getChecksum(GeminiContent content, GeminiTool tools, GeminiToolConfig toolConfig) {
        var sb = new StringBuilder();
        sb.append(content.parts().stream()
                .map(GeminiContent.GeminiPart::text)
                .collect(Collectors.joining(System.lineSeparator())));
        if (tools != null) {
            sb.append(System.lineSeparator()).append(Json.toJson(tools));
        }
        if (toolConfig != null) {
            sb.append(System.lineSeparator()).append(Json.toJson(toolConfig));
        }
        return DigestUtils.sha256Hex(sb.toString());
    }

    private static class CachedContentMetadata {

        String id;
        String key;
        String checksum;
        Instant expirationTime;

        CachedContentMetadata(GeminiCachedContent cachedContent) {
            this.id = cachedContent.name();
            String[] parts = cachedContent.displayName().split(":");
            this.key = parts[0];
            this.checksum = parts.length == 2 ? parts[1] : "undefined";
            this.expirationTime = Instant.parse(cachedContent.expireTime());
        }

        public String getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public Instant getExpirationTime() {
            return expirationTime;
        }

        public boolean hasAlmostExpired() {
            return expirationTime.minusSeconds(60).isBefore(Instant.now());
        }

        public boolean hasExpired() {
            return expirationTime.isBefore(Instant.now());
        }

        public boolean checksumMatches(GeminiContent content, GeminiTool tools, GeminiToolConfig toolConfig) {
            return this.checksum.equals(GeminiCacheManager.getChecksum(content, tools, toolConfig));
        }

        @Override
        public String toString() {
            return "CachedContentMetadata{" +
                    "id='" + id + '\'' +
                    ", key='" + key + '\'' +
                    ", checksum='" + checksum + '\'' +
                    ", expirationTime=" + expirationTime +
                    '}';
        }

    }

}
