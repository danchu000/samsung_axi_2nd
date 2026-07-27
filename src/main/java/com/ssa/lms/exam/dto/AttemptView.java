package com.ssa.lms.exam.dto;

import java.util.List;

/**
 * 응시 화면(do-test.html) 전체 모델.
 *
 * <b>remainSeconds 는 표시용일 뿐이다.</b> 제출 마감 판정은 서버가 ExamAttempt.expiresAt 으로만 한다.
 * 클라이언트가 시계를 조작해도 서버 판정은 바뀌지 않는다.
 */
public record AttemptView(
        String attemptId,
        String examId,
        String examName,
        String courseName,
        String note,
        int attemptNo,
        int maxAttempts,
        /** IN_PROGRESS / SUBMITTED / AUTO_SUBMITTED / ... */
        String status,
        /** 서버가 계산한 마감 시각 (yyyy-MM-dd HH:mm:ss). */
        String expiresAt,
        /** 렌더 시점의 서버 시각. 화면 타이머는 이 값 기준으로 카운트다운한다. */
        String serverNow,
        /** 남은 초. 0 이면 이미 만료. */
        long remainSeconds,
        boolean blockTabSwitch,
        boolean blockCopyPaste,
        boolean proctorEnabled,
        boolean requireWebcam,
        /** 본인인증 완료 시각 (화면 상단 배지). */
        String identityVerifiedAt,
        String identityVerifyMethod,
        List<AttemptQuestionRow> questions
) {
}
