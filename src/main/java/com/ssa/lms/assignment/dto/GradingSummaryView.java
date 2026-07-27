package com.ssa.lms.assignment.dto;

import com.ssa.lms.assignment.entity.CourseAssignment;

import java.time.format.DateTimeFormatter;

/**
 * 채점 화면 상단 요약 박스 (assignment-grading.html 의 grading-summary-box).
 * 화면이 보여주는 값: 과정명 / 과제명 / 제출 기간 / 평가 방식 / 합격 기준.
 */
public record GradingSummaryView(
        Long courseAssignmentId,
        String courseName,
        String assignmentTitle,
        String period,
        String gradingMethod,
        String passCriteria,
        String submissionTypeLabel,
        int score,
        int passScore,
        long enrolledCount,
        long submittedCount,
        long gradedCount
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static GradingSummaryView of(CourseAssignment ca, long enrolled, long submitted, long graded) {
        return new GradingSummaryView(
                ca.getId(),
                ca.getCourse().getCourseName()
                        + (ca.getCourse().getCohort() == null ? "" : " (" + ca.getCourse().getCohort() + ")"),
                ca.getAssignment().getTitle(),
                ca.getStartAt().format(FMT) + " ~ " + ca.getEndAt().format(FMT),
                ca.isAutoGrading() ? "자동 + 수동" : "수동",
                ca.effectivePassScore() + "점 이상",
                SubmissionTypes.label(ca.getSubmissionType()),
                ca.getScore(),
                ca.effectivePassScore(),
                enrolled,
                submitted,
                graded
        );
    }
}
