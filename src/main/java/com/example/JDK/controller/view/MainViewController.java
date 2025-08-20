package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.
import com.example.JDK.Role; // ← MEMBER / LAWYER enum;
import com.example.JDK.service.MainViewService;
import com.example.JDK.service.UserService; // ← 가입 로직을 담당할 서비스 (이름은 프로젝트에 맞게);
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequiredArgsConstructor
@RequestMapping("/view")
public class MainViewController {

    private final MainViewService mainViewService;
    private final UserService userService; // ← 프로젝트에 있는 사용자/인증 서비스 주입

    @GetMapping
    public String home(Model model) {
        model.addAttribute("latestPosts", mainViewService.latestPosts(6));
        model.addAttribute("sections", mainViewService.categorySections(4));
        model.addAttribute("postCategories", mainViewService.postCategoryTabs());
        model.addAttribute("postCards", mainViewService.postCards(12));
        model.addAttribute("lawyers", mainViewService.lawyerCards(6));
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // ✅ 회원가입 폼
    @GetMapping("/signup")
    public String signupForm() {
        return "signup";  // 다니엘 오빠가 준 머스태치 파일 이름과 동일해야 합니다.
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String username,
            @RequestParam String address,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthday,
            @RequestParam Role role, // com.example.JDK.Role
            @RequestParam(required = false) String certificateNumber,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            if (role == Role.LAWYER) {
                userService.registerLawyer(email, password, username, address, birthday, certificateNumber);
            } else {
                userService.registerMember(email, password, username, address, birthday);
            }
            ra.addFlashAttribute("isSignup", true);
            ra.addFlashAttribute("msg", "회원가입이 완료되었습니다. 로그인해 주세요.");
            return "redirect:/view/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        } catch (Exception e) {
            model.addAttribute("error", "회원가입 처리 중 오류가 발생했습니다.");
            return "signup";
        }
    }
}
