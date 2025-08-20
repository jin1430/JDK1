package com.example.JDK;

import lombok.Getter;
@Getter
public enum LawyerSpecialty {
    CRIMINAL("형사"),
    CIVIL("민사"),
    REAL_ESTATE("부동산"),
    FAMILY("가사"),
    CORPORATE("회사법"),
    TAX("세무"),
    ADMINISTRATIVE("행정"),
    LABOR("노동"),
    INTELLECTUAL_PROPERTY("지식재산권"),
    INTERNATIONAL("국제법");

    private final String koreanName;

    LawyerSpecialty(String koreanName) {
        this.koreanName = koreanName;
    }
}
