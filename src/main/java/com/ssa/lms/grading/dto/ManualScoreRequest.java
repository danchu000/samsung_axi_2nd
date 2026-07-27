package com.ssa.lms.grading.dto;

import java.util.List;

/**
 * 수동 채점 저장 요청 (채점 팝업 → 서버).
 *
 * @param scores  수동 채점 대상 문항의 점수. 자동 채점 문항이 섞여 오면 서버가 거부한다.
 * @param reason  성적 정정 사유. <b>확정(CONFIRMED)된 성적을 바꿀 때는 필수</b>이며,
 *                비어 있으면 저장을 거부하고 {@code GradeHistory} 도 남기지 않는다 (내역서 증빙 요건).
 * @param confirm true 면 저장과 동시에 성적을 확정한다.
 */
public record ManualScoreRequest(
        List<ScoreEntry> scores,
        String reason,
        boolean confirm
) {

    public record ScoreEntry(Long questionId, Integer score, String comment) {
    }

    public List<ScoreEntry> safeScores() {
        return scores == null ? List.of() : scores;
    }
}
