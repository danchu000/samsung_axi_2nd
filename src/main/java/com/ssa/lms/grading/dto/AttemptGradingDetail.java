package com.ssa.lms.grading.dto;

import java.util.List;

/**
 * 채점 팝업 전체 데이터 — {@code grading-modal-result.html} 의 {@code gradingData}.
 *
 * <p>확정된 성적이면 {@code confirmed=true} 이고, 이때 화면은 점수 저장 버튼 대신
 * "성적 정정(사유 필수)" 영역을 보여준다. 사유 없는 변경은 서버에서 거부한다.</p>
 */
public record AttemptGradingDetail(
        String attemptId,
        String examId,
        String courseName,
        String examName,
        Student student,
        String examTime,
        String status,
        String statusClass,
        int autoScore,
        int manualScore,
        int totalScore,
        int maxScore,
        int passScore,
        Boolean passed,
        boolean confirmed,
        boolean canGrade,
        boolean manualPending,
        int currentQ,
        List<GradingQuestionRow> questions,
        String saveUrl,
        String confirmUrl,
        String listUrl
) {

    /** 화면 표기용 최소 정보만. 이메일·전화 등 개인정보는 채점 화면에 내리지 않는다. */
    public record Student(String name, String id) {
    }
}
