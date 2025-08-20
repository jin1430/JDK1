package com.example.JDK.controller.api;


// Refactor: categorised as api controller; moved for structure-only readability.
import com.example.JDK.dto.CreatePostRequest;
import com.example.JDK.dto.PostUpdateRequest;
import com.example.JDK.entity.Post;
import com.example.JDK.service.CategoryService;
import com.example.JDK.service.PostService;
import jakarta.validation.Valid; // ★ 중요;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<Post> createPost(@AuthenticationPrincipal UserDetails user,
                                           @Valid @RequestBody CreatePostRequest req) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Post post = new Post();
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setCategory(categoryService.getRef(req.getCategoryId()));

        Post saved = postService.createPost(user.getUsername(), post);
        return ResponseEntity.created(URI.create("/api/posts/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<Post> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{id}")
    public Post getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Post>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {

        Post updated = postService.updatePost(id, req, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
