// src/main/java/com/example/JDK/api/ApiResponse.java
package com.example.JDK.controller.api;


// Refactor: categorised as api controller; moved for structure-only readability.
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final String code;   // e.g., "OK", "VALIDATION_ERROR"
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
