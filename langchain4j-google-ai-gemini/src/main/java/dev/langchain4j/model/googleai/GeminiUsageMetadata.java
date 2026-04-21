package dev.langchain4j.model.googleai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record GeminiUsageMetadata(
        @JsonProperty("promptTokenCount") Integer promptTokenCount,
        @JsonProperty("candidatesTokenCount") Integer candidatesTokenCount,
        @JsonProperty("totalTokenCount") Integer totalTokenCount) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;

        private Builder() {}

        Builder promptTokenCount(Integer promptTokenCount) {
            this.promptTokenCount = promptTokenCount;
            return this;
        }

        Builder candidatesTokenCount(Integer candidatesTokenCount) {
            this.candidatesTokenCount = candidatesTokenCount;
            return this;
        }

        Builder totalTokenCount(Integer totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
            return this;
        }

        GeminiUsageMetadata build() {
            return new GeminiUsageMetadata(promptTokenCount, candidatesTokenCount, totalTokenCount);
        }

    }

}
