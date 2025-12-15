package dev.langchain4j.model.googleai;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.model.googleai.Json.toJsonWithoutIndent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

class FunctionMapper {

    static GeminiTool fromToolSepcsToGTool(List<ToolSpecification> specifications, boolean allowCodeExecution) {

        GeminiTool.GeminiToolBuilder tool = GeminiTool.builder();

        if (allowCodeExecution) {
            tool.codeExecution(new GeminiCodeExecution());
        }
        if (isNullOrEmpty(specifications)) {
            if (allowCodeExecution) {
                // if there's no tool specification, but there's Python code execution
                return tool.build();
            } else {
                // if there's neither tool specification nor Python code execution
                return null;
            }
        }

        List<GeminiFunctionDeclaration> functionDeclarations = specifications.stream()
            .map(specification -> {
                GeminiFunctionDeclaration.GeminiFunctionDeclarationBuilder fnBuilder =
                    GeminiFunctionDeclaration.builder()
                            .name(specification.name());

                    if (specification.description() != null) {
                        fnBuilder.description(specification.description());
                    }

                    if (specification.parameters() != null) {
                        JsonObjectSchema functionParams = specification.parameters();
                        Map<String, Object> parametersJsonSchema = new LinkedHashMap<>();
                        parametersJsonSchema.put("type", "object");

                        Map<String, Object> properties = new LinkedHashMap<>();
                        functionParams.properties().forEach((propertyName, propertySchema) -> {
                            properties.put(propertyName, JsonSchemaElementUtils.toMap(propertySchema));
                        });
                        parametersJsonSchema.put("properties", properties);

                        parametersJsonSchema.put("required", functionParams.required());
                        parametersJsonSchema.put("additionalProperties", false);
                        fnBuilder.parametersJsonSchema(parametersJsonSchema);
                    }

                return fnBuilder.build();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (!functionDeclarations.isEmpty()) {
            tool.functionDeclarations(functionDeclarations);
        }

        return tool.build();
    }

    static List<ToolExecutionRequest> toToolExecutionRequests(List<GeminiFunctionCall> functionCalls) {
        return functionCalls.stream()
                .map(functionCall -> ToolExecutionRequest.builder()
                        .name(functionCall.getName())
                        .arguments(toJsonWithoutIndent(functionCall.getArgs()))
                        .build())
                .collect(Collectors.toList());
    }
}
