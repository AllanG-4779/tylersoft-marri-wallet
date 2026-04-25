package net.tylersoft.common.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,
        String message,
        T data,
        String error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("00", null, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("00", message, data, null);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>("01", error, null, error);
    }
}
