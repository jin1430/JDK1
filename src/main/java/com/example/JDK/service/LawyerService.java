// src/main/java/com/example/JDK/service/LawyerService.java
package com.example.JDK.service;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.LawyerSpecialty;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.User;
import com.example.JDK.repository.LawyerRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional
public class LawyerService {

    // region 목록/상세

    // endregion

    // region 작성/수정/삭제

    // endregion

    // region 검색/기타

    // endregion


    private final LawyerRepository lawyerRepository;

    // 변호사 등록
    public Lawyer registerLawyer(Lawyer lawyer) {
        return lawyerRepository.save(lawyer);
    }

    // 변호사 수정
    public Lawyer updateLawyer(Long id, Lawyer updatedLawyer) {
        Lawyer lawyer = lawyerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 변호사를 찾을 수 없습니다."));
        lawyer.setCertificateNumber(updatedLawyer.getCertificateNumber());
        lawyer.setApprovalStatus(updatedLawyer.getApprovalStatus());
        return lawyer; // @Transactional로 Dirty Checking 반영
    }

    // 단건 조회 (null 허용)
    @Transactional(readOnly = true)
    public Lawyer getById(Long id) {
        return lawyerRepository.findById(id).orElse(null);
    }

    // 유저 ID로 변호사 조회 (없으면 예외) — 변호사 전용 기능(댓글/프로필 업로드) 검증에 사용
    @Transactional(readOnly = true)
    public Lawyer getByUserId(Long userId) {
        return lawyerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("변호사 정보가 없습니다."));
    }

    // 변호사 ID로 단건 조회 (없으면 예외)
    public Lawyer getLawyerById(Long id) {
        return lawyerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 변호사를 찾을 수 없습니다."));
    }

    // 전체 조회
    public List<Lawyer> getAllLawyers() {
        return lawyerRepository.findAll();
    }

    // 승인 대기 목록
    public List<Lawyer> getPendingLawyers() {
        return lawyerRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    // 삭제
    public boolean deleteLawyer(Long lawyerId) {
        if (lawyerRepository.existsById(lawyerId)) {
            lawyerRepository.deleteById(lawyerId);
            return true;
        }
        return false;
    }

    // 승인/거절
    public void approveLawyer(Long lawyerId) {
        lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 변호사를 찾을 수 없습니다."))
                .setApprovalStatus(ApprovalStatus.APPROVED);
    }

    public void rejectLawyer(Long lawyerId) {
        lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 변호사를 찾을 수 없습니다."))
                .setApprovalStatus(ApprovalStatus.REJECTED);
    }

    public List<Lawyer> getApprovedLawyers() {
        return lawyerRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
    }

    public List<Lawyer> getApprovedLawyersBySpecialty(LawyerSpecialty spec) {
        return lawyerRepository.findBySpecialtyAndStatus(spec, ApprovalStatus.APPROVED);
    }

    // ===================== 검색 =====================

    public Page<Lawyer> searchLawyers(String q,
                                      String region,
                                      List<LawyerSpecialty> selectedSpecs,
                                      int page, int size) {

        Specification<Lawyer> spec = Specification.where(approvedOnly());

        if (q != null && !q.isBlank()) {
            spec = spec.and(nameContains(q));
        }
        if (region != null && !region.isBlank()) {
            spec = spec.and(regionContains(region));
        }
        if (selectedSpecs != null && !selectedSpecs.isEmpty()) {
            spec = spec.and(hasAnySpecialty(selectedSpecs)); // 여러 개 선택 시 OR
        }

        return lawyerRepository.findAll(spec, PageRequest.of(page, size));
    }

    private Specification<Lawyer> approvedOnly() {
        return (root, query, cb) -> cb.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED);
    }

    // User의 이름 유사검색(존재하는 필드만 OR)
    private Specification<Lawyer> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();

            Join<?, ?> user = root.join("user");
            String pat = "%" + keyword.toLowerCase(Locale.ROOT) + "%";

            String[] candidates = {"name", "username", "realName", "fullName", "nickname"};
            List<Predicate> ors = new ArrayList<>();
            for (String f : candidates) {
                if (hasUserMember(f)) {
                    ors.add(cb.like(cb.lower(user.get(f)), pat));
                }
            }
            return ors.isEmpty() ? cb.conjunction() : cb.or(ors.toArray(new Predicate[0]));
        };
    }

    // 지역 유사검색(필드 있을 때만)
    private Specification<Lawyer> regionContains(String region) {
        return (root, query, cb) -> {
            if (region == null || region.isBlank() || !hasUserMember("region")) {
                return cb.conjunction();
            }
            Join<?, ?> user = root.join("user");
            String pat = "%" + region.toLowerCase(Locale.ROOT) + "%";
            return cb.like(cb.lower(user.get("region")), pat);
        };
    }

    // 전문분야: 하나라도 포함(OR)
    private Specification<Lawyer> hasAnySpecialty(List<LawyerSpecialty> specs) {
        return (root, query, cb) -> {
            if (specs == null || specs.isEmpty()) return cb.conjunction();
            List<Predicate> ors = new ArrayList<>();
            for (LawyerSpecialty s : specs) {
                ors.add(cb.isMember(s, root.get("specialties")));
            }
            query.distinct(true);
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    // 화면 렌더링용 전체 전문분야
    public List<LawyerSpecialty> allSpecialties() {
        return List.of(LawyerSpecialty.values());
    }

    // ----- User 메타 반사 체크 -----
    private boolean hasUserMember(String field) {
        // 필드 존재 여부
        for (Field f : User.class.getDeclaredFields()) {
            if (f.getName().equals(field)) return true;
        }
        // getter 존재 여부
        String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
        try {
            Method m = User.class.getMethod(getter);
            return m != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // ===== 프로필 이미지 =====
    public Lawyer updateProfileImageUrl(Long lawyerId, String imageUrl) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 변호사를 찾을 수 없습니다."));
        lawyer.setProfileImage(imageUrl);
        return lawyer;
    }

    public void removeProfileImage(Long lawyerId) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 변호사를 찾을 수 없습니다."));
        lawyer.setProfileImage(null);
    }

    public Lawyer updateProfileImage(Long lawyerId, String storedPathOrUrl) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 변호사를 찾을 수 없습니다."));
        lawyer.setProfileImage(storedPathOrUrl);
        return lawyer;
    }
    @Transactional(readOnly = true)
    public Optional<Lawyer> findById(Long id) {
        return lawyerRepository.findById(id);
    }
    @Transactional(readOnly = true)
    public Optional<Lawyer> findDetailById(Long id) {
        return lawyerRepository.findDetailById(id); // JPQL에서 fetch join
    }
}
