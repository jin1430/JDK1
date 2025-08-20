package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
@RequestMapping("/__disavled__")
@RequiredArgsConstructor
public class CommentController {

//    private final CommentService commentService;
//
//    // 댓글 등록 (서비스에서: 변호사 검증 + 저장 + 알림 생성까지 처리)
//    @PostMapping("/view/posts/{postId}/comments")
//    public String createComment(@PathVariable Long postId,
//                                @RequestParam String content,
//                                @AuthenticationPrincipal UserDetails userDetails) {
//        String email = userDetails.getUsername();
//        try {
//            commentService.createComment(postId, email, content);  // ★ 여기서 알림도 같이 생성됨
//            return "redirect:/view/posts/" + postId;
//        } catch (IllegalArgumentException e) {
//            // 변호사 아님, 게시글 없음, 내용 비었음 등 서비스에서 던진 오류
//            return "redirect:/view/posts/" + postId + "?error=" + e.getMessage();
//        } catch (Exception e) {
//            return "redirect:/view/posts/" + postId + "?error=comment-failed";
//        }
//    }
//
//    // ✅ 댓글 수정 폼
//    @GetMapping("/view/posts/{postId}/comments/{commentId}/edit")
//    public String editForm(@PathVariable Long postId,
//                           @PathVariable Long commentId,
//                           @AuthenticationPrincipal UserDetails userDetails,
//                           Model model) {
//        var c = commentService.getCommentById(commentId);
//
//        // 본인(해당 변호사)만 수정 가능
//        String email = userDetails.getUsername();
//        if (!c.getLawyer().getUser().getEmail().equals(email)) {
//            return "redirect:/view/posts/" + postId + "?error=notallowed#c" + commentId;
//        }
//
//        model.addAttribute("postId", postId);
//        model.addAttribute("comment", c);
//        return "comments/edit";
//    }
//    // ✅ 댓글 수정 처리
//    @PostMapping("/view/posts/{postId}/comments/{commentId}/edit")
//    public String edit(@PathVariable Long postId,
//                       @PathVariable Long commentId,
//                       @RequestParam String content,
//                       @AuthenticationPrincipal UserDetails userDetails) {
//        String email = userDetails.getUsername();
//        boolean ok = commentService.editComment(commentId, email, content);
//        String suffix = ok ? "#c" + commentId : "?error=notallowed#c" + commentId;
//        return "redirect:/view/posts/" + postId + suffix;
//    }
//    // 댓글 삭제 (서비스에 위임)
//    @PostMapping("/view/posts/{postId}/comments/{commentId}/delete")
//    public String deleteComment(@PathVariable Long postId,
//                                @PathVariable Long commentId,
//                                @AuthenticationPrincipal UserDetails userDetails) {
//        String email = userDetails.getUsername();
//        boolean ok = commentService.deleteComment(commentId, email);
//        return "redirect:/view/posts/" + postId + (ok ? "" : "?error=notallowed");
//    }
}
