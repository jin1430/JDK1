package com.example.JDK.repository;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.LawyerSpecialty;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface LawyerRepository
        extends JpaRepository<Lawyer, Long>, JpaSpecificationExecutor<Lawyer> {

    Optional<Lawyer> findByUser_Id(Long userId);

    List<Lawyer> findByApprovalStatus(ApprovalStatus status);

    Optional<Lawyer> findByUser_Email(String email);

    @Query("""
           select distinct l
           from Lawyer l
           join l.specialties s
           where s = :spec and l.approvalStatus = :status
           """)
    List<Lawyer> findBySpecialtyAndStatus(@Param("spec") LawyerSpecialty spec,
                                          @Param("status") ApprovalStatus status);

    List<Lawyer> findByApprovalStatusOrderByIdDesc(com.example.JDK.ApprovalStatus status);
    boolean existsByUser_EmailAndApprovalStatus(String email, ApprovalStatus status);
    Page<Lawyer> findByApprovalStatus(ApprovalStatus status, Pageable pageable);
    boolean existsByCertificateNumber(String certificateNumber);
    long countByApprovalStatus(ApprovalStatus status); // (선택)
    Optional<Lawyer> findByUserId(Long userId);
    @Query("SELECT l FROM Lawyer l " +
            "LEFT JOIN FETCH l.user u " +                  // User 정보
            "LEFT JOIN FETCH l.specialties s " +           // 전문분야 (ManyToMany 등)
            "WHERE l.id = :id")
    Optional<Lawyer> findDetailById(@Param("id") Long id);
    Lawyer findByUser(User user);

}
