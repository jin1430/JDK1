// src/main/java/com/example/JDK/controller/MyPageController.java
package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.
import com.example.JDK.entity.Comment;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.Post;
import com.example.JDK.entity.User;
import com.example.JDK.repository.CommentRepository;
import com.example.JDK.repository.LawyerRepository;
import com.example.JDK.repository.PostRepository;
import com.example.JDK.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
@RequestMapping("/view/mypage")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MEMBER','LAWYER')") // 클래스 전체 보호
public class MyPageController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LawyerRepository lawyerRepository;

    // 마이페이지 메인
    @GetMapping
    public String myPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElseThrow();

        model.addAttribute("user", user);

        // GlobalModelAttributeAdvice 가 넣어준 isLawyer 를 활용
        Object isLawyerAttr = model.getAttribute("isLawyer");
        boolean isLawyer = (isLawyerAttr instanceof Boolean) && (Boolean) isLawyerAttr;

        if (isLawyer) {
            lawyerRepository.findByUserId(user.getId())
                    .ifPresent(lawyer -> model.addAttribute("lawyer", lawyer));
        }

        return "mypage/mypage";
    }

    // 정보 수정 폼
    @GetMapping("/edit")
    public String editProfileForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElseThrow();
        model.addAttribute("user", user);
        return "mypage/edit";
    }

    // 정보 수정 처리
    @PostMapping("/edit")
    public String editProfileSubmit(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam @NotBlank String name,
                                    @RequestParam String address,
                                    @RequestParam(required = false) String password) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElseThrow();

        user.setUsername(name);
        user.setAddress(address);

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        userRepository.save(user);
        return "redirect:/view/mypage?update=success";
    }

    // 비밀번호 변경 폼
    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "mypage/change-password";
    }

    // 비밀번호 변경 처리
    @PostMapping("/change-password")
    public String changePasswordSubmit(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam String currentPassword,
                                       @RequestParam String newPassword,
                                       Model model) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElseThrow();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
            return "mypage/change-password";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "redirect:/view/mypage?passwordChanged=true";
    }

    // 회원 탈퇴 확인
    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "mypage/withdraw";
    }

    @PostMapping("/withdraw")
    @Transactional
    public String withdraw(@AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        if (userDetails == null) return "redirect:/view/login";

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            userRepository.delete(user); // ← 연쇄 삭제 제대로 작동
        }

        session.invalidate();
        return "redirect:/view?withdraw=success";
    }

    // 내가 쓴 글
    @GetMapping("/posts")
    public String myPosts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Post> posts = postRepository.findByUser(user);
        model.addAttribute("posts", posts);
        return "mypage/myposts";
    }

    // 내가 쓴 댓글 (변호사만)
    @PreAuthorize("hasRole('LAWYER')") // 메서드 단 보호
    @GetMapping("/comments")
    public String myComments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        Lawyer lawyer = lawyerRepository.findByUser_Email(email).orElse(null);
        List<Comment> comments = (lawyer != null) ? commentRepository.findByLawyer(lawyer) : List.of();
        model.addAttribute("comments", comments);
        return "mypage/mycomments";
    }
}
