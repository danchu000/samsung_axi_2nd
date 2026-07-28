package com.ssa.lms.exam.dto;

/**
 * 제출 직후 응시자에게 보여줄 결과 요약.
 *
 * 자동 채점 결과만 담는다. 수동 채점(서술형)과 Grade 반영은 다음 슬라이스(시험 채점) 담당이라
 * 여기서는 {@code manualPending} 으로 "채점 대기"만 표시한다.
 */
public record AttemptResultView(
        String attemptId,
        String examName,
        /** SUBMITTED | AUTO_SUBMITTED */
        String status,
        /** 만료로 서버가 자동 제출했는지 — 응시자에게 반드시 알려야 한다. */
        boolean autoSubmitted,
        String submittedAt,
        /** 성적 비공개면 null — 화면에 점수를 아예 내려보내지 않는다. */
        Integer autoScore,
        Integer totalScore,
        /** 비공개면 null. 원시 타입이면 0 으로 내려가 "0점"처럼 보이므로 래퍼를 쓴다. */
        Integer examTotalScore,
        Integer passScore,
        Boolean passed,
        /** 수동 채점 대기 문항이 있으면 true — 이 경우 합격 여부는 아직 확정이 아니다. */
        boolean manualPending,
        int answeredCount,
        int questionCount,
        /** 이 응시자에게 결과를 보여줘도 되는지. Exam 의 성적 공개 설정으로 판정한다. */
        boolean resultVisible,
        /** 비공개일 때 화면에 띄울 안내 문구. 공개면 null. */
        String hiddenMessage,
        /** 연습(사전 모의) 시험이면 true — 성적에 반영되지 않는다. */
        boolean practiceMode,
        /**
         * 배정된 문제 세트 번호. {@code null} 은 세트 기능 도입 이전 회차라는 뜻이다
         * ({@link com.ssa.lms.exam.entity.ExamAttempt#getAssignedSetNo()} 계약). 원시 {@code int}
         * 로 받으면 그 null 을 언박싱하다 결과 집계 전체가 NPE 로 죽으므로 래퍼로 받는다.
         */
        Integer assignedSetNo
) {
}
