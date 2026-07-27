package com.ssa.lms.assignment.dto;

import com.ssa.lms.assignment.entity.CourseAssignment;

/**
 * 과제 목록 검색 조건.
 * 화면 셀렉트는 "전체"를 빈 문자열로 넘긴다 → null 로 정규화(null = 조건 미적용).
 *
 * @param status 화면 값(waiting/pending/completed)은 <b>채점 진행률에서 파생</b>되는 값이라
 *               DB 조건으로 못 건다. 서버는 행을 만든 뒤 메모리에서 걸러낸다.
 */
public record AssignmentSearchCond(
        Long courseId,
        String status,
        Long graderId,
        String keyword
) {

    public static AssignmentSearchCond empty() {
        return new AssignmentSearchCond(null, null, null, null);
    }

    private static boolean isAll(String v) {
        return v == null || v.isBlank() || "전체".equals(v);
    }

    public String keywordOrNull() {
        return isAll(keyword) ? null : keyword.trim();
    }

    public String derivedStatusOrNull() {
        return isAll(status) ? null : status.trim();
    }

    /** 엔티티 상태로 직접 거는 조건은 쓰지 않는다 (화면 상태와 1:1 이 아니다). */
    public CourseAssignment.CourseAssignmentStatus entityStatusOrNull() {
        return null;
    }

    public AssignmentSearchCond withGrader(Long id) {
        return new AssignmentSearchCond(courseId, status, id, keyword);
    }
}
