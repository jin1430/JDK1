package com.example.JDK.controller.api;


// Refactor: categorised as api controller; moved for structure-only readability.
import com.example.JDK.service.LawyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
public class LawyerApprovalController {

    private final LawyerService lawyerService;

    // 승인
    @PutMapping("/{id}/approve")
    public void approveLawyer(@PathVariable Long id) {
        lawyerService.approveLawyer(id);
    }

    // 거절
    @PutMapping("/{id}/reject")
    public void rejectLawyer(@PathVariable Long id) {
        lawyerService.rejectLawyer(id);
    }

    // 대기중인 변호사 리스트 확인 (선택)
    @GetMapping("/pending")
    public Object getPendingLawyers() {
        return lawyerService.getPendingLawyers();
    }
}
