package com.example.JDK.service;

import com.example.JDK.Role;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.Post;
import com.example.JDK.entity.User;
import com.example.JDK.repository.LawyerRepository;
import com.example.JDK.repository.PostRepository;
import com.example.JDK.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final LawyerRepository lawyerRepository;

    // ✅ CREATE - 회원 가입
    public User registerMember(String email, String rawPassword, String username,
                               String address, LocalDate birthday) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(rawPassword);
        user.setUsername(username);
        user.setAddress(address);
        user.setBirthday(birthday);
        user.setRole(Role.MEMBER); // com.example.JDK.Role

        user.encryptPassword(passwordEncoder);
        return userRepository.save(user);
    }
    public User registerLawyer(String email, String rawPassword, String username,
                               String address, LocalDate birthday, String certificateNumber) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        if (certificateNumber == null || certificateNumber.isBlank()) {
            throw new IllegalArgumentException("자격증 번호가 필요합니다.");
        }
        if (lawyerRepository.existsByCertificateNumber(certificateNumber)) {
            throw new IllegalArgumentException("이미 등록된 자격증 번호입니다.");
        }

        // 1) User 생성/저장
        User user = new User();
        user.setEmail(email);
        user.setPassword(rawPassword);
        user.setUsername(username);
        user.setAddress(address);
        user.setBirthday(birthday);
        user.setRole(Role.LAWYER); // com.example.JDK.Role
        user.encryptPassword(passwordEncoder);
        userRepository.save(user);

        // 2) Lawyer 생성/저장 (주인: Lawyer.user)
        Lawyer lawyer = Lawyer.builder()
                .user(user)
                .certificateNumber(certificateNumber)
                // approvalStatus 기본값 PENDING (Builder.Default)
                .build();
        lawyerRepository.save(lawyer);

        // 3) 양방향 세팅 (cascade/orphanRemoval 일관성)
        user.setLawyer(lawyer);

        return user;
    }
    // ✅ 로그인 기능 추가
    public boolean login(String email, String rawPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return passwordEncoder.matches(rawPassword, user.getPassword());
        }
        return false;
    }

    // ✅ ID로 사용자 조회
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // ✅ 전체 사용자 조회
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ✅ 사용자 정보 수정
    public User updateUser(Long id, User updatedUser) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setUsername(updatedUser.getUsername());
//            user.setNickname(updatedUser.getNickname());
            user.setAddress(updatedUser.getAddress());
            user.setBirthday(updatedUser.getBirthday());
            user.setRole(updatedUser.getRole()); // 역할까지 수정 가능하게!
            return user;
        }
        return null;
    }

    // ✅ 사용자 삭제
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public long countUsers() {
        return userRepository.count();
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자가 없습니다: " + email));
    }
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> searchByEmail(String keyword) {
        return userRepository.findByEmailContaining(keyword);
    }

    @Transactional
    public void deleteById(Long id) {
        // 해당 유저가 작성한 게시글 먼저 삭제
        List<Post> posts = postRepository.findByUserId(id);
        postRepository.deleteAll(posts);

        // 이후 유저 삭제
        userRepository.deleteById(id);
    }
    @Transactional
    public void updateUserFields(Long id, String username, String address, Role role) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + id));
        if (username != null) u.setUsername(username.trim());
        if (address  != null) u.setAddress(address.trim());
        if (role     != null) u.setRole(role);
    }
}
