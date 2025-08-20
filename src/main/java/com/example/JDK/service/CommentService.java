// src/main/java/com/example/JDK/service/CommentService.java
package com.example.JDK.service;

import com.example.JDK.entity.Comment;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.Post;
import com.example.JDK.repository.CommentRepository;
import com.example.JDK.repository.LawyerRepository;
import com.example.JDK.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    // region 목록/상세

    // endregion

    // region 작성/수정/삭제

    // endregion

    // region 검색/기타

    // endregion


    private final CommentRepository commentRepository;
    private final LawyerRepository lawyerRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    /* =====================================================================
     * PostController와 1:1 매칭되는 시그니처
     *  - createByLawyer(postId, lawyerId, content)
     *  - updateByLawyer(postId, commentId, lawyerId, content)
     *  - deleteByLawyer(postId, commentId, lawyerId)
     * ===================================================================== */

    /** 댓글 작성 (변호사 ID 기반, 알림 포함) */
    public Comment createByLawyer(Long postId, Long lawyerId, String content) {
        log.debug("[COMMENT] createByLawyer postId={}, lawyerId={}, contentLen={}",
                postId, lawyerId, content == null ? null : content.length());

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }

        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("변호사만 댓글을 작성할 수 있습니다."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다: " + postId));

        Comment comment = new Comment();
        comment.setLawyer(lawyer);
        comment.setPost(post);
        comment.setContent(content.trim());
        comment.setGenerationDate(LocalDateTime.now());

        comment = commentRepository.save(comment);
        log.debug("[COMMENT] saved commentId={}", comment.getId());

        // 알림은 실패해도 댓글 저장에 영향 없게 try/catch
        try {
            notificationService.createForLawyerComment(lawyer, post, comment);
        } catch (Exception e) {
            log.warn("[COMMENT] notify fail postId={}, lawyerId={}, commentId={}",
                    post.getId(), lawyer.getId(), comment.getId(), e);
        }

        return comment;
    }

    /** 댓글 수정 (본인 변호사만) */
    public Comment updateByLawyer(Long postId, Long commentId, Long lawyerId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다: " + commentId));
        if (!Objects.equals(c.getPost().getId(), postId)) {
            throw new IllegalArgumentException("다른 게시글의 댓글입니다.");
        }
        if (!Objects.equals(c.getLawyer().getId(), lawyerId)) {
            throw new SecurityException("수정 권한이 없습니다.");
        }
        c.setContent(content.trim()); // Dirty Checking
        return c;
    }

    /** 댓글 삭제 (본인 변호사만) */
    public void deleteByLawyer(Long postId, Long commentId, Long lawyerId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다: " + commentId));
        if (!Objects.equals(c.getPost().getId(), postId)) {
            throw new IllegalArgumentException("다른 게시글의 댓글입니다.");
        }
        if (!Objects.equals(c.getLawyer().getId(), lawyerId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        commentRepository.delete(c);
    }

    /* =====================================================================
     * 기존(이메일 기반) 시그니처 — 하위호환용. 내부에서 위 메소드로 위임.
     * ===================================================================== */

    public Comment createComment(Long postId, String lawyerEmail, String content) {
        Lawyer lawyer = lawyerRepository.findByUser_Email(lawyerEmail)
                .orElseThrow(() -> new IllegalArgumentException("변호사만 댓글을 작성할 수 있습니다."));
        return createByLawyer(postId, lawyer.getId(), content);
    }

    public boolean editComment(Long commentId, String lawyerEmail, String newContent) {
        if (newContent == null || newContent.isBlank()) return false;
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다: " + commentId));
        Lawyer lawyer = lawyerRepository.findByUser_Email(lawyerEmail)
                .orElseThrow(() -> new IllegalArgumentException("변호사 정보가 없습니다."));
        try {
            updateByLawyer(c.getPost().getId(), commentId, lawyer.getId(), newContent);
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }

    public boolean deleteComment(Long commentId, String lawyerEmail) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다: " + commentId));
        Lawyer lawyer = lawyerRepository.findByUser_Email(lawyerEmail)
                .orElseThrow(() -> new IllegalArgumentException("변호사 정보가 없습니다."));
        try {
            deleteByLawyer(c.getPost().getId(), commentId, lawyer.getId());
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }

    /* =====================================================================
     * 조회/관리
     * ===================================================================== */

    @Transactional(readOnly = true)
    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다: " + id));
    }

    /** 관리자/테스트용 전체 목록 (fetch join) */
    @Transactional(readOnly = true)
    public List<Comment> getAllComments() {
        return commentRepository.findAllForAdmin();
    }

    /** 게시글 상세 화면용 목록 (fetch join) */
    @Transactional(readOnly = true)
    public List<Comment> findByPostForView(Long postId) {
        return commentRepository.findByPostIdForView(postId);
    }

    /** 변호사별 목록 (마이페이지용, 필요 시) */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByLawyer(Long lawyerId) {
        return commentRepository.findByLawyerIdForView(lawyerId);
    }

    /** 게시글별 댓글 리스트 (뷰용) */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPost(Post post) {
        return commentRepository.findByPost(post);
    }

    /** 변호사별 댓글 리스트 (마이페이지용) */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByLawyer(Lawyer lawyer) {
        return commentRepository.findByLawyer(lawyer);
    }

    /** 전체 개수 (대시보드) */
    @Transactional(readOnly = true)
    public long countComments() {
        return commentRepository.count();
    }

    /** ✅ 관리자 강제 삭제 (CommentApiController에서 호출) */
    public void deleteCommentByAdmin(Long id) {
        commentRepository.deleteById(id);
    }
}
