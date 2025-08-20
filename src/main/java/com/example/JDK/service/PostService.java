package com.example.JDK.service;

import com.example.JDK.CrimeCategory;
import com.example.JDK.dto.PostUpdateRequest;
import com.example.JDK.entity.Category;
import com.example.JDK.entity.Post;
import com.example.JDK.entity.User;
import com.example.JDK.repository.CategoryRepository;
import com.example.JDK.repository.CommentRepository;
import com.example.JDK.repository.PostRepository;
import com.example.JDK.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
@Service
@RequiredArgsConstructor
public class PostService {

    // region 목록/상세

    // endregion

    // region 작성/수정/삭제

    // endregion

    // region 검색/기타

    // endregion


    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    // 이미지 저장 (로컬 예시) → "/uploads/posts/{uuid}_{filename}"
    @Transactional
    public String savePostImage(MultipartFile image) {
        if (image == null || image.isEmpty()) return null;
        try {
            String base = System.getProperty("user.dir");
            Path uploadDir = Paths.get(base, "uploads", "posts");
            Files.createDirectories(uploadDir);
            String clean = Objects.requireNonNull(image.getOriginalFilename()).replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = UUID.randomUUID() + "_" + clean;
            Path dest = uploadDir.resolve(filename);
            image.transferTo(dest.toFile());
            return "/uploads/posts/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장 실패", e);
        }
    }

    @Transactional
    public Post createPost(String userEmail, Post post) {
        User writer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("작성자 이메일이 존재하지 않습니다: " + userEmail));
        post.setUser(writer);
        if (post.getGenerationDate() == null) {
            post.setGenerationDate(LocalDateTime.now());
        }
        if (post.getViews() == 0) {
            post.setViews(0);
        }
        return postRepository.save(post);
    }

    @Transactional
    public Post updatePost(Long id, PostUpdateRequest req, String editorEmail) {
        Post origin = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. id=" + id));
        origin.setTitle(req.title());
        origin.setContent(req.content());
        if (req.categoryId() != null) {
            origin.setCategory(categoryService.getRef(req.categoryId()));
        }
        return origin;
    }

    @Transactional
    public void deletePost(Long id) { postRepository.deleteById(id); }

    @Transactional(readOnly = true)
    public long countPosts() { return postRepository.count(); }

    @Transactional(readOnly = true)
    public List<Post> searchPostsIncludingComments(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllPosts();
        }
        List<Post> a = postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, keyword);
        List<Post> b = commentRepository.findPostsByCommentContent(keyword);
        return Stream.concat(a.stream(), b.stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new));
    }

    @Transactional
    public void increaseViews(Long id) {
        Post p = postRepository.findById(id).orElseThrow();
        p.setViews(p.getViews() + 1);
    }

    @Transactional(readOnly = true)
    public List<Post> findTop5Latest() { return postRepository.findTop5ByOrderByGenerationDateDesc(); }

    @Transactional(readOnly = true)
    public Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음: " + id));
    }

    @Transactional
    public void updatePostFromAdmin(Long id, String title, String content, Long categoryId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음: " + id));
        if (title   != null) p.setTitle(title);
        if (content != null) p.setContent(content);
        if (categoryId != null) p.setCategory(categoryService.getRef(categoryId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryOptions(Long selectedId) {
        return categoryService.findAll().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("selected", Objects.equals(c.getId(), selectedId));
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Post createPostFromEnum(String userEmail, Post post, CrimeCategory enumCat) {
        User writer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("작성자 없음: " + userEmail));
        Category cat = categoryService.findByNameOrThrow(enumCat.getKoreanName());
        post.setUser(writer);
        post.setCategory(cat);
        if (post.getViews() == 0) post.setViews(0);
        return postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Post getPostDetail(Long id) {
        return postRepository.findDetail(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음: " + id));
    }

    @Transactional
    public void updatePostFromEnum(Long id, String title, String content, CrimeCategory ec) {
        var post = getPostById(id);
        post.setTitle(title);
        post.setContent(content);
        applyCrimeCategory(post, ec);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public boolean isPostInCategory(Post post, CrimeCategory ec) {
        return mapCategoryToEnum(post).equals(ec);
    }

    private void applyCrimeCategory(Post post, CrimeCategory ec) {
        String name = ec.getKoreanName();
        Category cat = categoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없음: " + name));
        post.setCategory(cat);
    }

    @Transactional(readOnly = true)
    public CrimeCategory mapCategoryToEnum(Post post) {
        String name = post.getCategory() != null ? post.getCategory().getName() : null;
        if (name == null) throw new IllegalStateException("게시글에 카테고리가 없습니다.");
        for (CrimeCategory ec : CrimeCategory.values()) {
            if (name.equals(ec.getKoreanName()) || name.equals(ec.name())) {
                return ec;
            }
        }
        throw new IllegalArgumentException("매핑 불가 카테고리: " + name);
    }
    public List<Post> searchByKeyword(String keyword) {
        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword);
    }
}
