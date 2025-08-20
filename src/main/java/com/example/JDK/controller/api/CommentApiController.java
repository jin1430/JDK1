package com.example.JDK.controller.api;


// Refactor: categorised as api controller; moved for structure-only readability.
import com.example.JDK.entity.Comment;
import com.example.JDK.service.CommentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentApiController {

    private final CommentService commentService;

    // 댓글 생성 (lawyer 권한 필요)
    @PostMapping
    public Comment createComment(
            @RequestParam Long postId,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails) {

        String lawyerEmail = userDetails.getUsername();
        Comment comment = commentService.createComment(postId, lawyerEmail, content);
        if (comment == null) {
            throw new IllegalArgumentException("권한이 없거나 게시글이 존재하지 않습니다.");
        }
        return comment;
    }


    // 댓글 전체 조회 (옵션)
    @GetMapping
    public List<Comment> getAllComments() {
        return commentService.getAllComments();
    }

    // 댓글 수정 (lawyer 권한 필요)
    @PutMapping("/{id}")
    public boolean updateComment(
            @PathVariable Long id,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails) {

        String lawyerEmail = userDetails.getUsername();
        return commentService.editComment(id, lawyerEmail, content);
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id) {
        commentService.deleteCommentByAdmin(id);
        return "redirect:/admin/comments";
    }
}
