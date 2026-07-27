package com.ssa.lms.assignment.dto;

import com.ssa.lms.assignment.entity.Assignment;

/**
 * 배정 화면 Step 2 "콘텐츠 은행에서 과제 선택" 테이블 한 행.
 * 기존 화면 JS 의 더미 배열 shape( id / title / difficulty / category / status )을 그대로 따른다.
 * id 는 화면이 data-id 문자열과 === 비교하므로 문자열이다.
 */
public record AssignmentOptionRow(
        String id,
        String title,
        String difficulty,
        String category,
        /** 화면 값 Active / Archived */
        String status,
        /** 배정 시 기본값으로 채울 제출 유형 (엔티티 코드) */
        String defaultSubmissionType,
        Integer maxScore,
        String description
) {

    public static AssignmentOptionRow of(Assignment a) {
        return new AssignmentOptionRow(
                String.valueOf(a.getId()),
                a.getTitle(),
                a.getDifficulty() == null ? "-" : a.getDifficulty().name().toLowerCase(),
                a.getCategory(),
                a.getStatus() == Assignment.AssignmentStatus.ACTIVE ? "Active" : "Archived",
                a.getDefaultSubmissionType().name(),
                a.getMaxScore(),
                a.getDescription()
        );
    }
}
