package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Exam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 시험 목록 한 행.
 *
 * 화면(admin-evaluation-test.html) 테이블 헤더와 대응:
 * 번호 / 시험명 / 과정 / 강사명 / 소요시간 / 문항 수 / 응시기간 / 상태 / 수정하기
 *
 * 엔티티를 그대로 넘기지 않는 이유: LAZY 프록시 + 화면 JS 가 data-id 문자열과 === 비교하기 때문.
 * 날짜도 직렬화 이슈를 피하려고 문자열로 내린다 (QuestionListRow 와 같은 방침).
 */
public record ExamListRow(
        String id,
        String examName,
        String examType,
        /** 강사 담당 과정 필터링에 쓴다. 화면에는 노출하지 않는다. */
        Long courseId,
        String courseName,
        String courseCode,
        String instructorName,
        int timeLimitMin,
        long questionCount,
        int totalScore,
        int passScore,
        String windowStart,
        String windowEnd,
        /** 화면 값: 임시저장 / 예정 / 진행중 / 종료 / 보관 */
        String status,
        /** 화면 필터(활성화/비활성화)와 배지 클래스용. */
        String statusGroup,
        /** 내역서 요건 노출 — 본인인증 강제 여부. */
        boolean requireIdentityVerification,
        boolean proctorEnabled,
        /** 문제 세트 개수. 1 이면 전원 동일 세트. */
        int questionSetCount,
        /** 성적 공개 방식 문구 (즉시 공개 / 채점 완료 후 공개 / ... / 비공개). */
        String resultReleaseText,
        /** 사전 모의 테스트 여부 — 목록에서 정식 시험과 구분 표시한다. */
        boolean practiceMode
) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static ExamListRow of(Exam exam, long questionCount) {
        return new ExamListRow(
                String.valueOf(exam.getId()),
                exam.getExamName(),
                ExamForm.typeLabel(exam.getExamType()),
                exam.getCourse() == null ? null : exam.getCourse().getId(),
                exam.getCourse() == null ? "-" : exam.getCourse().getCourseName(),
                exam.getCourse() == null ? "-" : exam.getCourse().getCourseCode(),
                exam.getInstructor() == null ? "-" : exam.getInstructor().getName(),
                exam.getTimeLimitMin(),
                questionCount,
                exam.getTotalScore(),
                exam.getPassScore(),
                format(exam.getWindowStart()),
                format(exam.getWindowEnd()),
                ExamForm.statusLabel(exam.getStatus()),
                groupOf(exam.getStatus()),
                exam.isRequireIdentityVerification(),
                exam.isProctorEnabled(),
                exam.getQuestionSetCount() == null ? 1 : exam.getQuestionSetCount(),
                exam.resultReleaseText(),
                exam.isPracticeMode()
        );
    }

    /** 화면 필터는 활성화/비활성화 두 갈래다. 종료·보관만 비활성으로 본다. */
    private static String groupOf(Exam.ExamStatus status) {
        return switch (status) {
            case CLOSED, ARCHIVED -> "inactive";
            default -> "active";
        };
    }

    private static String format(LocalDateTime at) {
        return at == null ? "-" : at.format(FORMAT);
    }
}
