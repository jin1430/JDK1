package com.example.JDK.repository;

import com.example.JDK.entity.Report;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("""
        select r from Report r
        left join fetch r.post p
        left join fetch r.comment c
        left join fetch r.reporter rep
        order by r.id desc
    """)
    List<Report> findAllForAdmin();
}
