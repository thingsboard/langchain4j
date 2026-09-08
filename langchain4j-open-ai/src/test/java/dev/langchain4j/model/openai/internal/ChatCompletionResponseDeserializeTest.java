package dev.langchain4j.model.openai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.openai.internal.chat.AssistantMessage;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionChoice;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionResponse;
import dev.langchain4j.model.openai.internal.chat.ToolCall;
import org.junit.jupiter.api.Test;

class ChatCompletionResponseDeserializeTest {

    @Test
    void should_deserialize_reasoning_content_when_message_uses_reasoning_content_field() {

        // given
        String json = """
                {
                    "id": "chatcmpl-123",
                    "object": "chat.completion",
                    "created": 1742268380,
                    "model": "deepseek-ai/DeepSeek-V3",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "391",
                                "reasoning_content": "17*20 = 340, 17*3 = 51, sum = 391."
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 83,
                        "completion_tokens": 2,
                        "total_tokens": 85
                    }
                }
                """;

        // when
        ChatCompletionResponse response = Json.fromJson(json, ChatCompletionResponse.class);

        // then
        ChatCompletionChoice chatCompletionChoice = response.choices().get(0);
        assertThat(chatCompletionChoice.message().reasoningContent()).isEqualTo("17*20 = 340, 17*3 = 51, sum = 391.");
        assertThat(chatCompletionChoice.message().content()).isEqualTo("391");
    }

    @Test
    void should_deserialize_reasoning_content_when_message_uses_reasoning_field() {

        // given
        String json = """
                {
                    "id": "chatcmpl-123",
                    "object": "chat.completion",
                    "created": 1742268380,
                    "model": "openai/gpt-oss-120b",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "391",
                                "reasoning": "17*20 = 340, 17*3 = 51, sum = 391."
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 83,
                        "completion_tokens": 2,
                        "total_tokens": 85
                    }
                }
                """;

        // when
        ChatCompletionResponse response = Json.fromJson(json, ChatCompletionResponse.class);

        // then
        ChatCompletionChoice chatCompletionChoice = response.choices().get(0);
        assertThat(chatCompletionChoice.message().reasoningContent()).isEqualTo("17*20 = 340, 17*3 = 51, sum = 391.");
        assertThat(chatCompletionChoice.message().content()).isEqualTo("391");
    }

    @Test
    void should_deserialize_reasoning_content_when_delta_uses_reasoning_field() {

        // given
        String json = """
                {
                    "id": "0195a749b17b5668b9753240788da6f8",
                    "object": "chat.completion.chunk",
                    "created": 1742268380,
                    "model": "openai/gpt-oss-120b",
                    "choices": [
                        {
                            "index": 0,
                            "delta": {
                                "role": "assistant",
                                "content": null,
                                "reasoning": "We need to compute 17*23."
                            },
                            "finish_reason": null
                        }
                    ],
                    "system_fingerprint": "",
                    "usage": {
                        "prompt_tokens": 83,
                        "completion_tokens": 2,
                        "total_tokens": 85
                    }
                }
                """;

        // when
        ChatCompletionResponse response = Json.fromJson(json, ChatCompletionResponse.class);

        // then
        ChatCompletionChoice chatCompletionChoice = response.choices().get(0);
        assertThat(chatCompletionChoice.delta().reasoningContent()).isEqualTo("We need to compute 17*23.");
    }

    @Test
    void should_serialize_reasoning_content_with_snake_case_name() {

        // given
        AssistantMessage message = AssistantMessage.builder()
                .content("391")
                .reasoningContent("thinking...")
                .build();

        // when
        String json = Json.toJson(message);

        // then
        assertThat(json).isEqualTo("""
                        {
                          "role" : "assistant",
                          "content" : "391",
                          "reasoning_content" : "thinking..."
                        }""");
    }

    @Test
    void should_deserialize_chat_response_without_tool_type() {

        // given
        String json = """
                {
                    "id": "0195a749b17b5668b9753240788da6f8",
                    "object": "chat.completion.chunk",
                    "created": 1742268380,
                    "model": "deepseek-ai/DeepSeek-V3",
                    "choices": [
                        {
                            "index": 0,
                            "delta": {
                                "content": null,
                                "reasoning_content": null,
                                "tool_calls": [
                                    {
                                        "index": 0,
                                        "id": "",
                                        "type": "",
                                        "function": {
                                            "arguments": "{\\""
                                        }
                                    }
                                ]
                            },
                            "finish_reason": null
                        }
                    ],
                    "system_fingerprint": "",
                    "usage": {
                        "prompt_tokens": 83,
                        "completion_tokens": 2,
                        "total_tokens": 85
                    }
                }
                """;

        // when
        ChatCompletionResponse response = Json.fromJson(json, ChatCompletionResponse.class);

        // then
        ChatCompletionChoice chatCompletionChoice = response.choices().get(0);
        ToolCall toolCall = chatCompletionChoice.delta().toolCalls().get(0);
        assertThat(toolCall.function().arguments()).isEqualTo("{\"");
    }
}
