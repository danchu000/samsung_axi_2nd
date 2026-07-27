package com.ssa.lms.assignment.dto;

import com.ssa.lms.assignment.entity.CourseAssignment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 관리자/강사 과제 목록 한 행.
 *
 * static/js/assignments.js 의 assignmentData 더미 shape 을 그대로 따른다
 * (number / courseName / courseId / title / instructor / evalType /
 *  startDate·startTime / endDate·endTime / submitted / notSubmitted / status).
 * 기존 렌더 함수를 고치지 않고 데이터만 서버 것으로 바꾸기 위함이다.
 *
 * 화면 status 는 엔티티 상태가 아니라 <b>채점 진행률에서 파생</b>한다:
 *  - waiting(평가예정)  : 아직 공개 전이거나 제출자가 0명 → 채점 버튼 비활성
 *  - pending(채점예정)  : 제출은 있는데 채점이 덜 끝남
 *  - completed(채점완료): 제출자 전원 채점 완료
 */
public record CourseAssignmentRow(
        /** 화면 JS 가 data-id 문자열과 비교하므로 문자열. */
        String id,
        int number,
        String courseName,
        /** 화면의 "COURSE-2024-001" 자리 — 과정 코드다 (PK 아님). */
        String courseId,
        String title,
        String instructor,
        /** 항상 "assignment". 시험 목록과 같은 렌더러를 공유하기 때문에 필요하다. */
        String evalType,
        String startDate,
        String startTime,
        String endDate,
        String endTime,
        long submitted,
        long notSubmitted,
        String status,
        /** 채점 화면 URL. assignments.js 가 routes 보다 이 값을 우선한다. */
        String address
) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public static CourseAssignmentRow of(CourseAssignment ca, int number,
                                         long submitted, long graded, long enrolled,
                                         String gradingUrlPrefix) {
        long notSubmitted = Math.max(0, enrolled - submitted);
        return new CourseAssignmentRow(
                String.valueOf(ca.getId()),
                number,
                ca.getCourse().getCourseName(),
                ca.getCourse().getCourseCode(),
                ca.getAssignment().getTitle(),
                ca.getGrader() == null ? "-" : ca.getGrader().getName(),
                "assignment",
                ca.getStartAt().format(DATE),
                ca.getStartAt().format(TIME) + "부터",
                ca.getEndAt().format(DATE),
                ca.getEndAt().format(TIME) + "까지",
                submitted,
                notSubmitted,
                deriveStatus(ca, submitted, graded),
                gradingUrlPrefix + ca.getId() + "/grading"
        );
    }

    private static String deriveStatus(CourseAssignment ca, long submitted, long graded) {
        boolean notOpenYet = ca.getStatus() == CourseAssignment.CourseAssignmentStatus.DRAFT
                || ca.getStatus() == CourseAssignment.CourseAssignmentStatus.SCHEDULED;
        if (notOpenYet || submitted == 0) {
            return "waiting";
        }
        return graded >= submitted ? "completed" : "pending";
    }

    static String fmtDate(LocalDateTime at) {
        return at == null ? "-" : at.format(DATE);
    }
}
