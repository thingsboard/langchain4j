package dev.langchain4j.model.openai;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OpenAiChatModelTimeoutTest {

    @Test
    void should_keep_http_client_builder_timeouts_when_model_timeout_is_not_set_for_chat_model() {

        // given
        RecordingHttpClientBuilder httpClientBuilder = new RecordingHttpClientBuilder();
        httpClientBuilder.connectTimeout(ofSeconds(5));
        httpClientBuilder.readTimeout(ofSeconds(180));

        // when
        OpenAiChatModel.builder()
                .apiKey("key")
                .modelName("m")
                .httpClientBuilder(httpClientBuilder)
                .build();

        // then
        assertThat(httpClientBuilder.connectTimeout()).isEqualTo(ofSeconds(5));
        assertThat(httpClientBuilder.readTimeout()).isEqualTo(ofSeconds(180));
    }

    @Test
    void should_apply_defaults_when_neither_model_nor_http_client_builder_set_timeouts_for_chat_model() {

        // given
        RecordingHttpClientBuilder httpClientBuilder = new RecordingHttpClientBuilder();

        // when
        OpenAiChatModel.builder()
                .apiKey("key")
                .modelName("m")
                .httpClientBuilder(httpClientBuilder)
                .build();

        // then
        assertThat(httpClientBuilder.connectTimeout()).isEqualTo(ofSeconds(15));
        assertThat(httpClientBuilder.readTimeout()).isEqualTo(ofSeconds(60));
    }

    @Test
    void should_prefer_model_timeout_when_both_are_set_for_chat_model() {

        // given
        RecordingHttpClientBuilder httpClientBuilder = new RecordingHttpClientBuilder();
        httpClientBuilder.readTimeout(ofSeconds(180));

        // when
        OpenAiChatModel.builder()
                .apiKey("key")
                .modelName("m")
                .httpClientBuilder(httpClientBuilder)
                .timeout(ofSeconds(7))
                .build();

        // then
        assertThat(httpClientBuilder.connectTimeout()).isEqualTo(ofSeconds(7));
        assertThat(httpClientBuilder.readTimeout()).isEqualTo(ofSeconds(7));
    }

    @Test
    void should_keep_http_client_builder_timeouts_when_model_timeout_is_not_set_for_streaming_chat_model() {

        // given
        RecordingHttpClientBuilder httpClientBuilder = new RecordingHttpClientBuilder();
        httpClientBuilder.connectTimeout(ofSeconds(5));
        httpClientBuilder.readTimeout(ofSeconds(180));

        // when
        OpenAiStreamingChatModel.builder()
                .apiKey("key")
                .modelName("m")
                .httpClientBuilder(httpClientBuilder)
                .build();

        // then
        assertThat(httpClientBuilder.connectTimeout()).isEqualTo(ofSeconds(5));
        assertThat(httpClientBuilder.readTimeout()).isEqualTo(ofSeconds(180));
    }

    @Test
    void should_apply_defaults_when_neither_model_nor_http_client_builder_set_timeouts_for_streaming_chat_model() {

        // given
        RecordingHttpClientBuilder httpClientBuilder = new RecordingHttpClientBuilder();

        // when
        OpenAiStreamingChatModel.builder()
                .apiKey("key")
                .modelName("m")
                .httpClientBuilder(httpClientBuilder)
                .build();

        // then
        assertThat(httpClientBuilder.connectTimeout()).isEqualTo(ofSeconds(15));
        assertThat(httpClientBuilder.readTimeout()).isEqualTo(ofSeconds(60));
    }

    @Test
    void should_prefer_model_timeout_when_both_are_set_for_streaming_chat_model() {

        // given
        RecordingHttpClientBuilder httpClientBuilder = new RecordingHttpClientBuilder();
        httpClientBuilder.readTimeout(ofSeconds(180));

        // when
        OpenAiStreamingChatModel.builder()
                .apiKey("key")
                .modelName("m")
                .httpClientBuilder(httpClientBuilder)
                .timeout(ofSeconds(7))
                .build();

        // then
        assertThat(httpClientBuilder.connectTimeout()).isEqualTo(ofSeconds(7));
        assertThat(httpClientBuilder.readTimeout()).isEqualTo(ofSeconds(7));
    }

    /**
     * Records the connect/read timeouts set on it and hands out a mock {@link HttpClient} from {@link #build()}.
     * Unlike {@code dev.langchain4j.http.client.MockHttpClientBuilder}, the getters here actually return the
     * values set via the setters, which is required to observe the timeout fallback chain in these tests.
     */
    static class RecordingHttpClientBuilder implements HttpClientBuilder {

        private Duration connectTimeout;
        private Duration readTimeout;

        @Override
        public Duration connectTimeout() {
            return connectTimeout;
        }

        @Override
        public HttpClientBuilder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        @Override
        public Duration readTimeout() {
            return readTimeout;
        }

        @Override
        public HttpClientBuilder readTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        @Override
        public HttpClient build() {
            return mock(HttpClient.class);
        }
    }
}
