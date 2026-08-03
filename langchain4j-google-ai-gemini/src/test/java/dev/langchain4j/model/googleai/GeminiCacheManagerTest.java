package dev.langchain4j.model.googleai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeminiCacheManagerTest {

    private static final String CACHE_KEY = "assistant-cache";
    private static final String MODEL = "gemini-test";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final GeminiService geminiService = mock(GeminiService.class);
    private final GeminiContent systemInstruction =
            new GeminiContent(List.of(GeminiContent.GeminiPart.ofText("You are a test assistant.")), "model");
    private final AtomicInteger idSequence = new AtomicInteger();

    @BeforeEach
    void setUp() {
        stubListCachedContents();
        when(geminiService.createCachedContent(anyString(), any())).thenAnswer(invocation -> {
            String modelName = invocation.getArgument(0);
            GeminiCachedContent requested = invocation.getArgument(1);
            return GeminiCachedContent.builder()
                    .name("cachedContents/test-" + idSequence.incrementAndGet())
                    .displayName(requested.displayName())
                    .model("models/" + modelName)
                    .expireTime(Instant.now().plus(TTL).toString())
                    .build();
        });
    }

    @Test
    void shouldCreateCachedContentWithEffectiveKeyAsDisplayName_whenNoneExists() {
        // Given
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);

        // When
        String id = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

        // Then
        assertThat(id).isEqualTo("cachedContents/test-1");
        ArgumentCaptor<GeminiCachedContent> captor = ArgumentCaptor.forClass(GeminiCachedContent.class);
        verify(geminiService).createCachedContent(eq(MODEL), captor.capture());
        String displayName = captor.getValue().displayName();
        assertThat(displayName)
                .isEqualTo(GeminiCacheManager.prepareEffectiveKey(CACHE_KEY, systemInstruction, null, null, MODEL))
                .matches("assistant-cache:[0-9a-f]{64}");
    }

    @Test
    void shouldReuseCachedContent_whenCalledTwiceWithSameInputs() {
        // Given
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);

        // When
        String firstId = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);
        String secondId = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

        // Then
        assertThat(secondId).isEqualTo(firstId);
        verify(geminiService, times(1)).createCachedContent(anyString(), any());
    }

    @Test
    void shouldCreateSeparateCachedContents_whenOnlyModelDiffers() {
        // Given
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);

        // When
        String firstId = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, "gemini-3.5-flash");
        String secondId = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, "gemini-3.6-flash");

        // Then
        assertThat(secondId).isNotEqualTo(firstId);
        ArgumentCaptor<GeminiCachedContent> captor = ArgumentCaptor.forClass(GeminiCachedContent.class);
        verify(geminiService, times(2)).createCachedContent(anyString(), captor.capture());
        assertThat(captor.getAllValues().get(0).displayName()).isNotEqualTo(captor.getAllValues().get(1).displayName());
    }

    @Test
    void shouldBoundDisplayNameLength_whenCacheKeyExceedsLimit() {
        // Given
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);
        String longCacheKey = "k".repeat(200);

        // When
        cacheManager.getOrCreateCached(longCacheKey, TTL, systemInstruction, null, null, MODEL);

        // Then
        ArgumentCaptor<GeminiCachedContent> captor = ArgumentCaptor.forClass(GeminiCachedContent.class);
        verify(geminiService).createCachedContent(eq(MODEL), captor.capture());
        String displayName = captor.getValue().displayName();
        assertThat(displayName).hasSize(128);
        assertThat(displayName).startsWith(longCacheKey.substring(0, 63) + ":");
    }

    @Test
    void shouldReuseLoadedCachedContent_whenModelMatches() {
        // Given
        stubListCachedContents(preexistingCachedContent("models/" + MODEL, Instant.now().plus(TTL)));
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);

        // When
        String id = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

        // Then
        assertThat(id).isEqualTo("cachedContents/preexisting");
        verify(geminiService, never()).createCachedContent(anyString(), any());
    }

    @Test
    void shouldRecreateCachedContent_whenLoadedEntryWasCreatedForDifferentModel() {
        // Given
        stubListCachedContents(preexistingCachedContent("models/other-model", Instant.now().plus(TTL)));
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);

        // When
        String id = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

        // Then
        assertThat(id).isEqualTo("cachedContents/test-1");
        verify(geminiService).createCachedContent(eq(MODEL), any());
    }

    @Test
    void shouldRecreateCachedContent_whenEvicted() {
        // Given
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);
        String firstId = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

        // When
        cacheManager.evict(CACHE_KEY, systemInstruction, null, null, MODEL);
        String secondId = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

        // Then
        assertThat(firstId).isEqualTo("cachedContents/test-1");
        assertThat(secondId).isEqualTo("cachedContents/test-2");
        verify(geminiService, times(2)).createCachedContent(anyString(), any());
    }

    @Test
    void shouldExtendTtl_whenCachedContentIsAlmostExpired() {
        // Given
        stubListCachedContents(preexistingCachedContent("models/" + MODEL, Instant.now().plusSeconds(30)));
        when(geminiService.updateCachedContent(eq("preexisting"), any()))
                .thenReturn(preexistingCachedContent("models/" + MODEL, Instant.now().plus(TTL)));
        GeminiCacheManager cacheManager = new GeminiCacheManager(geminiService);

        // When
        String id = cacheManager.getOrCreateCached(CACHE_KEY, TTL, systemInstruction, null, null, MODEL);

        // Then
        assertThat(id).isEqualTo("cachedContents/preexisting");
        verify(geminiService).updateCachedContent(eq("preexisting"), any());
        verify(geminiService, never()).createCachedContent(anyString(), any());
    }

    private void stubListCachedContents(GeminiCachedContent... cachedContents) {
        GoogleAiListCachedContentsResponse response = new GoogleAiListCachedContentsResponse();
        response.setCachedContents(List.of(cachedContents));
        when(geminiService.listCachedContents(any())).thenReturn(response);
    }

    private GeminiCachedContent preexistingCachedContent(String model, Instant expireTime) {
        return GeminiCachedContent.builder()
                .name("cachedContents/preexisting")
                .displayName(GeminiCacheManager.prepareEffectiveKey(CACHE_KEY, systemInstruction, null, null, MODEL))
                .model(model)
                .expireTime(expireTime.toString())
                .build();
    }

}
