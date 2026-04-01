package com.sso.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    /** Shared mapper only used for JsonNode → plain-Java-object conversion. */
    private static final ObjectMapper NODE_MAPPER = new ObjectMapper();

    /**
     * When {@code data} is a {@link JsonNode} Jackson would otherwise serialize it
     * as a POJO (calling isArray(), isBoolean() etc. as bean properties).
     * Convert it to a plain Map/List/primitive first so Spring's serializer
     * writes the actual JSON content.
     */
    @SuppressWarnings("unchecked")
    public static <T> ApiResponse<T> ok(T data) {
        T payload = data;
        if (data instanceof JsonNode jsonNode) {
            try {
                payload = (T) NODE_MAPPER.treeToValue(jsonNode, Object.class);
            } catch (Exception ignored) { /* fall through with original value */ }
        }
        return ApiResponse.<T>builder().success(true).message("OK").data(payload).build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder().success(true).message(message).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().success(false).message(message).build();
    }
}
