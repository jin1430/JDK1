package com.example.JDK.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generationDate;

    // 작성자: User가 삭제되면 이 글도 같이 삭제(외래키 레벨에서도 보강)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private long views;

    @PrePersist
    protected void onCreate() {
        if (this.generationDate == null) {
            this.generationDate = LocalDateTime.now();
        }
    }

    @Column(name = "image_url" ,nullable = true)
    private String imageUrl;

    @Formula("(SELECT COUNT(*) FROM comments c WHERE c.post_id = id)")
    private long commentsCount;

    /* ====== 연관관계: 게시글 → 댓글/신고 ====== */

    // 글의 댓글들: 글 삭제 시 댓글들도 같이 삭제
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 글 관련 신고들: 글 삭제 시 신고들도 같이 삭제
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports = new ArrayList<>();
}
