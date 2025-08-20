// src/main/java/com/example/JDK/controller/admin/AdminLawyerController.java
package com.example.JDK.controller.admin;


// Refactor: categorised as admin controller; moved for structure-only readability.
import com.example.JDK.ApprovalStatus;
import com.example.JDK.dto.LawyerRowVM;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.service.LawyerAdminService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/lawyers")
@RequiredArgsConstructor
public class AdminLawyerController {

    private final LawyerAdminService lawyerAdminService;

    @GetMapping
    public String list(
            @RequestParam(required = false, defaultValue = "PENDING") String status, // ★ String으로 받아 ALL 지원
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        // ★ ALL이면 전체 조회, 아니면 해당 상태 조회
        Page<Lawyer> result;
        if ("ALL".equalsIgnoreCase(status)) {
            result = lawyerAdminService.listAll(page, size);
        } else {
            ApprovalStatus st = ApprovalStatus.valueOf(status.toUpperCase());
            result = lawyerAdminService.listByStatus(st, page, size);
        }

        int currentPage = result.getNumber();
        int totalPages  = Math.max(result.getTotalPages(), 1);
        int prevPage    = Math.max(currentPage - 1, 0);
        int nextPage    = result.hasNext() ? currentPage + 1 : currentPage;

        List<LawyerRowVM> rows = result.getContent().stream()
                .map(this::toRow)
                .collect(Collectors.toList());

        // ★ Mustache에서 쓰는 값들 주입
        model.addAttribute("rows", rows);
        model.addAttribute("page", result);         // ★ 페이지 객체 자체도 추가 ({{#page.hasNext}} 등)
        model.addAttribute("status", status);       // 현재 필터 문자열 그대로
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("displayPage", currentPage + 1);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("prevPage", prevPage);
        model.addAttribute("nextPage", nextPage);

        // 상태 플래그 (ALL 포함)
        model.addAttribute("statusIsAll",      "ALL".equalsIgnoreCase(status));
        model.addAttribute("statusIsPending",  "PENDING".equalsIgnoreCase(status));
        model.addAttribute("statusIsApproved", "APPROVED".equalsIgnoreCase(status));
        model.addAttribute("statusIsRejected", "REJECTED".equalsIgnoreCase(status));

        // 디버깅용: rows 개수 찍어보기 (원하면 log로)
        // System.out.println("[AdminLawyer] rows.size=" + rows.size() + ", status=" + status);

        return "admin/lawyers";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(defaultValue = "PENDING") ApprovalStatus status,
                          @RequestParam(defaultValue = "0") int page,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        lawyerAdminService.approve(id);
        ra.addFlashAttribute("msg", "승인되었습니다.");
        return "redirect:/admin/lawyers?status=" + status + "&page=" + page;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(defaultValue = "PENDING") ApprovalStatus status,
                         @RequestParam(defaultValue = "0") int page,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        lawyerAdminService.reject(id);
        ra.addFlashAttribute("msg", "반려되었습니다.");
        return "redirect:/admin/lawyers?status=" + status + "&page=" + page;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam ApprovalStatus status,
                               @RequestParam(defaultValue = "PENDING", name = "filter") String filter,
                               @RequestParam(defaultValue = "0") int page,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        lawyerAdminService.changeStatus(id, status);
        ra.addFlashAttribute("msg", "상태가 " + status.name() + "(으)로 변경되었습니다.");
        return "redirect:/admin/lawyers?status=" + filter + "&page=" + page;
    }

    private LawyerRowVM toRow(Lawyer l) {
        var user = l.getUser();
        var status = l.getApprovalStatus();
        return new LawyerRowVM(
                l.getId(),
                user != null ? user.getEmail() : null,
                user != null ? user.getUsername() : null,
                l.getCertificateNumber(),
                status.name(),
                status == ApprovalStatus.PENDING,
                status == ApprovalStatus.APPROVED,
                status == ApprovalStatus.REJECTED
        );
    }
}
