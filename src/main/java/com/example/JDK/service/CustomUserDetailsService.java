package com.example.JDK.service;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.entity.User;
import com.example.JDK.repository.LawyerRepository;
import com.example.JDK.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final LawyerRepository lawyerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("not found: " + username));

        List<GrantedAuthority> auths = new ArrayList<>();
        // 기본 롤 (예: USER/ADMIN)
        auths.add(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));

        // 템플릿에서 ROLE_MEMBER를 쓰고 있다면, USER에게 MEMBER도 같이 부여
        if ("USER".equalsIgnoreCase(u.getRole().name())) {
            auths.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }

        // 변호사 프로필이 '승인' 상태면 ROLE_LAWYER 부여
        boolean approvedLawyer = lawyerRepository
                .existsByUser_EmailAndApprovalStatus(username, ApprovalStatus.APPROVED);
        if (approvedLawyer) {
            auths.add(new SimpleGrantedAuthority("ROLE_LAWYER"));
        }

        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getPassword(),
                auths
        );
    }
}
