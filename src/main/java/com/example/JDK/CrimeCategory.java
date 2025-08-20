package com.example.JDK;

import lombok.Getter;

import java.util.Locale;
import java.util.Set;
@Getter
public enum CrimeCategory {
    SEXUAL_CRIME("성범죄"),
    ASSAULT_THREATS("폭행/협박"),
    DEFAMATION("명예훼손/모욕"),
    PROPERTY_CRIME("재산범죄"),
    TRAFFIC_CRIME("교통사고/범죄"),
    CRIMINAL_PROCEDURE("형사절차"),
    REAL_ESTATE("부동산/임대차"),
    FAMILY("가족"),
    CORPORATE("회사"),
    MEDICAL_TAX("의료/세금/행정"),
    // ✅ 추가한 카테고리들
    ECONOMY("경제"),
    FINANCE("금융"),
    HOMICIDE("살인");

    private final String koreanName;
    private final Set<String> aliases;

    CrimeCategory(String koreanName) {
        this(koreanName, Set.of(koreanName));
    }
    CrimeCategory(String koreanName, Set<String> aliases) {
        this.koreanName = koreanName;
        this.aliases = aliases;
    }

    /** "성범죄", "폭행", "경제" 등 들어오면 가장 잘 맞는 enum으로 매핑 */
    public static CrimeCategory fromLabel(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        String lower = s.toLowerCase(Locale.ROOT);

        for (CrimeCategory c : values()) {
            if (c.koreanName.equals(s)) return c;               // 정확 일치(한글 라벨)
            if (c.aliases.contains(s) || c.aliases.contains(lower)) return c; // 별칭 매칭
        }
        return null; // 못 찾으면 null 반환(서비스단에서 기본값 처리)
    }
}
