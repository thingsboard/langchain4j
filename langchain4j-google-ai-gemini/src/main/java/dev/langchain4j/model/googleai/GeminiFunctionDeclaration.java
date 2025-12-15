package dev.langchain4j.model.googleai;

import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class GeminiFunctionDeclaration {
    private String name;
    private String description;
    private Map<String, Object> parametersJsonSchema;

    @JsonCreator
    GeminiFunctionDeclaration(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("parametersJsonSchema") Map<String, Object> parametersJsonSchema
    ) {
        this.name = name;
        this.description = description;
        this.parametersJsonSchema = parametersJsonSchema;
    }

    public static GeminiFunctionDeclarationBuilder builder() {
        return new GeminiFunctionDeclarationBuilder();
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Map<String, Object> getParametersJsonSchema() {
        return this.parametersJsonSchema;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParametersJsonSchema(Map<String, Object> parametersJsonSchema) {
        this.parametersJsonSchema = parametersJsonSchema;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeminiFunctionDeclaration that = (GeminiFunctionDeclaration) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(parametersJsonSchema, that.parametersJsonSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, parametersJsonSchema);
    }

    @Override
    public String toString() {
        return "GeminiFunctionDeclaration{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parametersJsonSchema=" + parametersJsonSchema +
                '}';
    }

    public static class GeminiFunctionDeclarationBuilder {
        private String name;
        private String description;
        private Map<String, Object> parametersJsonSchema;

        GeminiFunctionDeclarationBuilder() {
        }

        public GeminiFunctionDeclarationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GeminiFunctionDeclarationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public GeminiFunctionDeclarationBuilder parametersJsonSchema(Map<String, Object> parametersJsonSchema) {
            this.parametersJsonSchema = parametersJsonSchema;
            return this;
        }

        public GeminiFunctionDeclaration build() {
            return new GeminiFunctionDeclaration(this.name, this.description, this.parametersJsonSchema);
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GeminiFunctionDeclarationBuilder that = (GeminiFunctionDeclarationBuilder) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(parametersJsonSchema, that.parametersJsonSchema);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, description, parametersJsonSchema);
        }

        @Override
        public String toString() {
            return "GeminiFunctionDeclarationBuilder{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", parametersJsonSchema=" + parametersJsonSchema +
                    '}';
        }

    }
}
