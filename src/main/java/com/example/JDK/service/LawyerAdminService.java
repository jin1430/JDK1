// src/main/java/com/example/JDK/service/LawyerAdminService.java
package com.example.JDK.service;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.repository.LawyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional
public class LawyerAdminService {

    private final LawyerRepository lawyerRepository;
    private final NotificationService notificationService; // ✅ 알림 서비스 주입

    @Transactional(readOnly = true)
    public Page<Lawyer> listAll(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "id"));
        return lawyerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Lawyer> listByStatus(ApprovalStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1),
                Sort.by(Sort.Direction.DESC,"id"));
        return lawyerRepository.findByApprovalStatus(status, pageable);
    }

    public void approve(Long id) { changeStatus(id, ApprovalStatus.APPROVED); }

    public void reject(Long id)  { changeStatus(id, ApprovalStatus.REJECTED); }

    /** 공통 상태 변경 + 알림 */
    public void changeStatus(Long id, ApprovalStatus newStatus) {
        Lawyer l = lawyerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("변호사를 찾을 수 없습니다: " + id));

        var old = l.getApprovalStatus();
        if (old != newStatus) {
            l.setApprovalStatus(newStatus);
            notificationService.createForLawyerApproval(l, newStatus); // ✅ 알림 생성
        }
    }

    private void sendStatusNotification(Lawyer l, ApprovalStatus st) {
        var user = l.getUser();
        if (user == null) return;

        String title = "변호사 승인 상태 변경";
        String msg;
        switch (st) {
            case APPROVED ->
                    msg = "변호사 인증이 승인되었습니다. 서비스를 이용하실 수 있어요.";
            case REJECTED ->
                    msg = "변호사 인증이 반려되었습니다. 자격번호/서류를 확인해 다시 신청해 주세요.";
            default ->
                    msg = "변호사 인증 상태가 변경되었습니다: " + st.name();
        }
        // 클릭 시 이동할 URL (원하는 경로로 바꿔도 됨)
        String url = "/view/mypage";

        // 타입은 프로젝트 enum/설계에 맞춰 변경 가능
        // lawyer 객체와 바꿀 상태(APPROVED/REJECTED)가 있어야 함
        notificationService.createForLawyerApproval(l, ApprovalStatus.APPROVED); // or REJECTED

    }
}
