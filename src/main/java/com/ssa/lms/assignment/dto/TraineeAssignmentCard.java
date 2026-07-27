package com.ssa.lms.assignment.dto;

/**
 * 훈련생 과제 카드 (trainee/assignments.html 의 TASKS 배열 한 건).
 *
 * 기존 화면 JS 의 shape 을 그대로 유지한다:
 *   id / name / course / session / status(ONGOING·SUBMITTED·LATE) /
 *   deadline / submitType(FILE·LINK·TEXT·FILE_TEXT) / allowResubmit /
 *   description / graded / feedback / answer
 *
 * id 는 course_assignment.id 다 — 훈련생이 보고 제출하는 단위는 언제나 CourseAssignment 다.
 * 내부 메모(visibleToTrainee=false)는 절대 담기지 않는다.
 */
public record TraineeAssignmentCard(
        String id,
        String name,
        String course,
        String session,
        /** ONGOING / SUBMITTED / LATE */
        String status,
        String deadline,
        /** FILE / LINK / TEXT / FILE_TEXT */
        String submitType,
        boolean allowResubmit,
        String description,
        boolean graded,
        String feedback,
        /** 결과 모달의 "내 답안" — 본인이 낸 텍스트/링크/첨부 요약. */
        String answer,
        /** "85 / 100" 형태. 미채점이면 null. */
        String scoreText,
        /** 지금 제출 가능한지 (기간 + 재제출 한도). */
        boolean canSubmit,
        /** 지금까지 제출 횟수. 0 이면 미제출. */
        int attemptCount,
        /** 마감 후 제출로 기록됐는지 — 감점 근거를 훈련생에게도 보여준다. */
        boolean late
) {
}
