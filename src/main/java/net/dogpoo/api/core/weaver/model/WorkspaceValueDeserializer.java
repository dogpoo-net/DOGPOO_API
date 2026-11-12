package net.dogpoo.api.core.weaver.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Bridge Boot 4's HTTP mapper to the existing workspace persistence model. */
public final class WorkspaceValueDeserializer extends ValueDeserializer<JsonNode> {
    private static final ObjectMapper STORAGE_JSON = new ObjectMapper();

    @Override
    public JsonNode getNullValue(DeserializationContext context) {
        return com.fasterxml.jackson.databind.node.NullNode.getInstance();
    }

    @Override
    public JsonNode deserialize(JsonParser parser, DeserializationContext context) {
        var value = context.readTree(parser);
        try {
            return STORAGE_JSON.readTree(value.toString());
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid workspace JSON value", exception);
        }
    }
}
