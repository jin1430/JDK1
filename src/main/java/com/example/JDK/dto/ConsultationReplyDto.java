// src/main/java/com/example/JDK/dto/consult/ConsultationReplyDto.java
package com.example.JDK.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class ConsultationReplyDto {
    @NotBlank
    private String reply;
}
