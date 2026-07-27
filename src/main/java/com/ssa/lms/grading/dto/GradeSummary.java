package com.ssa.lms.grading.dto;

import com.ssa.lms.grading.entity.Grade;

import java.time.LocalDateTime;

/**
 * 개발자 A의 이수(completion) 판정이 읽는 성적 요약.
 *
 * `grade` 테이블은 개발자 B 소유이므로 A는 엔티티/리포지토리를 직접 쓰지 않고
 * {@code GradeQueryService} 를 통해 이 레코드만 받는다. (a-requests.md P1-8)
 *
 * @param evalType  EXAM 또는 ASSIGNMENT
 * @param evalRefId evalType 에 따라 exam.id 또는 course_assignment.id
 * @param passed    합격 여부. 미채점이면 null
 */
public record GradeSummary(
        Long gradeId,
        Long userId,
        Long courseId,
        Grade.EvalType evalType,
        Long evalRefId,
        Integer totalScore,
        Boolean passed,
        Grade.GradeStatus status,
        LocalDateTime confirmedAt
) {

    public static GradeSummary from(Grade g) {
        return new GradeSummary(
                g.getId(),
                g.getUser().getId(),
                g.getCourse().getId(),
                g.getEvalType(),
                g.getEvalRefId(),
                g.getTotalScore(),
                g.getPassed(),
                g.getStatus(),
                g.getConfirmedAt()
        );
    }

    public boolean isConfirmed() {
        return status == Grade.GradeStatus.CONFIRMED;
    }
}
