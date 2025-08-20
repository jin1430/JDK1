// src/main/java/com/example/JDK/config/GlobalModelAttributeAdvice.java
package com.example.JDK.config;

import com.example.JDK.LawyerSpecialty;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.User;
import com.example.JDK.repository.LawyerRepository;
import com.example.JDK.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Slf4j
@ControllerAdvice
@Component
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final UserRepository userRepository;
    private final LawyerRepository lawyerRepository;


    @ModelAttribute
    public void injectCommonModel(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            model.addAttribute("userEmail", null);
            model.addAttribute("username", null);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isLawyer", false);
            model.addAttribute("isMember", false);
            return;
        }

        String email = auth.getName(); // UserDetails.getUsername() == email
        model.addAttribute("userEmail", email);

        // 이름 표기 (User.name)
        Optional<User> u = userRepository.findByEmail(email);
        model.addAttribute("username", u.map(User::getUsername).orElse(email));

        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        boolean isLawyer = hasRole(auth, "ROLE_LAWYER");
        boolean isMember = hasRole(auth, "ROLE_MEMBER");
        log.info(String.valueOf(isLawyer));

        if (isLawyer && u.isPresent()) {
            Lawyer lawyer = lawyerRepository.findByUser(u.get());  // ✅ User 기반으로 Lawyer 찾기
            if (lawyer != null) {
                model.addAttribute("lawyerId", lawyer.getId());
            }
        }
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLawyer", isLawyer);
        model.addAttribute("isMember", isMember);
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    @ControllerAdvice
    @Component
    public class GlobalModelAttributes {
        @ModelAttribute
        public void addCsrfToken(Model model, CsrfToken csrfToken) {
            if (csrfToken != null) {
                model.addAttribute("_csrf", csrfToken);
            }
        }
    }
    @ModelAttribute
    void defaults(Model model) {
        if (!model.containsAttribute("keyword")) model.addAttribute("keyword", "");
    }
    @ModelAttribute
    public void injectLightweightGlobals(Model model) {
        if (!model.containsAttribute("lawyerCategories")) {
            List<Map<String, Object>> tabs = new ArrayList<>();
            tabs.add(Map.of("key", "all", "label", "전체", "active", true));
            for (var s : LawyerSpecialty.values()) {
                tabs.add(Map.of(
                        "key", s.name(),                // data-category 값
                        "label", s.getKoreanName(),     // UI 표시 라벨
                        "active", false
                ));
            }
            model.addAttribute("lawyerCategories", tabs);
        }
    }
}
