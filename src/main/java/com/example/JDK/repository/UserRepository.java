package com.example.JDK.repository;

import com.example.JDK.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByEmailContaining(String keyword);
    void deleteByEmail(String email);
    List<User> findByEmailContainingIgnoreCase(String keyword);

}
