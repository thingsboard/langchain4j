package dev.langchain4j.model.googleai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.exception.HttpException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeminiCacheManagerTest {

    private static final String CACHE_KEY = "test-key";
    private static final String MODEL = "gemini-2.0-flash";
    private static final Duration TTL = Duration.ofMinutes(30);

    @Mock
    GeminiService mockGeminiService;

    GeminiCacheManager cacheManager;

    GeminiContent systemInstruction;
    String systemInstructionChecksum;

    @BeforeEach
    void setUp() {
        GoogleAiListCachedContentsResponse emptyResponse = new GoogleAiListCachedContentsResponse();
        when(mockGeminiService.listCachedContents(any())).thenReturn(emptyResponse);
        cacheManager = new GeminiCacheManager(mockGeminiService);
        systemInstruction = new GeminiContent(
                List.of(GeminiContent.GeminiPart.builder().text("You are a helpful assistant").build()),
                GeminiRole.MODEL.toString());
        systemInstructionChecksum = DigestUtils.sha256Hex("You are a helpful assistant");
    }

    private GeminiCachedContent createCachedContentResponse(String key, String checksum, Instant expireTime) {
        return GeminiCachedContent.builder()
                .name("cachedContents/cache-id-123")
                .displayName(key + ":" + checksum)
                .expireTime(expireTime.toString())
                .build();
    }

    @Nested
    class GetOrCreateCached {

        @Test
        void shouldReturnNullAndRemoveEntryWhenCreateCachedContentThrows() {
            // Given
            when(mockGeminiService.createCachedContent(anyString(), any()))
                    .thenThrow(new HttpException(500, "Internal Server Error"));

            // When
            String result = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

            // Then - returns null because create failed
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullAndRemoveEntryWhenExtendTtlThrows() {
            // Given - first call succeeds, creating a cache entry that's almost expired
            Instant almostExpired = Instant.now().plusSeconds(30); // within 60s threshold
            GeminiCachedContent cachedResponse = createCachedContentResponse(
                    CACHE_KEY, systemInstructionChecksum, almostExpired);
            when(mockGeminiService.createCachedContent(anyString(), any()))
                    .thenReturn(cachedResponse);

            // First call - create the entry
            String result1 = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);
            assertThat(result1).isEqualTo("cachedContents/cache-id-123");

            // Given - extendTtl (updateCachedContent) throws
            when(mockGeminiService.updateCachedContent(anyString(), any()))
                    .thenThrow(new HttpException(403, "CachedContent not found"));

            // When - second call should try to extend TTL since it's almost expired and checksum matches
            String result2 = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

            // Then - returns null because extendTtl failed
            assertThat(result2).isNull();
        }

        @Test
        void shouldReturnNullAndRemoveEntryWhenDeleteCachedContentThrows() {
            // Given - first call succeeds with specific content
            GeminiContent originalContent = new GeminiContent(
                    List.of(GeminiContent.GeminiPart.builder().text("original content").build()),
                    GeminiRole.MODEL.toString());
            Instant validExpiry = Instant.now().plusSeconds(300);
            String originalChecksum = DigestUtils.sha256Hex("original content");
            GeminiCachedContent cachedResponse = createCachedContentResponse(
                    CACHE_KEY, originalChecksum, validExpiry);
            when(mockGeminiService.createCachedContent(anyString(), any()))
                    .thenReturn(cachedResponse);

            String result1 = cacheManager.getOrCreateCached(CACHE_KEY, TTL, originalContent, null, null, MODEL);
            assertThat(result1).isEqualTo("cachedContents/cache-id-123");

            // Given - delete throws when checksum doesn't match (different content)
            GeminiContent differentContent = new GeminiContent(
                    List.of(GeminiContent.GeminiPart.builder().text("different content").build()),
                    GeminiRole.MODEL.toString());
            doThrow(new HttpException(403, "CachedContent not found"))
                    .when(mockGeminiService).deleteCachedContent(anyString());

            // When - call with different content (triggers delete due to checksum mismatch)
            String result2 = cacheManager.getOrCreateCached(CACHE_KEY, TTL, differentContent, null, null, MODEL);

            // Then - returns null because delete failed
            assertThat(result2).isNull();
            verify(mockGeminiService).deleteCachedContent(anyString());
        }
    }

    @Nested
    class Invalidate {

        @Test
        void shouldRemoveExistingEntry() {
            // Given - create a cache entry first
            Instant validExpiry = Instant.now().plusSeconds(300);
            GeminiCachedContent cachedResponse = createCachedContentResponse(
                    CACHE_KEY, systemInstructionChecksum, validExpiry);
            when(mockGeminiService.createCachedContent(anyString(), any()))
                    .thenReturn(cachedResponse);
            cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

            // When
            cacheManager.invalidate(CACHE_KEY);

            // Then - next call should create a new entry (not reuse old one)
            cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);
            // createCachedContent called twice total (initial + after invalidation)
            verify(mockGeminiService, org.mockito.Mockito.times(2)).createCachedContent(anyString(), any());
        }

        @Test
        void shouldBeNoOpWhenKeyDoesNotExist() {
            // When/Then - should not throw
            cacheManager.invalidate("non-existent-key");
        }
    }
}
