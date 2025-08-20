package com.example.JDK.controller;

import com.example.JDK.service.DummyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class SeedController {

    private final DummyDataService dummy;

    // GET 으로 들어가면 관리자 확인 후 초기화 실행
    @PostMapping("/seed")
    public String resetAndSeed(RedirectAttributes ra) {
        dummy.resetAndSeed();
        ra.addFlashAttribute("message", "DB 시드를 초기화했습니다.");
        return "redirect:/admin"; // 관리자 메인 페이지로 리다이렉트
    }
}