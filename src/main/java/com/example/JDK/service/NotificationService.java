package com.example.JDK.service;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.NotificationType;
import com.example.JDK.entity.*;
import com.example.JDK.repository.NotificationRepository;
import com.example.JDK.repository.PostRepository;
import com.example.JDK.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public void createForLawyerComment(Lawyer lawyer, Post post, Comment comment) {
        log.debug("[NOTI] createForLawyerComment IN postId={}, commentId={}, lawyerId={}",
                post == null ? null : post.getId(),
                comment == null ? null : comment.getId(),
                lawyer == null ? null : lawyer.getId());

        if (post == null) {
            log.warn("[NOTI] post is null. skip");
            return;
        }
        var recipient = post.getUser();
        if (recipient == null) {
            log.debug("[NOTI] post.user is null. skip");
            return;
        }

        // 자기 글 스킵 로직
        Long recipientUserId = recipient.getId();
        Long lawyerUserId = (lawyer != null && lawyer.getUser() != null) ? lawyer.getUser().getId() : null;
        if (lawyerUserId != null && recipientUserId != null && recipientUserId.equals(lawyerUserId)) {
            log.debug("[NOTI] same user (author==lawyerUser). skip");
            return;
        }

        String lawyerName = (lawyer != null && lawyer.getUser() != null
                && lawyer.getUser().getUsername() != null)
                ? lawyer.getUser().getUsername()
                : "변호사";

        String msg  = String.format("변호사 %s님이 댓글을 달았습니다.", lawyerName);
        // NotificationService.createForLawyerComment(...)
        String link = "/view/post/" + post.getId() + (comment != null ? "#c" + comment.getId() : "");
        log.info(link);
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setPost(post);
        n.setLawyer(lawyer);
        n.setType(NotificationType.LAWYER_COMMENT);
        n.setMessage(msg);
        n.setLinkUrl(link);

        log.debug("[NOTI] try save (recipientUserId={}, postId={}, lawyerId={}, msg='{}', link='{}')",
                recipientUserId, post.getId(), lawyer == null ? null : lawyer.getId(), msg, link);
        Notification saved = notificationRepository.save(n);
        log.debug("[NOTI] saved notificationId={}, createdAt={}", saved.getId(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<Notification> getMyNotifications(User me, Pageable pageable) {
        log.debug("[NOTI] getMyNotifications for userId={}, page={}", me.getId(), pageable);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(me, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User me) {
        long c = notificationRepository.countByRecipientAndReadIsFalse(me);
        log.debug("[NOTI] unreadCount userId={} => {}", me.getId(), c);
        return c;
    }

    public void markAsRead(User me, Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("알림이 없습니다."));
        if (!n.getRecipient().getId().equals(me.getId())) {
            throw new AccessDeniedException("본인 알림만 읽음 처리 가능");
        }
        n.setRead(true);
        log.debug("[NOTI] markAsRead notificationId={}, by userId={}", id, me.getId());
    }

    public int markAllAsRead(User me) {
        int updated = notificationRepository.markAllRead(me);
        log.debug("[NOTI] markAllAsRead updated={}, userId={}", updated, me.getId());
        return updated;
    }
    /** ✅ 변호사 승인 상태 변경 알림 생성 */
    public void createForLawyerApproval(Lawyer lawyer, ApprovalStatus status) {
        if (lawyer == null || lawyer.getUser() == null) {
            log.warn("[NOTI] lawyer or lawyer.user is null. skip approval notification");
            return;
        }
        User recipient = lawyer.getUser();

        String msg;
        switch (status) {
            case APPROVED -> msg = "변호사 인증이 승인되었습니다. 서비스를 이용하실 수 있어요.";
            case REJECTED -> msg = "변호사 인증이 반려되었습니다. 제출하신 정보를 확인해 주세요.";
            default       -> msg = "변호사 인증 상태가 변경되었습니다: " + status.name();
        }

        Notification n = Notification.builder()
                .recipient(recipient)
                .lawyer(lawyer)
                .type(NotificationType.LAWYER_APPROVAL)
                .message(msg)
                .linkUrl("/view/mypage") // 원하는 경로로 변경 가능
                .read(false)
                .build();

        Notification saved = notificationRepository.save(n);
        log.debug("[NOTI] approval notification saved id={}, userId={}, status={}",
                saved.getId(), recipient.getId(), status);
    }
    public void createForConsultationRequest(Consultation c) {
        if (c == null || c.getLawyer() == null || c.getLawyer().getUser() == null) return;
        User recipient = c.getLawyer().getUser();
        String msg = "새 상담 요청: " + (c.getTitle() == null ? "" : c.getTitle());
        Notification n = Notification.builder()
                .recipient(recipient)
                .lawyer(c.getLawyer())
                .type(NotificationType.CONSULTATION_REQUEST)
                .message(msg)
                .linkUrl("/view/mypage/lawyer/consultations?open=" + c.getId())
                .read(false)
                .build();
        notificationRepository.save(n);
    }
    public void createForConsultationReply(Consultation c) {
        if (c == null || c.getRequester() == null) return;
        User recipient = c.getRequester();
        String msg = "상담 답변이 도착했습니다: " + (c.getTitle() == null ? "" : c.getTitle());
        Notification n = Notification.builder()
                .recipient(recipient)
                .lawyer(c.getLawyer())
                .type(NotificationType.CONSULTATION_REPLY)
                .message(msg)
                .linkUrl("/view/mypage/consultations?open=" + c.getId())
                .read(false)
                .build();
        notificationRepository.save(n);
    }
}
