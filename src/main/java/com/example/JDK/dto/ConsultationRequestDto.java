// src/main/java/com/example/JDK/dto/consult/ConsultationRequestDto.java
package com.example.JDK.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class ConsultationRequestDto {
    @NotNull
    private Long lawyerId;
    @NotBlank
    private String title;
    @NotBlank
    private String content;
}
