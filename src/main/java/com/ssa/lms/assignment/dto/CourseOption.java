package com.ssa.lms.assignment.dto;

/** 과정 셀렉트 옵션. id 는 화면 JS 가 문자열 비교하므로 라벨과 함께 문자열로도 노출한다. */
public record CourseOption(Long id, String courseCode, String courseName, String cohort) {

    /** "클라우드 기반 풀스택 개발자 양성과정 (1기)" */
    public String label() {
        return cohort == null || cohort.isBlank()
                ? courseName
                : courseName + " (" + cohort + ")";
    }
}
