package com.example.JDK.repository;

import com.example.JDK.entity.Comment;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.Post;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost(Post post);
    List<Comment> findByLawyer(Lawyer lawyer);

    List<Comment> findByPostOrderByGenerationDateDesc(Post post);
    List<Comment> findByPostOrderByGenerationDateAsc(Post post);

    @Query("""
           select distinct c.post
           from Comment c
           where lower(c.content) like lower(concat('%', :q, '%'))
           """)
    List<Post> findPostsByCommentContent(@Param("q") String q);

    @Query("""
           select c
           from Comment c
           left join c.post p
           left join c.lawyer l
           left join l.user u
           where (:q is null or :q = ''
              or lower(c.content) like lower(concat('%', :q, '%'))
              or lower(p.title) like lower(concat('%', :q, '%'))
              or lower(u.email) like lower(concat('%', :q, '%')))
           """)
    Page<Comment> searchAdmin(@Param("q") String q, Pageable pageable);

    @Modifying
    @Transactional
    int deleteByLawyer(Lawyer lawyer);

    @Query("""
        select c from Comment c
        join fetch c.post p
        join fetch c.lawyer l
        join fetch l.user u
        order by c.id desc
    """)
    List<Comment> findAllForAdmin();

    @Query("""
        select c from Comment c
        join fetch c.lawyer l
        join fetch l.user u
        where c.post.id = :postId
        order by c.id desc
    """)
    List<Comment> findByPostIdForView(@Param("postId") Long postId);

    @Query("""
        select c from Comment c
        join fetch c.post p
        where c.lawyer.id = :lawyerId
        order by c.id desc
    """)
    List<Comment> findByLawyerIdForView(@Param("lawyerId") Long lawyerId);
    Optional<Comment> findByIdAndLawyer_User_Email(Long id, String email);


}

