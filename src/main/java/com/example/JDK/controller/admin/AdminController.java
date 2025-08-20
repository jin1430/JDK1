package com.example.JDK.controller.admin;


// Refactor: categorised as admin controller; moved for structure-only readability.
import com.example.JDK.CrimeCategory;
import com.example.JDK.service.*;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final ReportService reportService;
    private final CategoryService categoryService;

    // 대시보드
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.countUsers());
        model.addAttribute("postCount", postService.countPosts());
        model.addAttribute("commentCount", commentService.countComments());
        return "admin/dashboard";
    }

    /* -------------------- 게시글 관리 -------------------- */
    // 목록 + 검색
    @GetMapping("/posts")
    public String listPosts(@RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        var postList = (keyword != null && !keyword.isBlank())
                ? postService.searchPostsIncludingComments(keyword)
                : postService.getAllPosts();

        model.addAttribute("postList", postList);
        model.addAttribute("keyword", keyword == null ? "" : keyword); // ★ 항상 채워넣기
        return "admin/posts";
    }

    // 수정 폼
    @GetMapping("/posts/{id}/edit")
    public String editPostForm(@PathVariable Long id, Model model) {
        var post = postService.getPost(id);
        model.addAttribute("post", post);
        model.addAttribute("categories", postService.getCategoryOptions(post.getCategory().getId()));
        var opts = Arrays.stream(CrimeCategory.values())
                .map(ec -> Map.of(
                        "id", ec.name(),                    // value
                        "name", ec.getKoreanName(),         // 라벨
                        "selected", post.getCategory().getName().equals(ec.getKoreanName())
                ))
                .toList();
        model.addAttribute("categories", opts);
        return "admin/post-edit";
    }

    // 수정 처리
    @PostMapping("/posts/{id}/edit")
    public String updatePost(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam String content,
                             @RequestParam Long categoryId) {
        postService.updatePostFromAdmin(id, title, content, categoryId);
        return "redirect:/admin/posts";
    }

    // 삭제
    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "redirect:/admin/posts";
    }

    /* -------------------- 댓글 관리 -------------------- */
    @GetMapping("/comments")
    public String listComments(Model model) {
        model.addAttribute("commentList", commentService.getAllComments());
        return "admin/comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id) {
        commentService.deleteCommentByAdmin(id);
        return "redirect:/admin/comments";
    }

    /* -------------------- 신고 관리 -------------------- */
    @GetMapping("/reports")
    public String listReports(Model model) {
        model.addAttribute("reportList", reportService.findAll());
        return "admin/reports";
    }

    @PostMapping("/reports/{id}/delete")
    public String deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return "redirect:/admin/reports";
    }
}
