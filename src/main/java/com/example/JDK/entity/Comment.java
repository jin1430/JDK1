package com.example.JDK.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 변호사 댓글: 변호사 삭제 시 이 댓글들도 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lawyer lawyer;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime generationDate;

    // 글에 달린 댓글: 글 삭제 시 댓글도 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    // 댓글에 대한 신고들: 댓글 삭제 시 신고도 삭제
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.generationDate == null) {
            this.generationDate = LocalDateTime.now();
        }
    }
}
