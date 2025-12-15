package dev.langchain4j.model.googleai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.output.structured.Description;
import org.junit.jupiter.api.Test;

class FunctionMapperTest {

    enum Projection {
        WGS84,
        NAD83,
        PZ90,
        GCJ02,
        BD09
    }

    static class Coordinates {
        @Description("latitude")
        double latitude;

        @Description("latitude")
        double longitude;

        @Description("Geographic projection system used")
        Projection projection;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.projection = Projection.WGS84;
        }
    }

    static class IssTool {
        @Tool("Get the distance between the user and the ISS.")
        int distanceBetween(
                @P("user coordinates") Coordinates userCoordinates, @P("ISS coordinates") Coordinates issCoordinates) {
            return 3456;
        }
    }

    @Test
    void should_convert_nested_structures() throws JsonProcessingException {
        // when
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(IssTool.class);

        // then
        assertThat(toolSpecifications).hasSize(1);
        ToolSpecification toolSpecification = toolSpecifications.get(0);
        assertThat(toolSpecification.name()).isEqualTo("distanceBetween");
        assertThat(toolSpecification.description()).isEqualTo("Get the distance between the user and the ISS.");

        // when
        GeminiTool geminiTool = FunctionMapper.fromToolSepcsToGTool(toolSpecifications, false);

        // then
        List<GeminiFunctionDeclaration> allFunctions = geminiTool.getFunctionDeclarations();
        assertThat(allFunctions).hasSize(1);

        GeminiFunctionDeclaration function = allFunctions.get(0);
        assertThat(function.getName()).isEqualTo("distanceBetween");
        assertThat(function.getDescription()).isEqualTo("Get the distance between the user and the ISS.");

        Map<String, Object> params = function.getParametersJsonSchema();
        assertThat(params).isNotNull();

        JsonNode expectedParamsJson = Json.OBJECT_MAPPER.readTree("""
                {
                  "type": "object",
                  "properties": {
                    "userCoordinates": {
                      "type": "object",
                      "description": "user coordinates",
                      "properties": {
                        "latitude": { "type": "number", "description": "latitude" },
                        "longitude": { "type": "number", "description": "latitude" },
                        "projection": {
                          "type": "string",
                          "description": "Geographic projection system used",
                          "enum": ["WGS84", "NAD83", "PZ90", "GCJ02", "BD09"]
                        }
                      },
                      "required": ["latitude", "longitude", "projection"]
                    },
                    "issCoordinates": {
                      "type": "object",
                      "description": "ISS coordinates",
                      "properties": {
                        "latitude": { "type": "number", "description": "latitude" },
                        "longitude": { "type": "number", "description": "latitude" },
                        "projection": {
                          "type": "string",
                          "description": "Geographic projection system used",
                          "enum": ["WGS84", "NAD83", "PZ90", "GCJ02", "BD09"]
                        }
                      },
                      "required": ["latitude", "longitude", "projection"]
                    }
                  },
                  "required": ["userCoordinates", "issCoordinates"],
                  "additionalProperties": false
                }
                """);
        JsonNode actualParamsJson = Json.OBJECT_MAPPER.valueToTree(params);

        assertThat(actualParamsJson).isEqualTo(expectedParamsJson);
    }

    static class Address {
        private final String street;
        private final String zipCode;
        private final String city;

        public Address(String street, String zipCode, String city) {
            this.street = street;
            this.zipCode = zipCode;
            this.city = city;
        }
    }

    static class Customer {
        private final String firstname;
        private final String lastname;

        private final Address shippingAddress;

        public Customer(String firstname, String lastname, Address shippingAddress) {
            this.firstname = firstname;
            this.lastname = lastname;
            this.shippingAddress = shippingAddress;
        }
    }

    static class Product {
        private final String name;
        private final String description;
        private final double price;

        public Product(String name, String description, double price) {
            this.name = name;
            this.description = description;
            this.price = price;
        }
    }

    static class LineItem {
        private final Product product;
        private final int quantity;

        public LineItem(int quantity, Product product) {
            this.product = product;
            this.quantity = quantity;
        }
    }

    static class Order {
        private final Double totalAmount;
        private final List<LineItem> lineItems;
        private final Customer customer;

        public Order(Double totalAmount, List<LineItem> lineItems, Customer customer) {
            this.totalAmount = totalAmount;
            this.lineItems = lineItems;
            this.customer = customer;
        }
    }

    static class OrderSystem {
        @Tool("Make an order")
        boolean makeOrder(@P(value = "The order to make") Order order) {
            return true;
        }
    }

    @Test
    void complexNestedGraph() throws JsonProcessingException {
        // given
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(OrderSystem.class);

        // when
        GeminiTool geminiTool = FunctionMapper.fromToolSepcsToGTool(toolSpecifications, false);

        // then
        List<GeminiFunctionDeclaration> allFunctions = geminiTool.getFunctionDeclarations();
        assertThat(allFunctions).hasSize(1);

        GeminiFunctionDeclaration function = allFunctions.get(0);
        assertThat(function.getName()).isEqualTo("makeOrder");
        assertThat(function.getDescription()).isEqualTo("Make an order");

        Map<String, Object> params = function.getParametersJsonSchema();
        assertThat(params).isNotNull();

        JsonNode expectedParamsJson = Json.OBJECT_MAPPER.readTree("""
                {
                  "type": "object",
                  "properties": {
                    "order": {
                      "type": "object",
                      "description": "The order to make",
                      "properties": {
                        "totalAmount": { "type": "number" },
                        "lineItems": {
                          "type": "array",
                          "items": {
                            "type": "object",
                            "properties": {
                              "product": {
                                "type": "object",
                                "properties": {
                                  "name": { "type": "string" },
                                  "description": { "type": "string" },
                                  "price": { "type": "number" }
                                },
                                "required": ["name", "description", "price"]
                              },
                              "quantity": { "type": "integer" }
                            },
                            "required": ["product", "quantity"]
                          }
                        },
                        "customer": {
                          "type": "object",
                          "properties": {
                            "firstname": { "type": "string" },
                            "lastname": { "type": "string" },
                            "shippingAddress": {
                              "type": "object",
                              "properties": {
                                "street": { "type": "string" },
                                "zipCode": { "type": "string" },
                                "city": { "type": "string" }
                              },
                              "required": ["street", "zipCode", "city"]
                            }
                          },
                          "required": ["firstname", "lastname", "shippingAddress"]
                        }
                      },
                      "required": ["totalAmount", "lineItems", "customer"]
                    }
                  },
                  "required": ["order"],
                  "additionalProperties": false
                }
                """);
        JsonNode actualParamsJson = Json.OBJECT_MAPPER.valueToTree(params);

        assertThat(actualParamsJson).isEqualTo(expectedParamsJson);
    }

    @Test
    void array() throws JsonProcessingException {
        // given
        ToolSpecification spec = ToolSpecification.builder()
                .name("toolName")
                .description("tool description")
                .parameters(JsonObjectSchema.builder()
                        .addProperty("arrayParameter", JsonArraySchema.builder()
                                .items(new JsonStringSchema())
                                .description("an array")
                                .build())
                        .required("arrayParameter")
                        .build())
                .build();

        // when
        GeminiTool geminiTool = FunctionMapper.fromToolSepcsToGTool(List.of(spec), false);

        // then
        List<GeminiFunctionDeclaration> allFunctions = geminiTool.getFunctionDeclarations();
        assertThat(allFunctions).hasSize(1);

        GeminiFunctionDeclaration function = allFunctions.get(0);
        assertThat(function.getName()).isEqualTo("toolName");
        assertThat(function.getDescription()).isEqualTo("tool description");

        Map<String, Object> params = function.getParametersJsonSchema();
        assertThat(params).isNotNull();
        JsonNode expectedParamsJson = Json.OBJECT_MAPPER.readTree("""
                {
                  "type": "object",
                  "properties": {
                    "arrayParameter": {
                      "type": "array",
                      "description": "an array",
                      "items": { "type": "string" }
                    }
                  },
                  "required": ["arrayParameter"],
                  "additionalProperties": false
                }
                """);
        JsonNode actualParamsJson = Json.OBJECT_MAPPER.valueToTree(params);

        assertThat(actualParamsJson).isEqualTo(expectedParamsJson);
    }

}
