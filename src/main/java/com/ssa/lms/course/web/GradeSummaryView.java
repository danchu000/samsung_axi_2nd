package com.ssa.lms.course.web;

import com.ssa.lms.course.service.screening.SuitabilityGrade;

/** 수강신청 승인 화면 상단 요약 카드 — 등급별 신청자 수. */
public record GradeSummaryView(String label, String cssSuffix, String range, long count) {

    public static GradeSummaryView of(SuitabilityGrade grade, long count) {
        return new GradeSummaryView(grade.getLabel(), grade.getCssSuffix(), rangeOf(grade), count);
    }

    private static String rangeOf(SuitabilityGrade grade) {
        return switch (grade) {
            case VERY_SUITABLE -> "90% 이상";
            case SUITABLE -> "75 ~ 89%";
            case UNSUITABLE -> "60 ~ 74%";
            case NOT_SUITABLE -> "60% 미만";
        };
    }
}
