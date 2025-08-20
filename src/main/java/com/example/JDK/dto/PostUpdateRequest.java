// src/main/java/com/example/JDK/dto/PostUpdateRequest.java
package com.example.JDK.dto;

import jakarta.validation.constraints.NotBlank;
public record PostUpdateRequest(
        @NotBlank String title,
        @NotBlank String content,
        Long categoryId // 선택값: null이면 카테고리 변경 안 함
) {}
