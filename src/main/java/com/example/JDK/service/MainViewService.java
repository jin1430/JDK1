package com.example.JDK.service;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.LawyerSpecialty;
import com.example.JDK.entity.Category;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.Post;
import com.example.JDK.repository.LawyerRepository;
import com.example.JDK.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor

public class MainViewService {
    private final PostRepository postRepository;
    private final CategoryService categoryService;
    private final LawyerRepository lawyerRepository;

    /* 최신 글 카드들 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> latestPosts(int limit) {
        // 있으면 가장 좋은 메서드: postRepository.findAllByOrderByGenerationDateDesc()
        List<Post> posts = postRepository.findTop5ByOrderByGenerationDateDesc(); // 없으면 교체: findAll 정렬 후 limit
        if (limit > 5) {
            posts = postRepository.findAll().stream()
                    .sorted(Comparator.comparing(Post::getGenerationDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(limit)
                    .toList();
        }
        return posts.stream()
                .map(this::toPostCard)
                .collect(Collectors.toList());
    }

    /* 카테고리별 섹션: 각 카테고리에서 최신 N개 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> categorySections(int postsPerCategory) {
        List<Category> categories = categoryService.findAll();
        // 전체 최신 글 미리 가져와서 카테고리로 그룹
        List<Post> all = postRepository.findAll().stream()
                .sorted(Comparator.comparing(Post::getGenerationDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        List<Map<String, Object>> sections = new ArrayList<>();
        for (Category cat : categories) {
            List<Map<String, Object>> postCards = all.stream()
                    .filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getId(), cat.getId()))
                    .limit(postsPerCategory)
                    .map(this::toPostCard)
                    .toList();

            if (!postCards.isEmpty()) {
                sections.add(Map.of(
                        "title", cat.getName(),
                        "posts", postCards
                ));
            }
        }
        return sections;
    }
    private String categoryKeyOf(Lawyer l) {
        // 1) Set<LawyerSpecialty>가 있으면 그걸 사용
        if (l.getSpecialties() != null && !l.getSpecialties().isEmpty()) {
            return l.getSpecialties().iterator().next().name(); // CRIMINAL, CIVIL, ...
        }
        // 2) String specialty(한글/영문)로 보완
        String s = l.getSpecialty();
        if (s != null && !s.isBlank()) {
            // 영문 코드로 저장된 경우
            try { return LawyerSpecialty.valueOf(s.toUpperCase()).name(); } catch (Exception ignore) {}
            // 한글로 저장된 경우
            for (var ls : LawyerSpecialty.values()) {
                if (ls.getKoreanName().equals(s) || ls.getKoreanName().equalsIgnoreCase(s)) {
                    return ls.name();
                }
            }
        }
        return "all"; // 기본
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lawyerCards(int limit) {
        List<Lawyer> lawyers = lawyerRepository.findAll().stream()
                .filter(l -> l.getApprovalStatus() == ApprovalStatus.APPROVED)
                .limit(limit)
                .toList();

        return lawyers.stream().map(l -> {
            var user = l.getUser();
            String primaryKey = categoryKeyOf(l); // ★ 여기만 교체

            // 칩(표시용 한글 라벨)도 specialties가 비었으면 String specialty로 보완
            List<String> chips = (l.getSpecialties() != null && !l.getSpecialties().isEmpty())
                    ? l.getSpecialties().stream().map(LawyerSpecialty::getKoreanName).toList()
                    : (l.getSpecialty() != null && !l.getSpecialty().isBlank()
                    ? List.of(l.getSpecialty()) : List.of());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("categoryKey", primaryKey);                       // 카드의 data-category에 들어감
            m.put("avatarUrl", Optional.ofNullable(l.getProfileImage())
                    .orElse("/images/lawyers/lawyer01.png"));
            m.put("name", user.getUsername());
            if (user.getAddress() != null && !user.getAddress().isBlank())
                m.put("office", user.getAddress());
            m.put("chips", chips);
            return m;
        }).toList();
    }

    /* ---- 내부 헬퍼 ---- */

    private Map<String, Object> toPostCard(Post p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("title", p.getTitle());
        m.put("content", p.getContent());
        m.put("views", p.getViews());
        if (p.getCategory() != null) {
            m.put("categoryPath", p.getCategory().getName());
        }
        if (p.getGenerationDate() != null) {
            m.put("ago", humanize(p.getGenerationDate()));
        }
        return m;
    }

    private String humanize(LocalDateTime t) {
        Duration d = Duration.between(t, LocalDateTime.now());
        long sec = Math.max(1, d.getSeconds());
        if (sec < 60) return sec + "초 전";
        long min = sec / 60;
        if (min < 60) return min + "분 전";
        long hr = min / 60;
        if (hr < 24) return hr + "시간 전";
        long day = hr / 24;
        if (day < 7) return day + "일 전";
        long wk = day / 7;
        if (wk < 5) return wk + "주 전";
        long mon = day / 30;
        if (mon < 12) return mon + "개월 전";
        long yr = day / 365;
        return yr + "년 전";
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> postCategoryTabs() {
        List<Map<String, Object>> tabs = new ArrayList<>();
        tabs.add(Map.of("key","all","label","전체","active",true));
        for (var c : categoryService.findAll()) {
            tabs.add(Map.of(
                    "key", String.valueOf(c.getId()), // data-category 키
                    "label", c.getName(),
                    "active", false
            ));
        }
        return tabs;
    }

    // 게시글 카드 데이터 (categoryKey 포함)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> postCards(int limit) {
        // 최신순으로 전부 가져와 자른다 (필요시 Repository에 전용 메서드 추가 가능)
        var all = postRepository.findAll().stream()
                .sorted(Comparator.comparing(Post::getGenerationDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();

        return all.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("content", p.getContent());
            m.put("views", p.getViews());
            if (p.getCategory() != null) {
                m.put("categoryPath", p.getCategory().getName());
                m.put("categoryKey", String.valueOf(p.getCategory().getId())); // ✅ 필터 키
            } else {
                m.put("categoryKey", "uncat");
            }
            if (p.getGenerationDate() != null) {
                m.put("ago", humanize(p.getGenerationDate())); // 네가 이미 쓰는 humanize 메서드 재사용
            }
            return m;
        }).collect(Collectors.toList());
    }
}
