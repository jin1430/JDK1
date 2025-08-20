// src/main/java/com/example/JDK/repository/PostRepository.java
package com.example.JDK.repository;

import com.example.JDK.entity.Category;
import com.example.JDK.entity.Post;
import com.example.JDK.entity.User;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByIdDesc();

    List<Post> findTop5ByOrderByGenerationDateDesc();

    // ✅ 파생쿼리 대신 JPQL로 명시: content(CLOB)는 cast(... as string) 후 lower 적용
    @Query("""
           select p
             from Post p
            where lower(p.title) like lower(concat('%', :title, '%'))
               or lower(cast(p.content as string)) like lower(concat('%', :content, '%'))
           """)
    List<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            @Param("title") String title,
            @Param("content") String content
    );

    List<Post> findByUser(User user);

    @Query("select p from Post p where p.user.id = :userId")
    List<Post> findByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Post p set p.user = null where p.user = :user")
    int detachUserFromPosts(@Param("user") User user);

    @Query("""
        select p from Post p
        join fetch p.user u
        left join fetch p.category c
        where p.id = :id
    """)
    Optional<Post> findDetail(@Param("id") Long id);

    List<Post> findAllByOrderByGenerationDateDesc();
    List<Post> findTop4ByCategoryOrderByGenerationDateDesc(Category category);
    List<Post> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword);
}
