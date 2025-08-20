// src/main/java/com/example/JDK/entity/Consultation.java
package com.example.JDK.entity;

import com.example.JDK.ConsultationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "consultations")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상담 요청자 (일반 유저) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User requester;

    /** 상담 받을 변호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private Lawyer lawyer;

    @Column(nullable = false)
    private String title;

    @Lob
    private String content;

    /** 변호사 답변 */
    @Lob
    private String lawyerReply;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationStatus status;

    private LocalDateTime requestDate;
    private LocalDateTime replyDate;

    @PrePersist
    public void onCreate() {
        if (requestDate == null) requestDate = LocalDateTime.now();
        if (status == null) status = ConsultationStatus.PENDING;
    }
}
