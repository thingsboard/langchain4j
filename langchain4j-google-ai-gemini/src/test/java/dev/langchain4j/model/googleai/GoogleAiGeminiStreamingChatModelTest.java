package dev.langchain4j.model.googleai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCharSequence;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GoogleAiGeminiStreamingChatModelTest {
    private static final ChatRequest DEFAULT_REQUEST =
            ChatRequest.builder().messages(new UserMessage("Hi")).build();

    @Test
    void should_fail_when_empty_messages_provided() {
        // when/then
        assertThatThrownBy(() -> ChatRequest.builder().messages().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("messages cannot be null or empty");
    }

    @Nested
    class GoogleAiGeminiStreamingChatModelBuilder {

        @Test
        void seedParameterInContentRequest() {
            GoogleAiGeminiStreamingChatModel chatModel = GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey("ApiKey")
                    .modelName("ModelName")
                    .seed(42)
                    .build();
            GeminiGenerateContentRequest result = chatModel.createGenerateContentRequest(DEFAULT_REQUEST);

            assertThatCharSequence(Json.toJson(result.generationConfig())).contains("\"seed\" : 42");
        }

        @Test
        void defaultSeedInContentRequest() {
            GoogleAiGeminiStreamingChatModel chatModel = GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey("ApiKey")
                    .modelName("ModelName")
                    .build();
            GeminiGenerateContentRequest result = chatModel.createGenerateContentRequest(DEFAULT_REQUEST);

            assertThatCharSequence(Json.toJson(result.generationConfig())).doesNotContain("\"seed\"");
        }

        @Test
        void cachedContentNameInContentRequest() {
            GoogleAiGeminiStreamingChatModel chatModel = GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey("ApiKey")
                    .modelName("ModelName")
                    .cachedContentName("cachedContents/abc123")
                    .build();
            GeminiGenerateContentRequest result = chatModel.createGenerateContentRequest(DEFAULT_REQUEST);

            assertThat(result.getCachedContent()).isEqualTo("cachedContents/abc123");
            assertThatCharSequence(Json.toJson(result)).contains("\"cachedContent\" : \"cachedContents/abc123\"");
        }

        @Test
        void cachedContentNameStripsSystemInstructionAndTools() {
            GoogleAiGeminiStreamingChatModel chatModel = GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey("ApiKey")
                    .modelName("ModelName")
                    .cachedContentName("cachedContents/abc123")
                    .toolConfig(GeminiMode.VALIDATED)
                    .build();
            ChatRequest request = ChatRequest.builder()
                    .messages(new SystemMessage("You are a helpful assistant"), new UserMessage("Hi"))
                    .toolSpecifications(ToolSpecification.builder().name("myTool").build())
                    .build();

            GeminiGenerateContentRequest result = chatModel.createGenerateContentRequest(request);

            // When a cached content is attached, the system instruction / tools / tool config already live in
            // the cache and must not be resent inline.
            assertThat(result.getCachedContent()).isEqualTo("cachedContents/abc123");
            assertThat(result.systemInstruction()).isNull();
            assertThat(result.tools()).isNull();
            assertThat(result.toolConfig()).isNull();
        }

        @Test
        void defaultCachedContentNameInContentRequest() {
            GoogleAiGeminiStreamingChatModel chatModel = GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey("ApiKey")
                    .modelName("ModelName")
                    .build();
            GeminiGenerateContentRequest result = chatModel.createGenerateContentRequest(DEFAULT_REQUEST);

            assertThat(result.getCachedContent()).isNull();
            assertThatCharSequence(Json.toJson(result)).doesNotContain("\"cachedContent\"");
        }
    }
}
