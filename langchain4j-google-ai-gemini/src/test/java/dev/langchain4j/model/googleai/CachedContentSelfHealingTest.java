package dev.langchain4j.model.googleai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedContentSelfHealingTest {

    private static final String CACHE_KEY = "assistant-cache";
    private static final String MODEL = "gemini-test";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String CACHED_CONTENT_ERROR =
            "Model used by GenerateContent request (models/gemini-test) and CachedContent (models/other) has to be the same.";

    private final GeminiService geminiService = mock(GeminiService.class);
    private final GeminiCacheManager cacheManager = mock(GeminiCacheManager.class);

    private ChatRequest chatRequest() {
        return ChatRequest.builder()
                .messages(SystemMessage.from("You are a test assistant."), UserMessage.from("ping"))
                .modelName(MODEL)
                .build();
    }

    private GoogleAiGeminiChatModel chatModel() {
        return new GoogleAiGeminiChatModel(
                GoogleAiGeminiChatModel.builder()
                        .apiKey("test-api-key")
                        .modelName(MODEL)
                        .cachingConfig(cachingConfig()),
                geminiService);
    }

    private GoogleAiGeminiStreamingChatModel streamingChatModel() {
        return new GoogleAiGeminiStreamingChatModel(
                GoogleAiGeminiStreamingChatModel.builder()
                        .apiKey("test-api-key")
                        .modelName(MODEL)
                        .cachingConfig(cachingConfig()),
                geminiService);
    }

    private GeminiCachingConfig cachingConfig() {
        return GeminiCachingConfig.builder()
                .cacheContents(true)
                .cacheKey(CACHE_KEY)
                .ttl(TTL)
                .cacheManagerProvider(service -> cacheManager)
                .build();
    }

    private static GeminiGenerateContentResponse generateContentResponse(String text) {
        var candidate = new GeminiGenerateContentResponse.GeminiCandidate(
                new GeminiContent(List.of(GeminiContent.GeminiPart.ofText(text)), "model"),
                GeminiGenerateContentResponse.GeminiCandidate.GeminiFinishReason.STOP,
                null,
                null);
        return new GeminiGenerateContentResponse("responseId", MODEL, List.of(candidate),
                GeminiUsageMetadata.builder().build(), null);
    }

    @Test
    void shouldEvictAndRetryOnce_whenGenerateContentFailsWithCachedContentError() {
        // Given
        when(geminiService.generateContent(anyString(), any()))
                .thenThrow(new InvalidRequestException(CACHED_CONTENT_ERROR))
                .thenReturn(generateContentResponse("pong"));

        // When
        ChatResponse response = chatModel().chat(chatRequest());

        // Then
        assertThat(response.aiMessage().text()).isEqualTo("pong");
        verify(geminiService, times(2)).generateContent(eq(MODEL), any());
        verify(cacheManager).evict(eq(CACHE_KEY), any(), any(), any(), eq(MODEL));
    }

    @Test
    void shouldNotRetry_whenGenerateContentFailsWithUnrelatedError() {
        // Given
        when(geminiService.generateContent(anyString(), any()))
                .thenThrow(new InvalidRequestException("Invalid JSON payload received."));

        // When-Then
        assertThatThrownBy(() -> chatModel().chat(chatRequest()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid JSON payload received.");
        verify(geminiService, times(1)).generateContent(eq(MODEL), any());
        verify(cacheManager, never()).evict(anyString(), any(), any(), any(), anyString());
    }

    @Test
    void shouldEvictAndRestartStream_whenStreamFailsWithCachedContentErrorBeforeAnyContent() {
        // Given
        StreamingChatResponseHandler handler = mock(StreamingChatResponseHandler.class);
        ChatResponse completeResponse = ChatResponse.builder().aiMessage(AiMessage.from("pong")).build();
        AtomicInteger streamInvocations = new AtomicInteger();
        doAnswer(invocation -> {
            StreamingChatResponseHandler invokedHandler = invocation.getArgument(4);
            if (streamInvocations.incrementAndGet() == 1) {
                invokedHandler.onError(new InvalidRequestException(CACHED_CONTENT_ERROR));
            } else {
                invokedHandler.onCompleteResponse(completeResponse);
            }
            return null;
        }).when(geminiService).generateContentStream(anyString(), any(), anyBoolean(), any(), any());

        // When
        streamingChatModel().chat(chatRequest(), handler);

        // Then
        verify(handler).onCompleteResponse(completeResponse);
        verify(handler, never()).onError(any());
        verify(geminiService, times(2)).generateContentStream(eq(MODEL), any(), anyBoolean(), any(), any());
        verify(cacheManager).evict(eq(CACHE_KEY), any(), any(), any(), eq(MODEL));
    }

    @Test
    void shouldPropagateStreamError_whenContentWasAlreadyDelivered() {
        // Given
        StreamingChatResponseHandler handler = mock(StreamingChatResponseHandler.class);
        InvalidRequestException error = new InvalidRequestException(CACHED_CONTENT_ERROR);
        doAnswer(invocation -> {
            StreamingChatResponseHandler invokedHandler = invocation.getArgument(4);
            invokedHandler.onPartialResponse("partial");
            invokedHandler.onError(error);
            return null;
        }).when(geminiService).generateContentStream(anyString(), any(), anyBoolean(), any(), any());

        // When
        streamingChatModel().chat(chatRequest(), handler);

        // Then
        verify(handler).onPartialResponse("partial");
        verify(handler).onError(error);
        verify(geminiService, times(1)).generateContentStream(eq(MODEL), any(), anyBoolean(), any(), any());
        verify(cacheManager, never()).evict(anyString(), any(), any(), any(), anyString());
    }

}
