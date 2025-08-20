package com.example.JDK.controller.admin;


// Refactor: categorised as admin controller; moved for structure-only readability.
import com.example.JDK.Role; // 패키지 확인: com.example.JDK.entity.Role 사용;
import com.example.JDK.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/members")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final UserService userService;

    // 목록 + 검색 (?keyword=)
    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        var members = (keyword != null && !keyword.isBlank())
                ? userService.searchByEmail(keyword)
                : userService.findAll();
        model.addAttribute("members", members);
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    // 수정 폼
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("roleIsMember", user.getRole().name().equals("MEMBER"));
        model.addAttribute("roleIsLawyer", user.getRole().name().equals("LAWYER"));
        model.addAttribute("roleIsAdmin",  user.getRole().name().equals("ADMIN"));
        return "admin/user-edit";
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String username,
                         @RequestParam(required = false) String address,
                         @RequestParam Role role) {
        userService.updateUserFields(id, username, address, role);
        return "redirect:/admin/members";
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/admin/members";
    }
}
