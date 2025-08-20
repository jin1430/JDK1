// src/main/java/com/example/JDK/controller/admin/vm/LawyerRowVM.java
package com.example.JDK.dto;

public record LawyerRowVM(
        Long id,
        String email,
        String username,
        String certificateNumber,
        String approvalStatus,   // "PENDING" | "APPROVED" | "REJECTED"
        boolean isPending,
        boolean isApproved,
        boolean isRejected
) {}
