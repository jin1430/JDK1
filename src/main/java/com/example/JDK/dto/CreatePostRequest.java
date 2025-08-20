// src/main/java/com/example/JDK/api/dto/CreatePostRequest.java
package com.example.JDK.dto;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class CreatePostRequest {
    private String title;
    private String content;
    private Long categoryId; // 셀렉트에서 넘어온 카테고리 id
}
