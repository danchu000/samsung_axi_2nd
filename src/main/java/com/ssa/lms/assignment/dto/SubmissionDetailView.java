package com.ssa.lms.assignment.dto;

import java.util.List;

/**
 * 채점 팝업(grading-modal.html)이 보여주는 제출물 상세.
 *
 * 왼쪽: 과제 설명 + 채점 기준 + 학생 답안(텍스트/링크/첨부)
 * 오른쪽: 배점·통과점수·입력점수 + 피드백(공개) + 내부 메모
 */
public record SubmissionDetailView(
        Long submissionId,
        Long courseAssignmentId,
        String studentName,
        String assignmentTitle,
        String courseName,
        String description,
        List<CriteriaRow> criteria,
        String submitTypeLabel,
        String contentText,
        String linkUrl,
        List<FileRow> files,
        int attemptNo,
        boolean late,
        String submittedAt,
        int maxScore,
        int passScore,
        /** 기존 점수. 미채점이면 null → 정정이 아니라 최초 채점이다. */
        Integer currentScore,
        String currentFeedback,
        String currentMemo,
        /** 이미 점수가 있으면 화면이 사유 입력을 필수로 만든다. */
        boolean regrade
) {

    public record CriteriaRow(int seq, String content, int score) {
    }

    public record FileRow(Long id, String originalName, long sizeBytes, String downloadUrl) {
    }
}
