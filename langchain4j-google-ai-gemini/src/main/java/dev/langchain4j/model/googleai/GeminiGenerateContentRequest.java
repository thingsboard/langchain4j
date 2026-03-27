package dev.langchain4j.model.googleai;

import java.util.List;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record GeminiGenerateContentRequest(
        String model,
        List<GeminiContent> contents,
        GeminiTool tools,
        GeminiToolConfig toolConfig,
        List<GeminiSafetySetting> safetySettings,
        GeminiContent systemInstruction,
        GeminiGenerationConfig generationConfig,
        Supplier<String> cachedContent) {

    @JsonGetter("cachedContent")
    public String getCachedContent() {
        return cachedContent != null ? cachedContent.get() : null;
    }

    static GeminiGenerateContentRequestBuilder builder() {
        return new GeminiGenerateContentRequestBuilder();
    }

    static class GeminiGenerateContentRequestBuilder {
        private String model;
        private List<GeminiContent> contents;
        private GeminiTool tools;
        private GeminiToolConfig toolConfig;
        private List<GeminiSafetySetting> safetySettings;
        private GeminiContent systemInstruction;
        private GeminiGenerationConfig generationConfig;
        private Supplier<String> cachedContent;

        GeminiGenerateContentRequestBuilder() {
        }

        GeminiGenerateContentRequestBuilder model(String model) {
            this.model = model;
            return this;
        }

        GeminiGenerateContentRequestBuilder contents(List<GeminiContent> contents) {
            this.contents = contents;
            return this;
        }

        GeminiGenerateContentRequestBuilder tools(GeminiTool tools) {
            this.tools = tools;
            return this;
        }

        GeminiGenerateContentRequestBuilder toolConfig(GeminiToolConfig toolConfig) {
            this.toolConfig = toolConfig;
            return this;
        }

        GeminiGenerateContentRequestBuilder safetySettings(List<GeminiSafetySetting> safetySettings) {
            this.safetySettings = safetySettings;
            return this;
        }

        GeminiGenerateContentRequestBuilder systemInstruction(GeminiContent systemInstruction) {
            this.systemInstruction = systemInstruction;
            return this;
        }

        GeminiGenerateContentRequestBuilder generationConfig(GeminiGenerationConfig generationConfig) {
            this.generationConfig = generationConfig;
            return this;
        }

        GeminiGenerateContentRequestBuilder cachedContent(Supplier<String> cachedContent) {
            this.cachedContent = cachedContent;
            return this;
        }

        public GeminiGenerateContentRequest build() {
            return new GeminiGenerateContentRequest(
                    this.model,
                    this.contents,
                    this.tools,
                    this.toolConfig,
                    this.safetySettings,
                    this.systemInstruction,
                    this.generationConfig,
                    this.cachedContent);
        }
    }

}
