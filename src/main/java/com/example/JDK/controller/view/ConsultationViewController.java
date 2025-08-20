// src/main/java/com/example/JDK/controller/ConsultationViewController.java
package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.
import com.example.JDK.dto.ConsultationReplyDto;
import com.example.JDK.dto.ConsultationRequestDto;
import com.example.JDK.entity.Consultation;
import com.example.JDK.service.ConsultationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequiredArgsConstructor
public class ConsultationViewController {

    private final ConsultationService consultationService;

    /** 프로필에서 상담 요청 (POST) */
    @PostMapping("/view/lawyers/{lawyerId}/consult")
    public String requestConsult(@PathVariable Long lawyerId,
                                 @Valid @ModelAttribute ConsultationRequestDto form,
                                 @AuthenticationPrincipal UserDetails auth) {
        if (auth == null) return "redirect:/view/login?continue=/view/lawyers/" + lawyerId;
        // form.lawyerId가 비어 있을 수 있으니 강제로 세팅
        form.setLawyerId(lawyerId);
        consultationService.create(form.getLawyerId(), auth.getUsername(), form.getTitle(), form.getContent());
        return "redirect:/view/mypage/consultations"; // 유저 마이페이지로
    }

    /** 변호사 마이페이지 - 상담 목록 */
    @GetMapping("/view/mypage/lawyer/consultations")
    public String lawyerList(@AuthenticationPrincipal UserDetails auth, Model model) {
        List<Consultation> list = consultationService.listForLawyer(auth.getUsername());
        model.addAttribute("consultations", list);
        return "consultations/lawyer-list";
    }

    /** 변호사 답변 */
    @PostMapping("/view/mypage/lawyer/consultations/{id}/reply")
    public String reply(@PathVariable Long id,
                        @Valid @ModelAttribute ConsultationReplyDto form,
                        @AuthenticationPrincipal UserDetails auth) {
        consultationService.reply(id, auth.getUsername(), form.getReply());
        return "redirect:/view/mypage/lawyer/consultations?open=" + id;
    }

    /** 유저 마이페이지 - 내가 요청한 상담 목록 */
    @GetMapping("/view/mypage/consultations")
    public String userList(@AuthenticationPrincipal UserDetails auth, Model model) {
        List<Consultation> list = consultationService.listForUser(auth.getUsername());
        model.addAttribute("consultations", list);
        return "consultations/user-list";
    }
}
