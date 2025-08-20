package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.
import com.example.JDK.entity.Comment;
import com.example.JDK.entity.Post;
import com.example.JDK.entity.User;
import com.example.JDK.service.CommentService;
import com.example.JDK.service.PostService;
import com.example.JDK.service.ReportService;
import com.example.JDK.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;

    @PostMapping("/post/{postId}")
    public String reportPost(@PathVariable Long postId,
                             @RequestParam String reason,
                             HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        User reporter = userService.findByEmail(email);
        Post post = postService.getPostById(postId);

        reportService.reportPost(reporter, post, reason);
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/comment/{commentId}")
    public String reportComment(@PathVariable Long commentId,
                                @RequestParam String reason,
                                HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        User reporter = userService.findByEmail(email);
        Comment comment = commentService.getCommentById(commentId);

        reportService.reportComment(reporter, comment, reason);
        return "redirect:/posts/" + comment.getPost().getId();
    }
}
