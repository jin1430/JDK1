package com.example.JDK.controller.view;


// Refactor: categorised as view controller; moved for structure-only readability.

import com.example.JDK.LawyerSpecialty;
import com.example.JDK.Role;
import com.example.JDK.entity.Lawyer;
import com.example.JDK.entity.User;
import com.example.JDK.service.ConsultationService;
import com.example.JDK.service.LawyerService;
import com.example.JDK.service.PostService;
import com.example.JDK.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Controller
@RequiredArgsConstructor
public class LawyerViewController {

    private final LawyerService lawyerService;
    private final UserService userService; // ✅ 상세화면의 canEdit 계산용
    private final PostService postService;
    private final ConsultationService consultationService;
    // =========================
    // 변호사 목록
    // =========================
    @GetMapping("/view/lawyers")
    public String listLawyers(@RequestParam(required = false) String q,
                              @RequestParam(required = false) String region,
                              @RequestParam(required = false, name = "specialty") List<String> specialtyCodes,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              Model model) {

        // 1) 필터바: 전문분야 목록(체크 상태 포함)
        var specialties = Arrays.stream(LawyerSpecialty.values())
                .map(s -> new SpecialtyVM(s.name(), s.getKoreanName()))
                .collect(Collectors.toList());
        model.addAttribute("specialties",
                specialties.stream().map(vm -> new Object() {
                    public final String code = vm.code();
                    public final String label = vm.label();
                    public final boolean isSelected = specialtyCodes != null && specialtyCodes.contains(vm.code());
                }).toList()
        );

        // 2) 파라미터 파싱: String -> Enum
        List<LawyerSpecialty> selectedSpecs =
                (specialtyCodes == null) ? List.of()
                        : specialtyCodes.stream().map(code -> {
                    try { return LawyerSpecialty.valueOf(code); }
                    catch (Exception e) { return null; }
                }).filter(Objects::nonNull).toList();

        // 3) 검색 실행
        Page<Lawyer> pageResult = lawyerService.searchLawyers(q, region, selectedSpecs, page, size);
        model.addAttribute("lawyersPage", pageResult); // 필요 시 페이징 컴포넌트에서 사용

        // 4) 카드 렌더링용 데이터
        List<Map<String, Object>> lawyerCards = pageResult.getContent().stream()
                .map(l -> {
                    Map<String, Object> m = new HashMap<>();
                    User u = l.getUser();
                    m.put("id", l.getId());
                    m.put("detailUrl", "/view/lawyers/" + l.getId());   // ✅ 상세 링크
                    m.put("name", resolveDisplayName(u));                // 안전한 이름
                    m.put("region", resolveString(u, "region"));         // 존재하면 표시
                    log.info((resolveString(u, "region")));
                    m.put("certificateNumber", l.getCertificateNumber());
                    m.put("specialtyKoreanNames", l.getSpecialtyKoreanNames()); // List<String>
                    m.put("categoryKey", l.getCategoryKey());
                   //log.info(l.getSpecialtyKoreanNames().toString());
                    m.put("profileImageUrl", safe(l.getProfileImage()));      // ✅ 썸네일 표시용
                    return m;
                }).toList();
        model.addAttribute("lawyers", lawyerCards);

        // 5) 페이지네이션 모델
        int current = pageResult.getNumber();
        int total = pageResult.getTotalPages();
        List<Map<String, Object>> pages = new ArrayList<>();
        int start = Math.max(0, current - 2);
        int end = Math.min(total - 1, current + 2);
        for (int i = start; i <= end; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("display", i + 1);
            m.put("active", i == current);
            pages.add(m);
        }
        model.addAttribute("pages", pages);
        model.addAttribute("prevPage", Math.max(0, current - 1));
        model.addAttribute("nextPage", Math.min(total - 1, current + 1));

        // 6) 쿼리 문자열(페이지 제외) - 페이지 링크에서 재사용
        String queryNoPage = buildQueryWithoutPage(q, region, specialtyCodes, size);
        model.addAttribute("query_no_page", queryNoPage);

        // 7) 검색창 값 유지
        model.addAttribute("q", (q == null) ? "" : q);
        model.addAttribute("region", (region == null) ? "" : region);
        model.addAttribute("size_is_12", size == 12);
        model.addAttribute("size_is_24", size == 24);
        model.addAttribute("size_is_48", size == 48);

        return "lawyer/list";
    }

    // =========================
    // 변호사 상세 (프로필)
    // =========================
    @GetMapping("/view/lawyers/{id}")
    public String lawyerDetail(@PathVariable Long id,
                               @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
                               Model model) {

        Lawyer lawyer = lawyerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("변호사를 찾을 수 없습니다."));
        boolean canEdit = false;
        boolean canConsult = false;
        if (auth != null) {
            User me = userService.findByEmail(auth.getUsername());
            if (me != null && lawyer.getUser() != null) {
                boolean isOwner = java.util.Objects.equals(lawyer.getUser().getId(), me.getId());
                boolean isAdmin = me.getRole() == Role.ADMIN;
                canEdit = isOwner || isAdmin;
                canConsult = !isOwner && me.getRole() == Role.MEMBER; // 일반 회원만 상담 가능
            }
        }
        if (lawyer.getProfileImage() != null && !lawyer.getProfileImage().isEmpty()) {
            model.addAttribute("profileImageUrl", lawyer.getProfileImage());
        }
        log.info(lawyer.toString());
        model.addAttribute("lawyer", lawyer);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("canConsult", canConsult);

        return "lawyer/detail";
    }

    // ----- helpers -----
    private String resolveDisplayName(User u) {
        if (u == null) return "";
        String[] order = {"name", "realName", "fullName", "username", "nickname", "email"};
        for (String f : order) {
            String v = resolveString(u, f);
            if (v != null && !v.isBlank()) {
                if ("email".equals(f)) {
                    int at = v.indexOf('@');
                    return (at > 0) ? v.substring(0, at) : v;
                }
                return v;
            }
        }
        return "";
    }

    private String resolveString(User u, String field) {
        try {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            Method m = u.getClass().getMethod(getter);
            Object val = m.invoke(u);
            return (val == null) ? "" : String.valueOf(val);
        } catch (Exception e) {
            return ""; // 필드가 없으면 빈 문자열
        }
    }

    private String buildQueryWithoutPage(String q, String region, List<String> specialtyCodes, int size) {
        List<String> params = new ArrayList<>();
        if (q != null && !q.isBlank()) params.add("q=" + urlEncode(q));
        if (region != null && !region.isBlank()) params.add("region=" + urlEncode(region));
        if (specialtyCodes != null) {
            for (String s : specialtyCodes) params.add("specialty=" + urlEncode(s));
        }
        params.add("size=" + size);
        return String.join("&", params);
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private record SpecialtyVM(String code, String label) { }

    // ─────────────────────────────────────────────
    // (A) 프로필 사진 업로드
    // ─────────────────────────────────────────────
    @PostMapping("/view/lawyers/{id}/photo")
    public String uploadPhoto(@PathVariable Long id,
                              @RequestParam("profileImage") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            // 1) 실행 중에도 쓸 수 있는 외부 저장 경로 지정
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            String fileName = "lawyer-" + id + "-" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, fileName);

            // 2) 디렉토리 생성 후 저장
            Files.createDirectories(path.getParent());
            file.transferTo(path.toFile());

            // 3) DB 업데이트 (뷰에서 접근할 경로만 저장)
            String imageUrl = "/uploads/" + fileName;
            lawyerService.updateProfileImage(id, imageUrl);
        }

        return "redirect:/view/lawyers/" + id;
    }

    // ─────────────────────────────────────────────
    // (B) 정보 수정 폼
    // ─────────────────────────────────────────────

    // ✅ 프로필 수정 화면
    @GetMapping("/view/lawyers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Lawyer lawyer = lawyerService.getLawyerById(id);

        // null-safe (뷰 깨짐 방지)
        if (lawyer.getTitle() == null) lawyer.setTitle("");
        if (lawyer.getBio() == null) lawyer.setBio("");
        if (lawyer.getOffice() == null) lawyer.setOffice("");
        if (lawyer.getCareer() == null) lawyer.setCareer("");
        if (lawyer.getSpecialty() == null) lawyer.setSpecialty("");

        model.addAttribute("lawyer", lawyer);
        return "lawyer/edit";
    }
    // ─────────────────────────────────────────────
// (C) 정보 수정 저장
// ─────────────────────────────────────────────
    // ✅ 프로필 수정 처리
    @PostMapping("/view/lawyers/{id}/edit")
    public String updateProfile(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String office,
                                @RequestParam(required = false) String career,
                                @RequestParam(required = false) String specialty,
                                @RequestParam(required = false) MultipartFile profileImage
    ) throws IOException {

        Lawyer lawyer = lawyerService.getLawyerById(id);

        // 이름은 User.username에 반영
        if (lawyer.getUser() != null) {
            lawyer.getUser().setUsername(name);
        }

        lawyer.setTitle(title);
        lawyer.setBio(bio);
        lawyer.setOffice(office);
        lawyer.setCareer(career);
        lawyer.setSpecialty(specialty);

        // 프로필 사진 업로드
        if (profileImage != null && !profileImage.isEmpty()) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            String fileName = "lawyer-" + id + "-" + profileImage.getOriginalFilename();
            Path path = Paths.get(uploadDir, fileName);
            profileImage.transferTo(path.toFile());

            String imageUrl = "/uploads/" + fileName;
            lawyerService.updateProfileImage(id, imageUrl);
        }

        return "redirect:/view/lawyers/" + id;
    }
//    // 상담 요청 (프로필 페이지의 폼 action="/view/lawyers/{{id}}/consult")
//    @PostMapping("/view/lawyers/{id}/consult")
//    public String requestConsult(@PathVariable Long id,
//                                 @AuthenticationPrincipal org.springframework.security.core.userdetails.User auth,
//                                 @RequestParam String title,
//                                 @RequestParam String content,
//                                 RedirectAttributes ra) {
//        if (auth == null) return "redirect:/view/login?continue=/view/lawyers/" + id;
//
//        Lawyer lawyer = lawyerService.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("변호사를 찾을 수 없습니다."));
//        User me = userService.findByEmail(auth.getUsername());
//        // 본인은 불가, 일반 회원만 가능
//        boolean isOwner = (lawyer.getUser() != null) && Objects.equals(lawyer.getUser().getId(), me.getId());
//        if (isOwner || me.getRole() != Role.MEMBER) {
//            ra.addFlashAttribute("error", "일반 회원만 상담을 요청할 수 있습니다.");
//            return "redirect:/view/lawyers/" + id;
//        }
//
//        // 서비스에 위임 (메서드명은 프로젝트에 맞게 조정)
//        consultationService.create(me.getId(), me.getEmail(), title, content);
//        ra.addFlashAttribute("msg", "상담 요청이 접수되었습니다.");
//        return "redirect:/view/lawyers/" + id;
//    }
    private boolean canEditLawyer(Lawyer lawyer, User me) {
        if (lawyer == null || me == null) return false;
        boolean isOwner = (lawyer.getUser() != null) && Objects.equals(lawyer.getUser().getId(), me.getId());
        boolean isAdmin = me.getRole() == Role.ADMIN;
        return isOwner || isAdmin;
    }
    private String toPublicUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) return null;

        // ✅ 외부 URL이면 그대로 반환
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }

        String s = pathOrUrl.replace("\\", "/");

        // ✅ 정적 리소스(static) 경로 처리
        // resources/static/images.lawyers/ → /images.lawyers/~
        if (s.startsWith("images.") || s.startsWith("/images.")) {
            if (!s.startsWith("/")) s = "/" + s;
            return s;
        }

        // ✅ 업로드(/uploads/) 경로 처리
        if (s.startsWith("/uploads/")) return s;
        if (s.startsWith("uploads/")) return "/" + s;

        // ✅ fallback: 업로드 기본 처리
        return "/uploads/" + s.replaceFirst("^/+", "");
    }

    // 실제 파일 저장: /uploads/<subdir>/uuid___.ext  -> "/uploads/<subdir>/uuid___.ext" 반환
    private String storeImageToUploads(MultipartFile file, String subdir) {
        try {
            String base = System.getProperty("user.dir");
            Path dir = Paths.get(base, "uploads", subdir);
            Files.createDirectories(dir);
            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .filter(n -> n.contains("."))
                    .map(n -> n.substring(n.lastIndexOf('.'))).orElse("");
            String name = java.util.UUID.randomUUID() + ext;
            Path dest = dir.resolve(name);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + subdir + "/" + name;
        } catch (Exception e) {
            throw new RuntimeException("이미지 저장 실패", e);
        }
    }
}
