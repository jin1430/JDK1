package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.

import com.example.JDK.CrimeCategory;
import com.example.JDK.entity.Comment;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.Post;
import com.example.JDK.entity.User;
import com.example.JDK.service.CommentService;
import com.example.JDK.service.LawyerService;
import com.example.JDK.service.PostService;
import com.example.JDK.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.*;
@Controller
@RequiredArgsConstructor
@RequestMapping("/view/posts")
public class PostController {

    // region 목록/상세

    // endregion

    // region 작성/수정/삭제

    // endregion

    // region 검색/기타

    // endregion


    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final LawyerService lawyerService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 목록
    @GetMapping
    public String listPosts(Model model) {
        model.addAttribute("posts", postService.getAllPosts());
        return "posts/list";
    }

    // 새 글 폼
    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                          Model model) {
        if (auth == null) {
            return "redirect:/view/login?continue=/view/posts/new";
        }
        var opts = Arrays.stream(CrimeCategory.values())
                .map(ec -> Map.of("value", ec.name(), "label", ec.getKoreanName()))
                .toList();
        model.addAttribute("categories", opts);
        return "posts/new";
    }

    // 등록 (이미지 선택 지원)
    @PostMapping("/new")
    public String create(@AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam String crimeCategory,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         RedirectAttributes ra) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/new";

        var ec = CrimeCategory.valueOf(crimeCategory);
        var post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setViews(0);

        // 이미지 저장 (선택)
        if (image != null && !image.isEmpty()) {
            String url = postService.savePostImage(image); // "/uploads/..." 형태 반환
            post.setImageUrl(url);
        }

        postService.createPostFromEnum(auth.getUsername(), post, ec);
        ra.addFlashAttribute("msg", "등록되었습니다.");
        return "redirect:/view/posts/" + post.getId();
    }

    // 상세
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                         Model model) {

        Post post = postService.getPostDetail(id);
        if (post == null) return "redirect:/view/posts";

        boolean isMyPost = false;
        User me = null;
        if (auth != null) {
            me = userService.findByEmail(auth.getUsername());
            if (me != null && post.getUser() != null) {
                isMyPost = Objects.equals(post.getUser().getId(), me.getId());
            }
        }

        // 로그인 사용자가 변호사인지 확인
        boolean isLawyer = false;
        Lawyer myLawyer = null;
        if (me != null) {
            try {
                myLawyer = lawyerService.getByUserId(me.getId());
                isLawyer = (myLawyer != null);
            } catch (Exception ignore) { }
        }

        // 댓글 로드 → 뷰모델
        List<Comment> comments = commentService.findByPostForView(id);
        List<Map<String, Object>> commentVMs = new ArrayList<>();
        for (Comment c : comments) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("content", c.getContent());
            if (c.getGenerationDate() != null) {
                m.put("createdAt", DATE_FMT.format(c.getGenerationDate()));
            }

            Lawyer commentLawyer = c.getLawyer();
            if (commentLawyer != null) {
                String lawyerName = safeDisplayName(commentLawyer.getUser());
                if (!lawyerName.isBlank()) m.put("lawyerName", lawyerName);
                String prof = toPublicUrl(commentLawyer.getProfileImage());
                if (prof != null && !prof.isBlank()) m.put("lawyerProfileImage", prof);
            }

            boolean canManage = isLawyer && commentLawyer != null && myLawyer != null
                    && Objects.equals(commentLawyer.getId(), myLawyer.getId());
            m.put("canManage", canManage);

            m.put("lawyer", commentLawyer); // 폴백용
            commentVMs.add(m);
        }

        Map<String, Object> postView = new HashMap<>();
        postView.put("id", post.getId());
        postView.put("title", post.getTitle());
        postView.put("content", post.getContent());
        postView.put("generationDate", post.getGenerationDate() != null ? DATE_FMT.format(post.getGenerationDate()) : "");
        if (post.getCategory() != null) {
            postView.put("category", post.getCategory());
            postView.put("categoryName", post.getCategory().getName());
        } else {
            postView.put("categoryName", null);
        }
        postView.put("imageUrl", toPublicUrl(post.getImageUrl()));

        model.addAttribute("post", postView);
        model.addAttribute("isMyPost", isMyPost);
        model.addAttribute("comments", commentVMs);
        model.addAttribute("commentsCount", commentVMs.size());

        // ✅ 변호사만 댓글 작성 가능
        model.addAttribute("canWriteComment", isLawyer);

        return "posts/detail";
    }

    // ===== 댓글: 변호사만 =====

    @PostMapping("/{postId}/comments")
    public String createComment(@PathVariable Long postId,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                                @RequestParam String content,
                                RedirectAttributes ra) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/" + postId;
        User me = userService.findByEmail(auth.getUsername());
        if (me == null) return "redirect:/view/login?continue=/view/posts/" + postId;

        Lawyer lawyer = lawyerService.getByUserId(me.getId()); // 변호사만 허용 (없으면 예외)
        commentService.createByLawyer(postId, lawyer.getId(), content);
        ra.addFlashAttribute("msg", "댓글이 등록되었습니다.");
        return "redirect:/view/posts/" + postId;
    }

    @GetMapping("/{postId}/comments/{commentId}/edit")
    public String editCommentForm(@PathVariable Long postId,
                                  @PathVariable Long commentId,
                                  @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                                  Model model) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/" + postId;
        User me = userService.findByEmail(auth.getUsername());
        Lawyer myLawyer = lawyerService.getByUserId(me.getId());

        Comment c = commentService.getCommentById(commentId);
        if (!Objects.equals(c.getPost().getId(), postId)) return "redirect:/view/posts/" + postId;
        if (!Objects.equals(c.getLawyer().getId(), myLawyer.getId())) return "redirect:/view/posts/" + postId;

        Map<String, Object> postVM = Map.of("id", postId);
        Map<String, Object> commentVM = Map.of("id", c.getId(), "content", c.getContent());
        model.addAttribute("post", postVM);
        model.addAttribute("comment", commentVM);
        return "comments/edit";
    }

    @PostMapping("/{postId}/comments/{commentId}/edit")
    public String updateComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                                @RequestParam String content,
                                RedirectAttributes ra) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/" + postId;
        User me = userService.findByEmail(auth.getUsername());
        Lawyer myLawyer = lawyerService.getByUserId(me.getId());

        commentService.updateByLawyer(postId, commentId, myLawyer.getId(), content);
        ra.addFlashAttribute("msg", "댓글이 수정되었습니다.");
        return "redirect:/view/posts/" + postId;
    }

    @PostMapping("/{postId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                                RedirectAttributes ra) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/" + postId;
        User me = userService.findByEmail(auth.getUsername());
        Lawyer myLawyer = lawyerService.getByUserId(me.getId());

        commentService.deleteByLawyer(postId, commentId, myLawyer.getId());
        ra.addFlashAttribute("msg", "댓글이 삭제되었습니다.");
        return "redirect:/view/posts/" + postId;
    }

    // ===== 글 수정/삭제 =====

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                           Model model) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/" + id + "/edit";

        var post = postService.getPostById(id);
        if (!canModify(post, auth)) return "redirect:/view/posts/" + id;

        var current = postService.mapCategoryToEnum(post);
        var opts = Arrays.stream(CrimeCategory.values())
                .map(ec -> Map.of(
                        "value", ec.name(),
                        "label", ec.getKoreanName(),
                        "selected", ec == current
                ))
                .toList();

        model.addAttribute("post", post);
        model.addAttribute("categories", opts);
        return "posts/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam String crimeCategory,
                         RedirectAttributes ra) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/" + id + "/edit";

        var post = postService.getPostById(id);
        if (!canModify(post, auth)) {
            ra.addFlashAttribute("error", "수정 권한이 없습니다.");
            return "redirect:/view/posts/" + id;
        }

        var ec = CrimeCategory.valueOf(crimeCategory);
        postService.updatePostFromEnum(id, title, content, ec);
        ra.addFlashAttribute("msg", "수정되었습니다.");
        return "redirect:/view/posts/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                         RedirectAttributes ra) {
        if (auth == null) return "redirect:/view/login?continue=/view/posts/" + id;

        var post = postService.getPostById(id);
        if (!canModify(post, auth)) {
            ra.addFlashAttribute("error", "삭제 권한이 없습니다.");
            return "redirect:/view/posts/" + id;
        }

        postService.deletePost(id);
        ra.addFlashAttribute("msg", "삭제되었습니다.");
        return "redirect:/view/posts";
    }

    // 작성자 또는 관리자만 허용
    private boolean canModify(Post post, org.springframework.security.core.userdetails.User auth) {
        if (post == null || auth == null) return false;

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        boolean isAuthor = (post.getUser() != null) &&
                auth.getUsername().equalsIgnoreCase(post.getUser().getEmail());

        return isAdmin || isAuthor;
    }

    private String safeDisplayName(User u) {
        if (u == null) return "";
        String[] order = {"name", "realName", "fullName", "username", "nickname", "email"};
        for (String f : order) {
            String v = reflectString(u, f);
            if (v != null && !v.isBlank()) {
                if ("email".equals(f)) {
                    int at = v.indexOf('@');
                    return (at > 0) ? v.substring(0, at) : v;
                }
                return v;
            }
        }
        return "";
    }
    private String reflectString(User u, String field) {
        try {
            var m = u.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
            Object val = m.invoke(u);
            return val == null ? "" : String.valueOf(val);
        } catch (Exception e) {
            return "";
        }
    }
    private String toPublicUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) return null;
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl;

        String s = pathOrUrl.replace("\\", "/");

        // ✅ 정적 리소스(images/)는 그대로 사용
        if (s.startsWith("/images/") || s.startsWith("images/")) return s;

        // ✅ 업로드 파일만 /uploads/로 감싸기
        if (s.startsWith("/uploads/")) return s;
        if (s.startsWith("uploads/")) return "/" + s;

        return "/uploads/" + s.replaceFirst("^/+", "");
    }
    @GetMapping("/search")
    public String searchPosts(@RequestParam("keyword") String keyword, Model model) {
        // 1. 서비스에서 검색 결과 가져오기
        List<Post> posts = postService.searchByKeyword(keyword);

        // 2. 모델에 값 넣기 (템플릿에서 사용)
        model.addAttribute("keyword", keyword);
        model.addAttribute("postList", posts);

        // 3. 검색 결과 페이지로 이동
        return "posts/search-result";
    }
}
