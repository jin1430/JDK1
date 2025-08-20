// src/main/java/com/example/JDK/repository/ConsultationRepository.java
package com.example.JDK.repository;

import com.example.JDK.entity.Consultation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    // 변호사 이메일(=Lawyer.user.email)로 조회
    List<Consultation> findByLawyer_User_EmailOrderByRequestDateDesc(String email);

    // 요청자 이메일로 조회
    List<Consultation> findByRequester_EmailOrderByRequestDateDesc(String email);
}
