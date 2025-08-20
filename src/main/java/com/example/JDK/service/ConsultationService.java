// src/main/java/com/example/JDK/service/ConsultationService.java
package com.example.JDK.service;

import com.example.JDK.ConsultationStatus;
import com.example.JDK.entity.Consultation;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.User;
import com.example.JDK.repository.ConsultationRepository;
import com.example.JDK.repository.LawyerRepository;
import com.example.JDK.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final LawyerRepository lawyerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** 상담 생성 */
    public Consultation create(Long lawyerId, String requesterEmail, String title, String content) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("요청자 정보를 찾을 수 없습니다."));
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("변호사 없음"));

        Consultation c = Consultation.builder()
                .requester(requester)
                .lawyer(lawyer)
                .title(title)
                .content(content)
                .status(ConsultationStatus.PENDING)
                .build();
        c = consultationRepository.save(c);

        // 변호사에게 알림
        try { notificationService.createForConsultationRequest(c); } catch (Exception ignore) {}

        return c;
    }

    /** 변호사 답변 */
    public void reply(Long consultationId, String lawyerEmail, String reply) {
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("상담 없음"));
        String ownerEmail = c.getLawyer().getUser().getEmail();
        if (!ownerEmail.equals(lawyerEmail)) {
            throw new SecurityException("본인 상담만 답변 가능");
        }
        c.setLawyerReply(reply);
        c.setStatus(ConsultationStatus.COMPLETED);
        c.setReplyDate(LocalDateTime.now());

        // 요청자에게 알림
        try { notificationService.createForConsultationReply(c); } catch (Exception ignore) {}
    }

    /** 변호사 마이페이지 목록 */
    @Transactional
    public List<Consultation> listForLawyer(String lawyerUserEmail) {
        return consultationRepository.findByLawyer_User_EmailOrderByRequestDateDesc(lawyerUserEmail);
    }

    /** 유저 마이페이지 목록 */
    @Transactional
    public List<Consultation> listForUser(String requesterEmail) {
        return consultationRepository.findByRequester_EmailOrderByRequestDateDesc(requesterEmail);
    }


}
