package com.ssa.lms.course.service.screening;

/**
 * 훈련생 선발 적합도 등급 — 총점(100점 만점) 비율에 따른 4단계.
 *
 * <p>구간·색상은 화면 배지와 1:1로 대응한다 (enrollment-approval.html 의 .grade-* 클래스).</p>
 */
public enum SuitabilityGrade {

    VERY_SUITABLE("아주 적합", "very", 90),
    SUITABLE("적합", "ok", 75),
    UNSUITABLE("미적합", "warn", 60),
    NOT_SUITABLE("부적합", "bad", 0);

    private final String label;
    /** 화면 배지 CSS 접미사 (.grade-very / .grade-ok / .grade-warn / .grade-bad) */
    private final String cssSuffix;
    /** 이 등급이 되기 위한 최소 적합도(%) */
    private final int minPercent;

    SuitabilityGrade(String label, String cssSuffix, int minPercent) {
        this.label = label;
        this.cssSuffix = cssSuffix;
        this.minPercent = minPercent;
    }

    public String getLabel() {
        return label;
    }

    public String getCssSuffix() {
        return cssSuffix;
    }

    public int getMinPercent() {
        return minPercent;
    }

    /** 적합도(%) → 등급. 선언 순서가 높은 등급부터라 첫 매칭이 답이다. */
    public static SuitabilityGrade of(int percent) {
        for (SuitabilityGrade g : values()) {
            if (percent >= g.minPercent) {
                return g;
            }
        }
        return NOT_SUITABLE;
    }
}
