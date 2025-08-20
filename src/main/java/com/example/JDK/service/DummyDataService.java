package com.example.JDK.service;

import com.example.JDK.ApprovalStatus;
import com.example.JDK.Role;
import com.example.JDK.LawyerSpecialty;
import com.example.JDK.entity.*;
import com.example.JDK.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DummyDataService {

    private final UserRepository userRepository;
    private final LawyerRepository lawyerRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationRepository notificationRepository;
    private final ConsultationRepository consultationRepository;
    private final ThreadLocalRandom R = ThreadLocalRandom.current();

    /* ──────────────────────────────────────────────────────────────
       실행 시 DB 초기화 + 대량 시드
       ────────────────────────────────────────────────────────────── */
    @Transactional
    public String resetAndSeed() {
        // 자식 → 부모 순서로 삭제 (FK 위반 방지)
        notificationRepository.deleteAllInBatch();
        reportRepository.deleteAllInBatch();
        commentRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        consultationRepository.deleteAllInBatch();     // 변호사 상담
        lawyerRepository.deleteAllInBatch();

        // 이제 category를 참조하는 애들 다 없어졌으므로 안전하게 삭제 가능
        categoryRepository.deleteAllInBatch();

        // 마지막에 사용자
        userRepository.deleteAllInBatch();

        // 관리자
        User admin = new User();
        admin.setEmail("admin@demo.kr");
        admin.setPassword(passwordEncoder.encode("pass"));
        admin.setUsername("관리자");
        admin.setAddress("서울특별시 종로구 청와대로 1");
        admin.setBirthday(LocalDate.of(1985, 1, 1));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        /* 1) 일반 회원 10명 */
        List<String> surnames = List.of("김","이","박","최","정","조","윤","장","임","한","오","서","신","권","황","송");
        List<String> givenL   = List.of("민","서","지","하","도","규","재","현","가","유","승","시","연","다","아","한","소");
        List<String> givenR   = List.of("준","연","윤","빈","민","현","율","주","경","우","환","혁","후","림","경","솔","찬");

        List<User> members = IntStream.rangeClosed(1, 10).mapToObj(i -> {
            String name = surnames.get(i % surnames.size())
                    + givenL.get((i + 2) % givenL.size())
                    + givenR.get((i + 5) % givenR.size());
            User u = new User();
            u.setEmail("member%02d@demo.kr".formatted(i));
            u.setPassword(passwordEncoder.encode("pass"));
            u.setUsername(name);
            u.setAddress("서울시 " + (10 + i) + "구");
            u.setBirthday(LocalDate.of(1990 + i, (i % 12) + 1, (i % 27) + 1));
            u.setRole(Role.MEMBER);
            return u;
        }).collect(Collectors.toList());
        userRepository.saveAll(members);

        /* 2) 변호사 유저 20명 + Lawyer 프로필 */
        List<String> offices = List.of(
                "로앤파트너스","한결법률사무소","신뢰법률센터","바른길법률","정도법무그룹",
                "세이프로펌","온앤온법률","백송법률사무소","정담로펌","라이트로펌"
        );

        // 검색/탭과 맞출 10개 전문분야(한글)
        List<String> korSpecs = List.of(
                "형사","민사","부동산","가사","회사법",
                "세무","행정","노동","지식재산권","국제법"
        );

        // 한글 -> Enum 매핑
        Map<String, LawyerSpecialty> K2E = Map.ofEntries(
                Map.entry("형사", LawyerSpecialty.CRIMINAL),
                Map.entry("민사", LawyerSpecialty.CIVIL),
                Map.entry("부동산", LawyerSpecialty.REAL_ESTATE),
                Map.entry("가사", LawyerSpecialty.FAMILY),
                Map.entry("회사법", LawyerSpecialty.CORPORATE),
                Map.entry("세무", LawyerSpecialty.TAX),
                Map.entry("행정", LawyerSpecialty.ADMINISTRATIVE),
                Map.entry("노동", LawyerSpecialty.LABOR),
                Map.entry("지식재산권", LawyerSpecialty.INTELLECTUAL_PROPERTY),
                Map.entry("국제법", LawyerSpecialty.INTERNATIONAL)
        );

        List<LawyerSpecialty> allEnums = List.of(
                LawyerSpecialty.CRIMINAL, LawyerSpecialty.CIVIL, LawyerSpecialty.REAL_ESTATE,
                LawyerSpecialty.FAMILY, LawyerSpecialty.CORPORATE, LawyerSpecialty.TAX,
                LawyerSpecialty.ADMINISTRATIVE, LawyerSpecialty.LABOR,
                LawyerSpecialty.INTELLECTUAL_PROPERTY, LawyerSpecialty.INTERNATIONAL
        );

        List<User> lawyerUsers = IntStream.rangeClosed(1, 20).mapToObj(i -> {
            String name = surnames.get((i + 1) % surnames.size())
                    + givenL.get((i + 3) % givenL.size())
                    + givenR.get((i + 7) % givenR.size());
            User u = new User();
            u.setEmail("lawyer%02d@demo.kr".formatted(i));
            u.setPassword(passwordEncoder.encode("pass"));
            u.setUsername(name);
            u.setAddress("부산시 " + (i % 5 + 1) + "구");
            u.setBirthday(LocalDate.of(1980 + (i % 10), (i % 12) + 1, (i % 27) + 1));
            u.setRole(Role.LAWYER);
            return u;
        }).collect(Collectors.toList());
        userRepository.saveAll(lawyerUsers);

        List<Lawyer> lawyers = IntStream.range(0, lawyerUsers.size()).mapToObj(i -> {
            User u = lawyerUsers.get(i);

            // 주력 한글 분야 + 최소 1~최대 3개의 Enum 세트 채우기
            String mainKor = korSpecs.get(i % korSpecs.size());
            Set<LawyerSpecialty> set = new HashSet<>();
            LawyerSpecialty mainEnum = K2E.get(mainKor);
            if (mainEnum != null) set.add(mainEnum);

            int extra = R.nextInt(0, 2); // 0~1개 추가
            while (set.size() < 1 + extra) {
                set.add(allEnums.get(R.nextInt(allEnums.size())));
            }

            return Lawyer.builder()
                    .user(u)
                    .certificateNumber("CERT-%04d".formatted(1000 + i))
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .title(mainKor + " 전문 변호사 " + u.getUsername())
                    .bio("주요 업무: " + mainKor + ". 초기 상담부터 수사/소송 전 과정 밀착 대응.")
                    .office(offices.get(i % offices.size()))
                    .career("해당 분야 실무 10년 이상 경력")
                    .specialty(mainKor) // 화면 표시용 한글
                    .profileImage("/images/lawyers/lawyer%02d.png".formatted(i + 1))
                    .specialties(set)   // ✅ 체크박스 검색용 Enum 세트
                    .build();
        }).collect(Collectors.toList());
        lawyerRepository.saveAll(lawyers);

        /* 3) 게시글 카테고리(드롭다운과 동일한 명칭) */
        List<String> postCategories = List.of(
                "성범죄","폭행/협박","명예훼손/모욕","재산범죄","교통사고/범죄",
                "형사절차","부동산/임대차","가족","회사","의료/세금/행정",
                "경제","금융","살인"
        );

        List<Category> categoryEntities = postCategories.stream().map(n -> {
            Category c = new Category();
            c.setName(n);
            return c;
        }).collect(Collectors.toList());
        categoryRepository.saveAll(categoryEntities);

        Map<String, Category> categoryMap = categoryEntities.stream()
                .collect(Collectors.toMap(Category::getName, c -> c));

        /* 4) 카테고리별 제목/문구 시드 */
        Map<String, List<String>> titleSeeds = new HashMap<>();
        titleSeeds.put("성범죄", List.of("데이트폭력 대응","불법촬영물 삭제","직장 내 성희롱","강제추행 처벌수위","위장수사 사례"));
        titleSeeds.put("폭행/협박", List.of("상해죄 합의","특수폭행 쟁점","가정폭력 임시조치","협박죄 성립요건","정당방위 여부"));
        titleSeeds.put("명예훼손/모욕", List.of("댓글 명예훼손","카톡 단체방 모욕","사실적시와 위법성조각","게시글 삭제요청","초상권/퍼블리시티"));
        titleSeeds.put("재산범죄", List.of("절도 초범 선처","사기 고소 진행","횡령/배임 대응","점유이탈물 횡령","전기통신금융 사기"));
        titleSeeds.put("교통사고/범죄", List.of("음주운전 초범/재범","윤창호법 쟁점","무면허/뺑소니","과실비율 분쟁","보험사 합의 전략"));
        titleSeeds.put("형사절차", List.of("피의자 조사 준비","압수수색 대응","영장실질심사","구속/보석","약식명령/정식재판"));
        titleSeeds.put("부동산/임대차", List.of("전세사기 대처","보증금 반환","상가권리금","명도소송","재계약 분쟁"));
        titleSeeds.put("가족", List.of("재산분할","양육권/면접교섭","위자료 산정","상간자 소송","친권변경"));
        titleSeeds.put("회사", List.of("주주간계약 분쟁","이사회/주주총회","대표이사 책임","지배구조","M&A 쟁점"));
        titleSeeds.put("의료/세금/행정", List.of("의료과실 입증","세무조사 대응","조세불복","영업정지 취소","행정심판/소송"));
        titleSeeds.put("경제", List.of("하자담보 책임","대금 청구","손해배상","계약해지","가압류/가처분"));
        titleSeeds.put("금융", List.of("P2P 피해구제","펀드 불완전판매","대출 미상환","코인 분쟁","전매제한"));
        titleSeeds.put("살인", List.of("정당방위 판단","우발적 범행 양형","교사/방조 책임","심신미약 감경","증거보전"));

        Map<String, List<String>> tips = new HashMap<>();
        tips.put("성범죄", List.of("초기 진술 일관성","증거물 보존","접근금지 신청"));
        tips.put("폭행/협박", List.of("진단서 확보","합의 시도","현장증거 수집"));
        tips.put("명예훼손/모욕", List.of("캡처/URL 보존","반박자료 준비","임시조치 병행"));
        tips.put("재산범죄", List.of("계좌내역 정리","피해액 특정","합의전략"));
        tips.put("교통사고/범죄", List.of("블랙박스/현장사진","보험 통지","음주측정 적법성"));
        tips.put("형사절차", List.of("출석 전 진술정리","변호인 참여","영장 대응"));
        tips.put("부동산/임대차", List.of("특약 확인","확정일자/전입","내용증명"));
        tips.put("가족", List.of("양육환경 기록","재산내역 목록화","협의서 초안"));
        tips.put("회사", List.of("의사록/규정 점검","대표 책임범위","지분구조 분석"));
        tips.put("의료/세금/행정", List.of("의무기록 열람","세무리스크 파악","행정불복 기한유지"));
        tips.put("경제", List.of("계약서 재검토","증빙수집","집행가능성 검토"));
        tips.put("금융", List.of("거래내역 보존","분쟁조정 신청","가압류"));
        tips.put("살인", List.of("객관증거 확보","정상참작 사유 정리","초동대응 중요"));

        List<String> regions = List.of("서울","경기","부산","대전","대구","광주","인천","울산","세종","제주");

        /* 5) 게시글: 카테고리별 20개 */
        List<Post> posts = new ArrayList<>();
        for (String cat : postCategories) {
            Category category = categoryMap.get(cat);
            List<String> seeds = titleSeeds.getOrDefault(cat, List.of(cat + " 사례 문의"));
            for (int i = 1; i <= 20; i++) {
                String base = seeds.get(i % seeds.size());
                String region = regions.get((i + cat.length()) % regions.size());
                String title = "[%s] %s - %s 사례 문의 #%d".formatted(cat, base, region, i);

                User writer = (i % 7 == 0)
                        ? lawyerUsers.get(i % lawyerUsers.size())
                        : members.get(i % members.size());

                String tipLine = String.join(" · ",
                        tips.getOrDefault(cat, List.of("초동대응 중요")));
                String content = """
                        카테고리: %s
                        상황: %s 이슈가 %s 지역에서 발생.
                        요청: 절차/기간/비용/합의 가능성 및 필요서류 문의.
                        참고: %s
                        """.formatted(cat, base, region, tipLine);

                posts.add(Post.builder()
                        .title(title)
                        .content(content)
                        .user(writer)
                        .category(category)
                        .views((long) R.nextInt(0, 500))
                        .generationDate(LocalDateTime.now().minusDays(R.nextInt(0, 30)))
                        .build());
            }
        }
        postRepository.saveAll(posts);

        /* 6) 댓글: 모든 댓글은 변호사 작성, 글마다 5개 */
        List<Comment> comments = new ArrayList<>();
        for (Post p : posts) {
            String cat = p.getCategory().getName();
            LawyerSpecialty specEnum = mapPostCategoryToEnum(cat);

            // 해당 전문 변호사 우선 매칭 (없으면 전체에서)
            List<Lawyer> domainLawyers = (specEnum == null)
                    ? lawyers
                    : lawyers.stream()
                    .filter(l -> l.getSpecialties() != null && l.getSpecialties().contains(specEnum))
                    .collect(Collectors.toList());
            if (domainLawyers.isEmpty()) domainLawyers = lawyers;

            for (int j = 1; j <= 5; j++) {
                Lawyer picked = domainLawyers.get(R.nextInt(domainLawyers.size()));
                Comment c = new Comment();
                c.setPost(p);
                c.setLawyer(picked);
                c.setGenerationDate(LocalDateTime.now().minusHours(R.nextInt(1, 96)));

                List<String> tipList = tips.getOrDefault(cat, List.of("사안 검토"));
                String tip = tipList.get(R.nextInt(tipList.size()));

                String line = switch (j % 5) {
                    case 1 -> "안녕하세요, %s 변호사입니다. 본 사안은 핵심 쟁점 정리가 중요합니다. (%s)";
                    case 2 -> "질문 주신 내용은 저희 분야입니다. 초기에는 '%s'를 우선 진행해 보시길 권합니다.";
                    case 3 -> "유사 판례상 '%s' 포인트가 자주 다투어집니다. 세부 자료 확인 후 전략 제시 가능합니다.";
                    case 4 -> "관할/절차에 따라 결과가 달라질 수 있습니다. 관련 서류를 먼저 확보해 주세요. (예: %s)";
                    default -> "추가 상담 시 구체 일정과 비용 범위를 안내드리겠습니다. '%s'도 병행하면 좋습니다.";
                };
                c.setContent(line.formatted(picked.getUser().getUsername(), tip));
                comments.add(c);
            }
        }
        commentRepository.saveAll(comments);

        /* 7) 신고 5건 */
        List<Report> reports = new ArrayList<>();
        List<String> reasons = List.of("광고성 내용 의심","비방·모욕 표현","허위 사실 우려","개인정보 노출","중복 게시");
        for (int i = 0; i < 5; i++) {
            Report r = new Report();
            r.setReporter(members.get(R.nextInt(members.size())));
            r.setReason(reasons.get(i));
            if (i % 2 == 0) r.setPost(posts.get(R.nextInt(posts.size())));
            else            r.setComment(comments.get(R.nextInt(comments.size())));
            reports.add(r);
        }
        reportRepository.saveAll(reports);

        return "✅ 시드 완료: members=" + members.size()
                + ", lawyers=" + lawyers.size()
                + ", categories=" + categoryEntities.size()
                + ", posts=" + posts.size()
                + ", comments=" + comments.size()
                + ", reports=" + reports.size();
    }

    /* 게시글 카테고리(한글) → 변호사 전문분야(Enum) */
    private LawyerSpecialty mapPostCategoryToEnum(String cat) {
        return switch (cat) {
            case "성범죄", "폭행/협박", "명예훼손/모욕", "재산범죄", "교통사고/범죄", "형사절차", "살인"
                    -> LawyerSpecialty.CRIMINAL;
            case "부동산/임대차" -> LawyerSpecialty.REAL_ESTATE;
            case "가족" -> LawyerSpecialty.FAMILY;
            case "회사" -> LawyerSpecialty.CORPORATE;
            case "의료/세금/행정" -> LawyerSpecialty.ADMINISTRATIVE;
            case "금융" -> LawyerSpecialty.TAX;
            case "경제" -> LawyerSpecialty.CIVIL;
            default -> null;
        };
    }
}
