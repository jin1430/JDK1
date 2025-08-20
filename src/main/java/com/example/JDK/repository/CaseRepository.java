package com.example.JDK.repository;

import com.example.JDK.entity.Case;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CaseRepository extends JpaRepository<Case, Long> {
    List<Case> findTop5ByCategoryOrderByCreatedAtDesc(String category);
}
