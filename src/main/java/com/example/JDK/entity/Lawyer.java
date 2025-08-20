package com.example.JDK.entity;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.LawyerSpecialty;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "lawyers")
public class Lawyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // (소유자: Lawyer) 회원이 삭제되면 User 쪽 cascade로 이것도 같이 삭제됨
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private String certificateNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Builder.Default
    @ElementCollection(targetClass = LawyerSpecialty.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "lawyer_specialties", joinColumns = @JoinColumn(name = "lawyer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", nullable = false, length = 50)
    private Set<LawyerSpecialty> specialties = new HashSet<>();

    @Column(name = "profile_image")
    private String profileImage;

    /* ====== 연관관계: 변호사 → 댓글 ====== */
    @OneToMany(mappedBy = "lawyer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 편의 메서드
    public void addSpecialty(LawyerSpecialty specialty) {
        if (specialties == null) specialties = new HashSet<>();
        specialties.add(specialty);
    }

    public void removeSpecialty(LawyerSpecialty specialty) {
        if (specialties != null) specialties.remove(specialty);
    }

    public List<String> getSpecialtyKoreanNames() {
        List<String> names = new ArrayList<>();

        // Enum 기반 specialties (비어있을 수도 있음)
        if (specialties != null) {
            for (LawyerSpecialty s : specialties) {
                names.add(s.getKoreanName());
            }
        }

        // String 기반 specialty (시드에서 넣은 값)
        if (specialty != null && !specialty.isBlank()) {
            names.add(specialty);
        }

        return names;
    }

    // ✅ 프로필 필드들
    @Column(length = 100)
    private String title;   // 한 줄 소개

    @Lob
    private String bio;     // 상세 소개

    @Column(length = 100)
    private String office;  // 소속/사무소

    @Column(length = 255)
    private String career;  // 경력

    @Column(length = 100)
    private String specialty; // 전문 분야

    public String getCategoryKey() {
        // specialties 우선 사용 (ENUM)
        if (specialties != null && !specialties.isEmpty()) {
            return specialties.iterator().next().name(); // CRIMINAL, FAMILY 이런 ENUM 이름
        }
        // 없으면 기존 specialty (문자열)
        if (specialty != null) {
            return specialty.toUpperCase(); // 혹시 문자열일 때도 대문자로 변환
        }
        return "ETC"; // 기본값
    }
}
