// src/main/java/com/example/JDK/config/GlobalExceptionHandler.java
package com.example.JDK.config;

import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
//        String msg = ex.getBindingResult().getAllErrors().stream()
//                .findFirst().map(e -> e.getDefaultMessage()).orElse("Validation error");
//        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", msg));
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
//        return ResponseEntity.badRequest().body(ApiResponse.error("CONSTRAINT_ERROR", ex.getMessage()));
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ApiResponse<Void>> handleIllegal(IllegalArgumentException ex) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
//    }
}
